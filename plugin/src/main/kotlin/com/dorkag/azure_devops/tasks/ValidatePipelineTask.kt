package com.dorkag.azure_devops.tasks

import com.dorkag.azure_devops.exceptions.PipelineConfigurationException
import com.dorkag.azure_devops.extensions.AzurePipelineExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class ValidatePipelineTask : DefaultTask() {

    @Internal
    lateinit var extension: AzurePipelineExtension

    @TaskAction
    fun validate() {
        val stages = extension.getStages()

        if (stages.isEmpty()) {
            throw PipelineConfigurationException("At least one stage must be configured in the root pipeline.")
        }


        stages.forEach { (stageName, stageConfig) ->
            if (stageConfig.jobs.get().isEmpty()) {
                throw PipelineConfigurationException("Stage '$stageName' must contain at least one job.")
            }

            stageConfig.jobs.get().forEach { (jobName, jobConfig) ->
                if (jobConfig.steps.get().isEmpty()) {
                    throw PipelineConfigurationException(
                        "Job '$jobName' in stage '$stageName' must contain at least one step."
                    )
                }
            }
        }

        logger.lifecycle("Pipeline configuration is valid.")
    }
}