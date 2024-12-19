package com.dorkag.azure_devops.tasks

import com.dorkag.azure_devops.dto.*
import com.dorkag.azure_devops.dto.flow.Job
import com.dorkag.azure_devops.dto.flow.Pipeline
import com.dorkag.azure_devops.dto.flow.Stage
import com.dorkag.azure_devops.dto.flow.Step
import com.dorkag.azure_devops.dto.flow.Task
import com.dorkag.azure_devops.extensions.AzurePipelineExtension
import com.dorkag.azure_devops.extensions.config.JobConfig
import com.dorkag.azure_devops.extensions.config.StageConfig
import com.dorkag.azure_devops.extensions.config.StepConfig
import com.dorkag.azure_devops.extensions.config.StrategyConfig
import com.dorkag.azure_devops.utils.YamlUtil
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class GenerateRootPipelineTask : DefaultTask() {

    @Internal
    lateinit var extension: AzurePipelineExtension

    @TaskAction
    fun generate() {
        // Create the Pipeline DTO
        val pipelineDto = Pipeline(
            name = extension.name.getOrElse("UnnamedPipeline"),
            trigger = extension.trigger.get().ifEmpty { null },  // top-level list
            pr = extension.pr.orNull?.branches?.get()?.ifEmpty { null },
            pool = Pool(vmImage = extension.vmImage.getOrElse("ubuntu-latest")),
            parameters = extension.parameters.map { it.toDto() },
            variables = extension.variables.get().ifEmpty { null },
            stages = mapStages(extension.getStages()),
            resources = extension.getResources(),
            schedules = null,  // optional
            lockBehavior = extension.lockBehavior.orNull,
            appendCommitMessageToRunName = extension.appendCommitMessageToRunName.orNull
        )

        val yaml = YamlUtil.toYaml(pipelineDto)
        val outputFile = project.file("azure-pipelines.yml")
        outputFile.writeText(yaml)

        logger.lifecycle("Pipeline YAML file generated at: ${outputFile.absolutePath}\n\n$yaml")
    }

    private fun mapStages(stagesMap: Map<String, StageConfig>): List<Stage> {
        return stagesMap.mapNotNull { (stageName, stageConfig) ->
            if (!stageConfig.enabled.get()) {
                null // skip disabled
            } else {
                val stageCondition = stageConfig.condition.orNull
                val stageVars = stageConfig.variables.get()

                // Convert the jobs from DSL to a DTO list
                val stageJobs = mapJobs(stageConfig.jobs.get())

                Stage(
                    stage = stageName,
                    displayName = stageConfig.displayName.orNull,
                    dependsOn = stageConfig.dependsOn.get().ifEmpty { null },
                    condition = stageCondition,
                    variables = if (stageVars.isEmpty()) null else stageVars,
                    template = null,
                    parameters = null,
                    jobs = stageJobs  // <--- NOW we store the jobs in the Stage DTO
                )
            }
        }
    }

    private fun mapJobs(jobsMap: Map<String, JobConfig>): List<Job> {
        return jobsMap.map { (jobName, jobCfg) ->
            val jobCondition = jobCfg.condition.orNull
            val jobVars = jobCfg.variables.get()
            val stepsDto = mapSteps(jobCfg.steps.get())

            Job(
                job = jobName,
                displayName = jobCfg.displayName.orNull,
                dependsOn = jobCfg.dependsOn.get().ifEmpty { null },
                condition = jobCondition,
                continueOnError = jobCfg.continueOnError.orNull?.takeIf { it },
                timeoutInMinutes = jobCfg.timeoutInMinutes.orNull?.takeIf { it != 60 },
                strategy = jobCfg.strategy.orNull?.toDto(),
                variables = if (jobVars.isEmpty()) null else jobVars,
                steps = stepsDto
            )
        }
    }

    private fun mapSteps(stepsMap: Map<String, StepConfig>): List<Step> {
        return stepsMap.map { (_, stepCfg) ->
            // If stepCfg.taskName is not empty, it's a "task" step
            if (!stepCfg.taskName.orNull.isNullOrEmpty()) {
                // Convert to the Step DTO with 'task: SomeTask@Version'
                Step(
                    script = null, displayName = stepCfg.displayName.orNull, task = Task(
                        name = stepCfg.taskName.get(), inputs = stepCfg.inputs.get().ifEmpty { null })
                )
            } else if (!stepCfg.script.orNull.isNullOrBlank()) {
                // It's a script step
                Step(
                    script = stepCfg.script.orNull, displayName = stepCfg.displayName.orNull, task = null
                )
            } else {
                // fallback if user didn't set script nor task
                Step(
                    script = "echo 'No script or task was defined'",
                    displayName = stepCfg.displayName.orNull,
                    task = null
                )
            }
        }
    }


    private fun StrategyConfig.toDto(): Strategy {
        return Strategy(
            type = this.type.orNull, maxParallel = this.maxParallel.orNull, matrix = this.matrix.get().ifEmpty { null })
    }
}