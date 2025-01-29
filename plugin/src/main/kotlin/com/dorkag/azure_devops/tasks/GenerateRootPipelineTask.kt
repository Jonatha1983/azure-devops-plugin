package com.dorkag.azure_devops.tasks

import com.dorkag.azure_devops.dto.Pool
import com.dorkag.azure_devops.dto.Strategy
import com.dorkag.azure_devops.dto.flow.*
import com.dorkag.azure_devops.extensions.AzurePipelineExtension
import com.dorkag.azure_devops.extensions.config.JobConfig
import com.dorkag.azure_devops.extensions.config.StageConfig
import com.dorkag.azure_devops.extensions.config.StepConfig
import com.dorkag.azure_devops.extensions.config.StrategyConfig
import com.dorkag.azure_devops.utils.AzureCommentMetadataGenerator
import com.dorkag.azure_devops.utils.YamlUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class GenerateRootPipelineTask : DefaultTask() {
  @get:Internal
  abstract val extensionProperty: Property<AzurePipelineExtension>

  @get:Internal
  internal abstract val subprojectReferences: ListProperty<SubprojectRef>

  @get:OutputFile
  abstract val pipelineYaml: RegularFileProperty

  @get:Internal
  abstract val pluginVersion: Property<String>

  @get:Internal
  abstract val gradleVersion: Property<String>

  init {
    pipelineYaml.convention(project.layout.projectDirectory.file("azure-pipelines.yml"))

    // Capture subproject information during configuration
    val subProjects = project.subprojects.filter {
      it.plugins.hasPlugin("com.dorkag.azuredevops")
    }.map { sub ->
      SubprojectRef(sub.name, sub.projectDir.relativeTo(project.projectDir).path)
    }
    subprojectReferences.set(subProjects)

    // Capture versions during configuration
    pluginVersion.set(
      project.plugins.findPlugin("com.dorkag.azuredevops")?.javaClass?.getPackage()?.implementationVersion ?: "development"
    )
    gradleVersion.set(project.gradle.gradleVersion)
  }

  @TaskAction
  fun generate() {
    val extension = extensionProperty.get()
    val hasSubprojects = subprojectReferences.get().isNotEmpty()

    val pipelineDto = if (hasSubprojects) { // If we have subprojects with the plugin, only generate template references
      Pipeline(
        name = extension.name.getOrElse("UnnamedPipeline"),
               trigger = extension.trigger.get().ifEmpty { null },
               pr = extension.pr.orNull?.branches?.get()?.ifEmpty { null },
               pool = Pool(vmImage = extension.vmImage.getOrElse("ubuntu-latest")),
               parameters = null,
               variables = extension.variables.get().ifEmpty { null },
               stages = subprojectReferences.get().map { subproject ->
                 Stage(template = "${subproject.relativePath}/azure-pipelines.yml")
               },
               resources = extension.getResources(),
               schedules = null,
               lockBehavior = extension.lockBehavior.orNull,
               appendCommitMessageToRunName = extension.appendCommitMessageToRunName.orNull
      )
    } else { // If no subprojects, generate the full pipeline from root configuration
      Pipeline(
        name = extension.name.getOrElse("UnnamedPipeline"),
               trigger = extension.trigger.get().ifEmpty { null },
               pr = extension.pr.orNull?.branches?.get()?.ifEmpty { null },
               pool = Pool(vmImage = extension.vmImage.getOrElse("ubuntu-latest")),
               parameters = extension.parameters.map { it.toDto() },
               variables = extension.variables.get().ifEmpty { null },
               stages = mapStages(extension.getStages()),
               resources = extension.getResources(),
               schedules = null,
               lockBehavior = extension.lockBehavior.orNull,
               appendCommitMessageToRunName = extension.appendCommitMessageToRunName.orNull
      )
    }

    val metadataComment = AzureCommentMetadataGenerator.generateMetadataComment(
      pluginVersion.get(), gradleVersion.get()
    )

    val finalYaml = YamlUtil.toYaml(pipelineDto)
    pipelineYaml.get().asFile.writeText(metadataComment + "\n" + finalYaml)

    logger.lifecycle(
      if (hasSubprojects) {
        "Root pipeline generated with subproject references. Wrote to: ${pipelineYaml.get().asFile.absolutePath}"
      } else {
        "Single project pipeline generated. Wrote to: ${pipelineYaml.get().asFile.absolutePath}"
      }
    )
  }

  // Helper data class to store subproject information
  internal data class SubprojectRef(val name: String, val relativePath: String)

  private fun mapStages(stagesMap: Map<String, StageConfig>): List<Stage> {
    return stagesMap.mapNotNull { (stageName, stageConfig) ->
      if (!stageConfig.enabled.get()) {
        null
      } else {
        Stage(
          stage = stageName,
              displayName = stageConfig.displayName.orNull,
              dependsOn = stageConfig.dependsOn.get().ifEmpty { null },
              condition = stageConfig.condition.orNull,
              variables = stageConfig.variables.get().ifEmpty { null },
              template = null,
              parameters = null,
              jobs = mapJobs(stageConfig.jobs.get())
        )
      }
    }
  }

  private fun mapJobs(jobsMap: Map<String, JobConfig>): List<Job> {
    return jobsMap.map { (jobName, jobCfg) ->
      Job(
        job = jobName,
          displayName = jobCfg.displayName.orNull,
          dependsOn = jobCfg.dependsOn.get().ifEmpty { null },
          condition = jobCfg.condition.orNull,
          continueOnError = jobCfg.continueOnError.orNull?.takeIf { it },
          timeoutInMinutes = jobCfg.timeoutInMinutes.orNull?.takeIf { it != 60 },
          strategy = jobCfg.strategy.orNull?.toDto(),
          variables = jobCfg.variables.get().ifEmpty { null },
          steps = mapSteps(jobCfg.steps.get())
      )
    }
  }

  private fun mapSteps(stepsMap: Map<String, StepConfig>): List<Step> {
    return stepsMap.map { (_, stepCfg) -> // Distinguish "task" step from "script" step, fallback if none
      if (!stepCfg.taskName.orNull.isNullOrEmpty()) {
        Step(
          script = null, displayName = stepCfg.displayName.orNull, task = Task(stepCfg.taskName.get(), stepCfg.inputs.get().ifEmpty { null })
        )
      } else if (!stepCfg.script.orNull.isNullOrBlank()) {
        Step(
          script = stepCfg.script.orNull, displayName = stepCfg.displayName.orNull, task = null
        )
      } else {
        Step(
          script = "echo 'No script or task was defined'", displayName = stepCfg.displayName.orNull, task = null
        )
      }
    }
  }

  private fun StrategyConfig.toDto(): Strategy {
    return Strategy(
      type = this.type.orNull, maxParallel = this.maxParallel.orNull, matrix = this.matrix.get().ifEmpty { null })
  }
}