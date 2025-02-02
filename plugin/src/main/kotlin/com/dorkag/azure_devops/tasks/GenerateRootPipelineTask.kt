package com.dorkag.azure_devops.tasks

import com.dorkag.azure_devops.dto.Pool
import com.dorkag.azure_devops.dto.flow.Job
import com.dorkag.azure_devops.dto.flow.Pipeline
import com.dorkag.azure_devops.dto.flow.Stage
import com.dorkag.azure_devops.dto.flow.Step
import com.dorkag.azure_devops.extensions.AzurePipelineExtension
import com.dorkag.azure_devops.extensions.config.JobConfig
import com.dorkag.azure_devops.extensions.config.StepConfig
import com.dorkag.azure_devops.utils.AzureCommentMetadataGenerator
import com.dorkag.azure_devops.utils.YamlUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*


@CacheableTask
abstract class GenerateRootPipelineTask : DefaultTask() {
  @get:Internal
  abstract val extensionProperty: Property<AzurePipelineExtension>

  @get:Internal
  abstract val subProjectsProperty: ListProperty<String> // Subproject pipelines

  @get:OutputFile
  val pipelineYaml: RegularFileProperty = project.objects.fileProperty().convention(project.layout.projectDirectory.file("azure-pipelines.yml"))

  @get:Input
  val pluginVersion: Property<String> = project.objects.property(String::class.java).convention("unknown")

  @get:Input
  val gradleVersion: Property<String> = project.objects.property(String::class.java).convention(project.gradle.gradleVersion)


  init { // Dynamically populate subProjectsProperty (subprojects that apply the plugin)
    subProjectsProperty.convention(
      project.provider {
        project.subprojects.filter { it.plugins.hasPlugin("com.dorkag.azuredevops") }.map { sp -> "${sp.name}|${sp.projectDir.relativeTo(project.projectDir).path}" }
      })

  }

  @Input
  fun getSubProjectRefs(): List<String> = subProjectsProperty.get()

  @TaskAction
  fun generate() {
    val ext = extensionProperty.get()
    val subStrings = subProjectsProperty.get()
    val hasSubs = subStrings.isNotEmpty()

    val pipelineDto: Pipeline = if (!hasSubs) { // single or multi but root only
      buildStandalonePipeline(ext)
    } else { // aggregator referencing subprojects
      buildAggregatorPipeline(ext, subStrings)
    }

    val metadata = AzureCommentMetadataGenerator.generateMetadataComment(pluginVersion.get(), gradleVersion.get())
    val finalYaml = YamlUtil.toYaml(pipelineDto)
    pipelineYaml.get().asFile.writeText(metadata + "\n" + finalYaml)

    logger.lifecycle(
      if (hasSubs) "Root pipeline with subproject references => ${pipelineYaml.get().asFile}"
      else "Single project pipeline => ${pipelineYaml.get().asFile}"
    )
  }

  private fun buildStandalonePipeline(ext: AzurePipelineExtension): Pipeline {
    val stageList = ext.stages.get().mapNotNull { (stageName, stageCfg) ->
      if (!stageCfg.enabled.get()) null
      else Stage(
        stage = stageName,
        displayName = stageCfg.displayName.orNull,
        dependsOn = stageCfg.dependsOn.get().ifEmpty { null },
        condition = stageCfg.condition.orNull,
        variables = stageCfg.variables.get().ifEmpty { null },
        jobs = mapJobs(stageCfg.jobs.get())
      )
    }

    return Pipeline(
      name = ext.name.getOrElse("UnnamedPipeline"),
      trigger = ext.trigger.get().ifEmpty { null },
      pr = ext.pr.orNull?.branches?.get()?.ifEmpty { null },
      parameters = ext.parameters.map { it.toDto() },
      pool = Pool(vmImage = ext.vmImage.getOrElse("ubuntu-latest")),
      variables = ext.variables.get().ifEmpty { null },
      resources = ext.getResources(),
      schedules = null,
      lockBehavior = ext.lockBehavior.orNull,
      appendCommitMessageToRunName = ext.appendCommitMessageToRunName.orNull,
      stages = stageList
    )
  }

  private fun buildAggregatorPipeline(ext: AzurePipelineExtension, subStrings: List<String>): Pipeline { // references subprojects as templates
    return Pipeline(
      name = ext.name.getOrElse("UnnamedPipeline"),
      trigger = ext.trigger.get().ifEmpty { null },
      pr = ext.pr.orNull?.branches?.get()?.ifEmpty { null },
      parameters = ext.parameters.map { it.toDto() },
      pool = Pool(vmImage = ext.vmImage.getOrElse("ubuntu-latest")),
      variables = ext.variables.get().ifEmpty { null },
      resources = ext.getResources(),
      schedules = null,
      lockBehavior = ext.lockBehavior.orNull,
      appendCommitMessageToRunName = ext.appendCommitMessageToRunName.orNull,
      stages = subStrings.map { s ->
        val (_, subPath) = s.split("|", limit = 2)
        Stage(template = "$subPath/azure-pipelines.yml")
      })
  }

  private fun mapJobs(jobs: Map<String, JobConfig>): List<Job> {
    return jobs.map { (jobName, jobCfg) ->
      Job(
        job = jobName, displayName = jobCfg.displayName.orNull, steps = mapSteps(jobCfg.steps.get())
      )
    }
  }

  @Suppress("DuplicatedCode")
  private fun mapSteps(steps: Map<String, StepConfig>): List<Step> {
    return steps.map { (_, stepCfg) ->
      if (stepCfg.taskName.isPresent) { // task step
        Step(
          task = stepCfg.taskName.get(), displayName = stepCfg.displayName.orNull, inputs = stepCfg.inputs.get().ifEmpty { null })
      } else if (stepCfg.script.isPresent) { // script step
        Step(
          script = stepCfg.script.orNull, displayName = stepCfg.displayName.orNull
        )
      } else {
        Step(script = "echo 'no script or task'")
      }
    }
  }
}