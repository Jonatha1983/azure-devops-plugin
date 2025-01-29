package com.dorkag.azure_devops

import com.dorkag.azure_devops.exceptions.PipelineConfigurationException
import com.dorkag.azure_devops.extensions.AzurePipelineExtension
import com.dorkag.azure_devops.extensions.AzurePipelineSubProjectExtension
import com.dorkag.azure_devops.tasks.GenerateDslFromYamlTask
import com.dorkag.azure_devops.tasks.GenerateRootPipelineTask
import com.dorkag.azure_devops.tasks.GenerateSubprojectTemplateTask
import com.dorkag.azure_devops.tasks.ValidatePipelineTask
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Azure DevOps Pipeline plugin.
 *
 * Provides:
 * - Root tasks:
 *   - generateRootPipeline (creates azure-pipelines.yml)
 *   - validatePipeline (checks pipeline correctness)
 *   - generatePipeline (aggregates root and subprojects)
 * - Subproject task:
 *   - generateSubprojectTemplate (creates subproject azure-pipelines.yml)
 * - convertYamlToDsl (global)
 */
@Suppress("unused")
class AzureDevopsPluginPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    if (project == project.rootProject) {
      applyToRootProject(project)
      validateRootAfterEvaluate(project)
    } else {
      applyToSubproject(project)
      ensureRootCoordination(project)
    }

    // Common task for all projects (root or subproject)
    project.tasks.register("convertYamlToDsl", GenerateDslFromYamlTask::class.java) {
      it.group = "Azure DevOps"
      it.description = "Generate Gradle DSL from an existing Azure DevOps YAML."
    }
  }

  private fun ensureRootCoordination(subproject: Project) { // Only do this once for the first subproject that applies the plugin
    if (!subproject.rootProject.tasks.names.contains("generatePipeline")) {
      subproject.rootProject.tasks.register("generatePipeline") { aggregator ->
        aggregator.group = "Azure DevOps"
        aggregator.description = "Generate pipeline for subprojects applying the plugin."

        // Find and depend on all subprojects that apply our plugin
        subproject.rootProject.subprojects.forEach { sub ->
          if (sub.plugins.hasPlugin("com.dorkag.azuredevops")) {
            aggregator.dependsOn("${sub.path}:generateSubprojectTemplate")
          }
        }
      }
    }
  }

  private fun applyToRootProject(project: Project) {
    val extension = project.extensions.create(
      "azurePipeline", AzurePipelineExtension::class.java, project.objects
    )

    val rootPipelineTask = project.tasks.register(
      "generateRootPipeline", GenerateRootPipelineTask::class.java
    ) {
      it.group = "Azure DevOps"
      it.description = "Generate the root Azure DevOps pipeline YAML file."
      it.extensionProperty.set(extension)
    }

    // Only create the aggregator task if it doesn't exist yet
    if (!project.tasks.names.contains("generatePipeline")) {
      project.tasks.register("generatePipeline") { aggregator ->
        aggregator.group = "Azure DevOps"
        aggregator.description = "Generate pipeline for root and any subprojects applying the plugin."
        aggregator.dependsOn(rootPipelineTask)

        project.subprojects.forEach { sub ->
          if (sub.plugins.hasPlugin("com.dorkag.azuredevops")) {
            aggregator.dependsOn("${sub.path}:generateSubprojectTemplate")
          }
        }
      }
    }

    project.tasks.register("validatePipeline", ValidatePipelineTask::class.java) {
      it.group = "Azure DevOps"
      it.description = "Validate the root Azure DevOps pipeline configuration."
      it.extension = extension
    }
  }

  private fun applyToSubproject(project: Project) {
    val subProjectExtension = project.extensions.create(
      "azurePipeline", AzurePipelineSubProjectExtension::class.java, project.objects
    )

    project.tasks.register(
      "generateSubprojectTemplate", GenerateSubprojectTemplateTask::class.java
    ) { task ->
      task.group = "Azure DevOps"
      task.description = "Generate azure-pipelines.yml for subproject '${project.name}'"
      task.subProjectExtensionProperty.set(subProjectExtension)
    }

    // Validate subproject configuration
    project.afterEvaluate {
      val requestedTasks = project.gradle.startParameter.taskNames
      val shouldValidateSub = requestedTasks.any { taskName ->
        taskName.contains("generateSubprojectTemplate", ignoreCase = true) || taskName.contains("generatePipeline", ignoreCase = true)
      }

      if (shouldValidateSub) {
        val stages = subProjectExtension.getStages()
        if (stages.isEmpty()) {
          throw PipelineConfigurationException(
            "Subproject '${project.name}' must define at least one stage if the Azure DevOps plugin is applied."
          )
        }

        // Validate each stage has jobs and steps
        stages.forEach { (stageName, stageConfig) ->
          if (!stageConfig.enabled.get()) return@forEach

          val jobs = stageConfig.jobs.get()
          if (jobs.isEmpty()) {
            throw PipelineConfigurationException(
              "Stage '$stageName' in subproject '${project.name}' must contain at least one job."
            )
          }

          jobs.forEach { (jobName, jobConfig) ->
            val steps = jobConfig.steps.get()
            if (steps.isEmpty()) {
              throw PipelineConfigurationException(
                "Job '$jobName' in stage '$stageName' must contain at least one step."
              )
            }
          }
        }
      }
    }
  }

  private fun validateRootAfterEvaluate(project: Project) {
    project.afterEvaluate {
      val requestedTasks = project.gradle.startParameter.taskNames
      val shouldValidate = requestedTasks.any { taskName ->
        taskName.contains("generateRootPipeline", ignoreCase = true) || taskName.contains("validatePipeline", ignoreCase = true) || taskName.contains(
          "generatePipeline", ignoreCase = true
        )
      }

      if (!shouldValidate) return@afterEvaluate

      val extension = project.extensions.findByType(AzurePipelineExtension::class.java) ?: throw PipelineConfigurationException(
        "AzurePipelineExtension not configured in the root project."
      )

      val stages = extension.getStages()
      if (stages.isEmpty()) {
        throw PipelineConfigurationException(
          "At least one stage must be configured in the root pipeline."
        )
      }

      // Validate each stage has jobs and steps
      stages.forEach { (stageName, stageConfig) ->
        if (!stageConfig.enabled.get()) return@forEach

        val jobs = stageConfig.jobs.get()
        if (jobs.isEmpty()) {
          throw PipelineConfigurationException(
            "Stage '$stageName' must contain at least one job."
          )
        }

        jobs.forEach { (jobName, jobConfig) ->
          val steps = jobConfig.steps.get()
          if (steps.isEmpty()) {
            throw PipelineConfigurationException(
              "Job '$jobName' in stage '$stageName' must contain at least one step."
            )
          }
        }
      }
    }
  }
}