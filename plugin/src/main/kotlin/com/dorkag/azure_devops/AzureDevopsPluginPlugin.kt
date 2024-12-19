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

@Suppress("unused")
class AzureDevopsPluginPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (project == project.rootProject) {
            applyToRootProject(project)
            validateBaseConfiguration(project)
        } else {
            applyToSubproject(project)
        }

        project.tasks.register("convertYamlToDsl", GenerateDslFromYamlTask::class.java) {
            it.group = "Azure DevOps"
            it.description = "Generate the Azure DevOps pipeline YAML file."
        }
    }

    private fun applyToRootProject(project: Project) {
        val extension = project.extensions.create(
            "azurePipeline",
            AzurePipelineExtension::class.java,
            project.objects
        )

        project.tasks.register("generatePipeline", GenerateRootPipelineTask::class.java) {
            it.group = "Azure DevOps"
            it.description = "Generate the root Azure DevOps pipeline YAML file."
            it.extension = extension
        }

        project.tasks.register("validatePipeline", ValidatePipelineTask::class.java) {
            it.group = "Azure DevOps"
            it.description = "Validate the Azure DevOps pipeline configuration."
            it.extension = extension
        }
    }

    private fun applyToSubproject(project: Project) {
        val subProjectExtension = project.extensions.create(
            "azurePipeline",
            AzurePipelineSubProjectExtension::class.java,
            project.objects
        )

        project.tasks.register("generateSubprojectTemplate", GenerateSubprojectTemplateTask::class.java) {
            it.group = "Azure DevOps"
            it.description = "Generate the Azure DevOps pipeline template for the subproject."
            it.subProjectExtension = subProjectExtension
        }
    }

    private fun validateBaseConfiguration(project: Project) {
        project.afterEvaluate {

            val requestedTasks = project.gradle.startParameter.taskNames

            // Check if any of the requested tasks require a valid pipeline configuration,
            // For example, if only `generatePipeline` and `validatePipeline` require validation
            val shouldValidate =
                requestedTasks.any { it.contains("generatePipeline") || it.contains("validatePipeline") }

            if (!shouldValidate) {
                // If we don't need to validate for the requested tasks (like when convertYamlToDsl is run),
                // just return and skip the validation logic.
                return@afterEvaluate
            }

            val extension = project.extensions.findByType(AzurePipelineExtension::class.java)
                ?: throw PipelineConfigurationException("AzurePipelineExtension not configured")

            val stages = extension.getStages()
            if (stages.isEmpty()) {
                throw PipelineConfigurationException("At least one stage must be configured in the root pipeline.")
            }


            stages.forEach { (stageName, stageConfig) ->
                if (!stageConfig.enabled.get()) return@forEach

                val jobs = stageConfig.jobs.get()
                if (jobs.isEmpty()) {
                    throw PipelineConfigurationException("Stage '$stageName' must contain at least one job.")
                }

                jobs.forEach { (jobName, jobConfig) ->
                    if (jobConfig.steps.get().isEmpty()) {
                        throw PipelineConfigurationException(
                            "Job '$jobName' in stage '$stageName' must contain at least one step."
                        )
                    }
                }
            }
        }
    }
}