package com.dorkag.azure_devops

import com.dorkag.azure_devops.exceptions.PipelineConfigurationException
import com.dorkag.azure_devops.extensions.AzurePipelineExtension
import com.dorkag.azure_devops.extensions.AzurePipelineSubProjectExtension
import com.dorkag.azure_devops.tasks.GenerateDslFromYamlTask
import com.dorkag.azure_devops.tasks.GenerateRootPipelineTask
import com.dorkag.azure_devops.tasks.GenerateSubprojectTemplateTask
import com.dorkag.azure_devops.tasks.ValidatePipelineTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion

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
    checkGradleVersion()

    if (project == project.rootProject) {
      applyToRootProject(project)
      validateRootAfterEvaluate(project)
    } else {
      applyToSubproject(project)
      ensureRootCoordination(project)
    }

    // Common task
    project.tasks.register("convertYamlToDsl", GenerateDslFromYamlTask::class.java) {
      it.group = "Azure DevOps"
      it.description = "Generate Gradle DSL from an existing Azure DevOps YAML."
    }
  }

  private fun checkGradleVersion() {
    if (GradleVersion.current() < GradleVersion.version("8.9")) {
      throw GradleException("Azure DevOps Plugin requires Gradle 8.9 or higher. You are using Gradle ${GradleVersion.current()}.")
    }
  }

  private fun ensureRootCoordination(subproject: Project) { // If root does not have "generatePipeline" yet, create aggregator
    if (!subproject.rootProject.tasks.names.contains("generatePipeline")) {
      subproject.rootProject.tasks.register("generatePipeline") { agg ->
        agg.group = "Azure DevOps"
        agg.description = "Generate pipeline for subprojects applying the plugin." // depends on subprojects
        subproject.rootProject.subprojects.forEach { sp ->
          if (sp.plugins.hasPlugin("com.dorkag.azuredevops")) {
            agg.dependsOn("${sp.path}:generateSubprojectTemplate")
          }
        }
      }
    }
  }

  private fun applyToRootProject(rootProject: Project) {
    val extension = rootProject.extensions.create(
      "azurePipeline", AzurePipelineExtension::class.java, rootProject.objects
    )

    val rootTask = rootProject.tasks.register("generateRootPipeline", GenerateRootPipelineTask::class.java) {
      it.group = "Azure DevOps"
      it.description = "Generate the root Azure DevOps pipeline YAML file."
      it.extensionProperty.set(extension)
    }

    // aggregator "generatePipeline"
    if (!rootProject.tasks.names.contains("generatePipeline")) {
      rootProject.tasks.register("generatePipeline") { agg ->
        agg.group = "Azure DevOps"
        agg.description = "Generate pipeline for root + subprojects"
        agg.dependsOn(rootTask)
        rootProject.subprojects.forEach { sp ->
          if (sp.plugins.hasPlugin("com.dorkag.azuredevops")) {
            agg.dependsOn("${sp.path}:generateSubprojectTemplate")
          }
        }
      }
    }

    // validate
    rootProject.tasks.register("validatePipeline", ValidatePipelineTask::class.java) {
      it.group = "Azure DevOps"
      it.description = "Validate the root Azure DevOps pipeline."
      it.extension = extension
    }
  }

  private fun applyToSubproject(subProject: Project) {
    val subProjectExtension = subProject.extensions.create(
      "azurePipeline", AzurePipelineSubProjectExtension::class.java, subProject, subProject.objects
    )

    subProject.tasks.register("generateSubprojectTemplate", GenerateSubprojectTemplateTask::class.java) { t ->
      t.group = "Azure DevOps"
      t.description = "Generate azure-pipelines.yml for subproject ${subProject.name}"
      t.subProjectExtensionProperty.set(subProjectExtension)

      // set the boolean property at configuration time
      t.rootPluginApplied.set(
        subProject.provider {
          subProject.rootProject.plugins.hasPlugin("com.dorkag.azuredevops")
        }
      )

      // set subprojectName, etc. here as well
      t.subprojectName.set(subProject.provider { subProject.name })
    }


    // optional validation for subprojects
    subProject.afterEvaluate {
      val requestedTasks = subProject.gradle.startParameter.taskNames
      val shouldValidateSub = requestedTasks.any { it.contains("generateSubprojectTemplate", ignoreCase = true) || it.contains("generatePipeline", ignoreCase = true) }

      if (shouldValidateSub) {
        val stages = subProjectExtension.getStages()
        if (stages.isEmpty()) {
          throw PipelineConfigurationException(
            "Subproject '${subProject.name}' must define at least one stage if the Azure DevOps plugin is applied."
          )
        }

        // Validate each stage has jobs and steps
        stages.forEach { (stageName, stageConfig) ->
          if (!stageConfig.enabled.get()) return@forEach

          // SKIP "jobs required" check if declaredFromRoot == true
          if (stageConfig.declaredFromRoot.get()) { // This stage is referencing a root-defined stage => no local jobs required
            return@forEach
          }

          // Otherwise, do the usual checks
          val jobs = stageConfig.jobs.get()
          if (jobs.isEmpty()) {
            throw PipelineConfigurationException(
              "Stage '$stageName' in subproject '${subProject.name}' must contain at least one job."
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

        val jobNames = jobs.keys
        val duplicates = jobNames.groupBy { it }.filter { it.value.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
          throw PipelineConfigurationException(
            "Duplicate job name(s) in stage '$stageName': ${duplicates.joinToString(", ")}. Job names must be unique."
          )
        }
      }

      val duplicateStages = stages.values.groupBy { it }.filter { it.value.size > 1 }.keys
      if (duplicateStages.isNotEmpty()) {
        throw PipelineConfigurationException(
          "Duplicate stage name(s) found: ${duplicateStages.joinToString(", ")}. Stage names must be unique."
        )
      }
    }
  }
}