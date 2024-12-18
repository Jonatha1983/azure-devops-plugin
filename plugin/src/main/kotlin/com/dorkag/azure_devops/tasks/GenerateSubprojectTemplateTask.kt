package com.dorkag.azure_devops.tasks

import com.dorkag.azure_devops.extensions.AzurePipelineSubProjectExtension
import com.dorkag.azure_devops.utils.YamlUtil
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class GenerateSubprojectTemplateTask : DefaultTask() {
    @Internal
    lateinit var subProjectExtension: AzurePipelineSubProjectExtension

    @TaskAction
    fun generateSubTemplate() {
        // For demonstration, let's produce a small YAML snippet with 'stages:'
        // listing the subproject's declared stages.

        val subStages = subProjectExtension.stages.get()
        if (subStages.isEmpty()) {
            logger.lifecycle("No stages defined for subproject '${project.name}'. Skipping template generation.")
            return
        }

        val snippetData = mapOf(
            "stages" to subStages.map { stageName -> mapOf("stage" to stageName) })

        // e.g., write out a subproject YAML file named after the subproject
        val outputFile = project.layout.projectDirectory.file("azure-pipelines.yml").asFile
        outputFile.writeText(YamlUtil.toYaml(snippetData))

        logger.lifecycle("Subproject pipeline template generated at: ${outputFile.absolutePath}")
        logger.lifecycle("Template content:\n${outputFile.readText()}")
    }
}