package com.dorkag.azure_devops.tasks

import com.dorkag.azure_devops.dto.Pool
import com.dorkag.azure_devops.dto.flow.Pipeline
import com.dorkag.azure_devops.dto.flow.Stage
import com.dorkag.azure_devops.dto.flow.Step
import com.dorkag.azure_devops.extensions.AzurePipelineSubProjectExtension
import com.dorkag.azure_devops.extensions.config.JobConfig
import com.dorkag.azure_devops.extensions.config.StepConfig
import com.dorkag.azure_devops.utils.AzureCommentMetadataGenerator
import com.dorkag.azure_devops.utils.YamlUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
abstract class GenerateSubprojectTemplateTask : DefaultTask() {
  @get:Internal
  abstract val subProjectExtensionProperty: Property<AzurePipelineSubProjectExtension>

  @get:OutputFile
  abstract val subprojectYaml: RegularFileProperty

  @get:Input
  abstract val pluginVersion: Property<String>

  @get:Input
  abstract val gradleVersion: Property<String>

  @get:Input
  abstract val projectName: Property<String>

  @get:Input
  abstract val rootProjectHasPlugin: Property<Boolean>

  @get:Input
  abstract val subprojectName: Property<String>

  init {
    subprojectYaml.convention(project.layout.projectDirectory.file("azure-pipelines.yml"))
    pluginVersion.convention("development")
    gradleVersion.convention(project.gradle.gradleVersion)
    projectName.convention(project.provider { project.name })
    subprojectName.convention(project.provider { project.name })
  }

  @TaskAction
  fun generateSubTemplate() {
    val ext = subProjectExtensionProperty.get()
    val rootApplies = project.rootProject.plugins.hasPlugin("com.dorkag.azuredevops")

    val pipeline = if (!rootApplies) { // If root doesn't apply, produce a standalone pipeline
      buildStandalone(ext)
    } else { // If root also applies, produce a snippet or override
      buildSnippet(ext)
    }

    val meta = AzureCommentMetadataGenerator.generateMetadataComment(pluginVersion.get(), gradleVersion.get())
    val finalYaml = YamlUtil.toYaml(pipeline)
    subprojectYaml.get().asFile.writeText(meta + "\n" + finalYaml)

    logger.lifecycle("Subproject pipeline for ${projectName.get()} at ${subprojectYaml.get().asFile}")
  }

  private fun buildStandalone(ext: AzurePipelineSubProjectExtension): Pipeline { // OK as long as ext.getStages() doesn't call `project` at execution time.
    val stageList = ext.getStages().mapNotNull { (stageName, stageCfg) ->
      if (!stageCfg.enabled.get()) null
      else Stage(
        stage = stageName, displayName = stageCfg.displayName.orNull, jobs = mapJobs(stageCfg.jobs.get())
      )
    }

    return Pipeline(
      name = ext.name.orNull ?: "${subprojectName.get()} Pipeline",
      trigger = ext.trigger.get().ifEmpty { null },
      pool = Pool(vmImage = ext.vmImage.getOrElse("ubuntu-latest")),
      stages = stageList
    )
  }

  private fun buildSnippet(ext: AzurePipelineSubProjectExtension): Pipeline {
    return buildStandalone(ext)
  }

  private fun mapJobs(jobs: Map<String, JobConfig>): List<com.dorkag.azure_devops.dto.flow.Job> {
    return jobs.map { (jobName, jobCfg) ->
      com.dorkag.azure_devops.dto.flow.Job(
        job = jobName, displayName = jobCfg.displayName.orNull, steps = mapSteps(jobCfg.steps.get())
      )
    }
  }

  @Suppress("DuplicatedCode")
  private fun mapSteps(steps: Map<String, StepConfig>): List<Step> {
    return steps.map { (_, stepCfg) ->
      if (stepCfg.taskName.isPresent) {
        Step(
          task = stepCfg.taskName.get(), displayName = stepCfg.displayName.orNull, inputs = stepCfg.inputs.get().ifEmpty { null })
      } else if (stepCfg.script.isPresent) {
        Step(
          script = stepCfg.script.orNull, displayName = stepCfg.displayName.orNull
        )
      } else {
        Step(script = "echo 'no script or task'")
      }
    }
  }
}