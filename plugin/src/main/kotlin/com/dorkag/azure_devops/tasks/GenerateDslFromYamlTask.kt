package com.dorkag.azure_devops.tasks

import com.dorkag.azure_devops.dto.Pipeline
import com.dorkag.azure_devops.dto.Stage
import com.dorkag.azure_devops.dto.Job
import com.dorkag.azure_devops.dto.Step
import com.dorkag.azure_devops.dto.Strategy
import com.dorkag.azure_devops.utils.DslBuilder
import com.dorkag.azure_devops.utils.YamlUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateDslFromYamlTask() : DefaultTask() {

    @get:InputFile
    abstract val inputYaml: RegularFileProperty

    @get:OutputFile
    val outputDsl: RegularFileProperty =
        project.objects.fileProperty().convention(project.layout.buildDirectory.file("generated-dsl.gradle.kts"))

    @TaskAction
    fun generateDsl() {
        val yamlFile: File = inputYaml.get().asFile
        require(yamlFile.exists()) { "YAML file does not exist: ${yamlFile.absolutePath}" }

        val pipelineDto = YamlUtil.fromYaml<Pipeline>(yamlFile.readText())

        val dslSnippet = DslBuilder().apply {
            block("azurePipeline") {
                line("name.set(\"${pipelineDto.name}\")")

                pipelineDto.trigger?.let { branches ->
                    val listString = branches.joinToString(", ") { "\"$it\"" }
                    line("triggerBranches.set(listOf($listString))")
                }

                pipelineDto.pr?.let { prBranches ->
                    val prList = prBranches.joinToString(", ") { "\"$it\"" }
                    block("pullRequestTrigger") {
                        line("branches.set(listOf($prList))")
                    }
                }

                pipelineDto.pool.vmImage.let { image ->
                    line("vmImage.set(\"$image\")")
                }

                pipelineDto.variables?.takeIf { it.isNotEmpty() }?.let { variables ->
                    val formattedVariables = generateVariableAssignment(variables)
                    line("variables.putAll(mapOf($formattedVariables))")
                }

                block("stages") {
                    pipelineDto.stages.forEach { stage ->
                        appendStage(this, stage)
                    }
                }
            }
        }.build()

        val outputFile = outputDsl.orNull?.asFile ?: project.file("generated-dsl.gradle.kts")
        outputFile.writeText(dslSnippet)
        logger.lifecycle("Generated Gradle DSL written to: ${outputFile.absolutePath}")
    }

    private fun appendStage(builder: DslBuilder, stage: Stage) {
        builder.block("\"${stage.stage}\"") {
            stage.displayName?.let { line("displayName.set(\"$it\")") }

            stage.dependsOn?.takeIf { it.isNotEmpty() }?.let { depends ->
                val listStr = depends.joinToString(", ") { "\"$it\"" }
                line("dependsOn.set(listOf($listStr))")
            }

            stage.condition?.let { cond ->
                line("condition.set(\"$cond\")")
            }

            stage.variables?.takeIf { it.isNotEmpty() }?.let { variables ->
                val formattedVariables = generateVariableAssignment(variables)
                line("variables.putAll(mapOf($formattedVariables))")
            }

            stage.jobs?.let { jobs ->
                block("jobs") {
                    jobs.forEach { job ->
                        appendJob(this, job)
                    }
                }
            }
        }
    }

    private fun appendJob(builder: DslBuilder, job: Job) {
        builder.block("\"${job.job}\"") {
            job.displayName?.let { line("displayName.set(\"$it\")") }
            job.condition?.let { line("condition.set(\"$it\")") }

            if (job.steps.isNotEmpty()) {
                block("steps") {
                    job.steps.forEachIndexed { index, step ->
                        appendStep(this, step, index)
                    }
                }
            }

            job.variables?.takeIf { it.isNotEmpty() }?.let { variables ->
                val formattedVariables = generateVariableAssignment(variables)
                line("variables.putAll(mapOf($formattedVariables))")
            }

            job.strategy?.let { strat ->
                appendStrategy(this, strat)
            }
        }
    }

    private fun appendStep(builder: DslBuilder, step: Step, index: Int) {
        val stepKey = "\"step${index + 1}\""
        builder.block(stepKey) {
            if (step.script != null) {
                line("script.set(\"${step.script}\")")
                step.displayName?.let { line("displayName.set(\"$it\")") }
            } else if (step.task != null) {
                line("taskName.set(\"${step.task.name}\")")
                step.displayName?.let { line("displayName.set(\"$it\")") }
                step.task.inputs?.forEach { (k, v) ->
                    line("inputs.put(\"$k\", \"$v\")")
                }
            }
        }
    }

    private fun appendStrategy(builder: DslBuilder, strategy: Strategy) {
        builder.block("strategy") {
            strategy.type?.let { line("type.set(\"$it\")") }
            strategy.maxParallel?.let { line("maxParallel.set($it)") }
            if (!strategy.matrix.isNullOrEmpty()) {
                line("matrix.putAll(mapOf(")
                // Increase indentation just inside the matrix map
                val originalIndent = getIndentLevel(builder)
                setIndentLevel(builder, originalIndent + 1)
                strategy.matrix.forEach { (matrixKey, matrixVal) ->
                    val innerMap = matrixVal.entries.joinToString(", ") { "\"${it.key}\" to \"${it.value}\"" }
                    line("\"$matrixKey\" to mapOf($innerMap),")
                }
                setIndentLevel(builder, originalIndent)
                line("))")
            }
        }
    }

    private fun generateVariableAssignment(variables: Map<String, String>): String {
        return variables.entries.joinToString(", ") { "\"${it.key}\" to \"${it.value}\"" }
    }

    /**
     * Utility functions to manipulate indentation level of DslBuilder if needed.
     * Typically, you'd avoid doing this and rely on `block` but for a custom
     * section like matrix maps, it can be handy.
     */
    private fun getIndentLevel(builder: DslBuilder): Int {
        val field = builder::class.java.getDeclaredField("indentLevel")
        field.isAccessible = true
        return field.get(builder) as Int
    }

    private fun setIndentLevel(builder: DslBuilder, level: Int) {
        val field = builder::class.java.getDeclaredField("indentLevel")
        field.isAccessible = true
        field.set(builder, level)
    }

}