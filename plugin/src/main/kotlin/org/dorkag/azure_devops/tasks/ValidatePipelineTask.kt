package org.dorkag.azure_devops.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class ValidatePipelineTask : DefaultTask() {
    @TaskAction
    fun validatePipeline() {


        val generatedFile = File(project.rootDir, "azure-pipelines.yml")
        val existingFile = File(project.rootDir, "azure-pipelines-current.yml")

        if (!generatedFile.exists() || !existingFile.exists()) {
            throw IllegalStateException("Pipeline validation failed: YAML files not found.")
        }

        if (generatedFile.readText() != existingFile.readText()) {
            throw IllegalStateException("Pipeline validation failed: Generated YAML does not match the existing pipeline.")
        }

        println("Pipeline validation succeeded.")
    }
}
