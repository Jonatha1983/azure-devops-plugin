package org.dorkag.azure_devops.tasks

import org.dorkag.azure_devops.dto.Branches
import org.dorkag.azure_devops.dto.Pipeline
import org.dorkag.azure_devops.dto.Pool
import org.dorkag.azure_devops.dto.Trigger
import org.dorkag.azure_devops.extensions.AzurePipelineExtension
import org.dorkag.azure_devops.utils.YamlUtil
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject


abstract class GenerateRootPipelineTask
@Inject constructor(
    objects: ObjectFactory
) : DefaultTask() {

    @Suppress("unused")
    @get:Input
    val pipelineName: String
        get() = project.extensions.findByType(AzurePipelineExtension::class.java)?.name
            ?: throw IllegalStateException("AzurePipelineExtension not configured.")


    @get:OutputFile
    val outputFile: RegularFileProperty =
        objects.fileProperty().convention(project.layout.projectDirectory.file("azure-pipelines.yml"))

    @TaskAction
    fun generatePipeline() {
        val extension = project.extensions.findByType(AzurePipelineExtension::class.java)
            ?: throw IllegalStateException("AzurePipelineExtension not configured.")

        // Validate required fields
        if (extension.name.isBlank()) {
            throw InvalidUserDataException("Pipeline name must be configured")
        }
        if (extension.vmImage.isBlank()) {
            throw InvalidUserDataException("vmImage must be configured")
        }
        if (extension.triggerBranches.isEmpty()) {
            throw InvalidUserDataException("At least one trigger branch must be configured")
        }
        if (extension.stages.isEmpty()) {
            throw InvalidUserDataException("At least one stage must be configured")
        }

        // Validate stages
        extension.stages.forEach { stage ->
            if (stage.jobs.isEmpty()) {
                throw InvalidUserDataException("Stage '${stage.name}' must contain at least one job")
            }
        }

        extension.stages.forEach { stage ->
            if (stage.jobs.isEmpty()) {
                throw InvalidUserDataException("Stage '${stage.name}' must contain at least one job")
            }
        }

        val stages = extension.stages.map { it.toStage() }

        // Create pipeline object
        val pipeline = Pipeline(
            name = extension.name,
            trigger = Trigger(Branches(extension.triggerBranches)),
            pool = Pool(extension.vmImage),
            parameters = extension.parameters,
            variables = extension.variables,
            stages = stages
        )

        // Write a pipeline to YAML
        val pipelineYaml = YamlUtil.toYaml(pipeline)
        outputFile.get().asFile.writeText(pipelineYaml)

    }


}
