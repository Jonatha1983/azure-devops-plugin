package com.dorkag.azure_devops.tasks

import com.dorkag.azure_devops.extensions.AzurePipelineSubProjectExtension
import com.dorkag.azure_devops.utils.AzureCommentMetadataGenerator
import com.dorkag.azure_devops.utils.YamlUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction


abstract class GenerateSubprojectTemplateTask : DefaultTask() {
  @get:Internal
  abstract val subProjectExtensionProperty: Property<AzurePipelineSubProjectExtension>

  @get:OutputFile
  abstract val subprojectYaml: RegularFileProperty

  @get:Internal
  abstract val pluginVersion: Property<String>

  @get:Internal
  abstract val gradleVersion: Property<String>

  @get:Internal
  abstract val projectName: Property<String>

  @get:Internal
  abstract val rootProjectAppliesPlugin: Property<Boolean>

  init {
    subprojectYaml.convention(project.layout.projectDirectory.file("azure-pipelines.yml"))
    projectName.set(project.name)
    pluginVersion.set(
      project.plugins.findPlugin("com.dorkag.azuredevops")?.javaClass?.getPackage()?.implementationVersion ?: "development"
    )
    gradleVersion.set(project.gradle.gradleVersion)
    rootProjectAppliesPlugin.set(project.rootProject.plugins.hasPlugin("com.dorkag.azuredevops"))
  }

  @TaskAction
  fun generateSubTemplate() {
    val extension = subProjectExtensionProperty.get()
    val stages = extension.getStages()

    if (stages.isEmpty()) {
      logger.lifecycle("No stages defined for subproject '${projectName.get()}'. Skipping template generation.")
      return
    }

    // Generate proper stage configurations
    val stageConfigs = stages.map { (stageName, stageConfig) ->
      mapOf(
        "stage" to stageName, "displayName" to (stageConfig.displayName.orNull ?: "Stage: $stageName"), "jobs" to stageConfig.jobs.get().map { (jobName, jobConfig) ->
          mapOf(
            "job" to jobName, "displayName" to (jobConfig.displayName.orNull ?: "$jobName job"), "steps" to jobConfig.steps.get().map { (_, stepConfig) ->
              mapOf(
                "script" to (stepConfig.script.orNull ?: "echo 'Executing $stageName'"), "displayName" to (stepConfig.displayName.orNull ?: "Run $stageName")
              )
            })
        })
    }

    val snippetData = if (rootProjectAppliesPlugin.get()) { // Generate template for root inclusion
      mapOf("stages" to stageConfigs)
    } else { // Generate complete standalone pipeline
      mapOf(
        "name" to (extension.name.orNull ?: "${projectName.get()} Pipeline"),
        "trigger" to extension.trigger.get(),
        "pool" to mapOf("vmImage" to extension.vmImage.get()),
        "stages" to stageConfigs
      )
    }.filterValues { it != null }

    val metadataComment = AzureCommentMetadataGenerator.generateMetadataComment(
      pluginVersion.get(), gradleVersion.get()
    )

    val outputFile = subprojectYaml.get().asFile
    outputFile.writeText(metadataComment + "\n" + YamlUtil.toYaml(snippetData))

    logger.lifecycle("Subproject pipeline template generated at: ${outputFile.absolutePath}")
    logger.lifecycle("Template content:\n${outputFile.readText()}")
  }
}