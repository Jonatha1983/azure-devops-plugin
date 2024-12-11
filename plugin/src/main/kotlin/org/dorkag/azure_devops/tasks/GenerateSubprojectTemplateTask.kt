package org.dorkag.azure_devops.tasks

import org.dorkag.azure_devops.dto.Branches
import org.dorkag.azure_devops.dto.Job
import org.dorkag.azure_devops.dto.Pipeline
import org.dorkag.azure_devops.dto.Pool
import org.dorkag.azure_devops.dto.Stage
import org.dorkag.azure_devops.dto.Step
import org.dorkag.azure_devops.dto.Trigger
import org.dorkag.azure_devops.utils.YamlUtil
import org.dorkag.azure_devops.extensions.AzurePipelineSubProjectExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

abstract class GenerateSubprojectTemplateTask : DefaultTask() {
    @TaskAction
    fun generateTemplate() {
        val subprojectConfig = project.extensions.findByType(AzurePipelineSubProjectExtension::class.java) ?: return

        if (!subprojectConfig.enabled) return

        val pipeline = Pipeline(
            name = "Subproject ${project.name.capitalize()}",
            trigger = Trigger(Branches(listOf("main"))),
            pool = Pool("ubuntu-latest"),
            stages = subprojectConfig.stages.map { stage ->
                Stage(
                    stage = stage, jobs = listOf(
                        Job(
                            job = "${stage}Job", steps = listOf(
                                Step(
                                    script = "./gradlew :${project.name}:$stage", displayName = "$stage Job"
                                )
                            )
                        )
                    )
                )
            })

        val pipelineYaml = YamlUtil.toYaml(pipeline)
        val outputFile = project.layout.projectDirectory.file("azure-pipelines-template.yml")
        outputFile.asFile.writeText(pipelineYaml)
        println("Subproject template generated at ${outputFile.asFile.absolutePath}")
    }
}
