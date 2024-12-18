package com.dorkag.azure_devops.tasks

import com.dorkag.azure_devops.dto.Pipeline
import com.dorkag.azure_devops.utils.YamlUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateDslFromYamlTask : DefaultTask() {

    @get:InputFile
    abstract val inputYaml: RegularFileProperty

    /**
     * Optionally, specify an output file for the generated DSL snippet.
     * If not set, we'll default to "generated-dsl.gradle.kts"
     */
    @get:OutputFile
    @get:Optional
    abstract val outputDsl: RegularFileProperty

    @TaskAction
    fun generateDsl() {
        val yamlFile: File = inputYaml.get().asFile
        require(yamlFile.exists()) { "YAML file does not exist: ${yamlFile.absolutePath}" }

        // Parse the pipeline YAML into your Pipeline DTO (or a similar structure)
        val pipelineDto = YamlUtil.fromYaml<Pipeline>(yamlFile.readText())

        // Build the Gradle DSL lines
        val dslSnippet = buildString {
            appendLine("azurePipeline {")

            // pipeline name
            appendLine("    name.set(\"${pipelineDto.name}\")")

            // triggers as a list of branches
            pipelineDto.trigger?.let { branches ->
                val listString = branches.joinToString(", ") { "\"$it\"" }
                appendLine("    triggerBranches.set(listOf($listString))")
            }

            // pr as a list of branches
            pipelineDto.pr?.let { prBranches ->
                val prList = prBranches.joinToString(", ") { "\"$it\"" }
                appendLine("    pullRequestTrigger {\n        branches.set(listOf($prList))\n    }")
            }

            // vmImage
            pipelineDto.pool.vmImage.let { image ->
                appendLine("    vmImage.set(\"$image\")")
            }

            // variables
            pipelineDto.variables?.takeIf { it.isNotEmpty() }?.let { vars ->
                val varAssignments = vars.entries.joinToString(", ") { "\"${it.key}\" to \"${it.value}\"" }
                appendLine("    variables.putAll(mapOf($varAssignments))")
            }

            // Stages
            pipelineDto.stages.let { stages ->
                appendLine("    stages {")
                for (stage in stages) {
                    appendStage(stage)
                }
                appendLine("    }")
            }

            appendLine("}") // end of azurePipeline block
        }

        // Write DSL snippet to file
        val outputFile = outputDsl.orNull?.asFile ?: project.file("generated-dsl.gradle.kts")
        outputFile.writeText(dslSnippet)
        logger.lifecycle("Generated Gradle DSL written to: ${outputFile.absolutePath}")
    }

    private fun StringBuilder.appendStage(stage: com.dorkag.azure_devops.dto.Stage) {
        appendLine("        \"${stage.stage}\" {")
        // optionally set displayName
        stage.displayName?.let { appendLine("            displayName.set(\"$it\")") }

        // dependsOn
        stage.dependsOn?.takeIf { it.isNotEmpty() }?.let { depends ->
            val listStr = depends.joinToString(", ") { "\"$it\"" }
            appendLine("            dependsOn.set(listOf($listStr))")
        }

        // condition
        stage.condition?.let { cond ->
            appendLine("            condition.set(\"$cond\")")
        }

        // variables
        stage.variables?.takeIf { it.isNotEmpty() }?.let { mapVars ->
            val varAssignments = mapVars.entries.joinToString(", ") { "\"${it.key}\" to \"${it.value}\"" }
            appendLine("            variables.putAll(mapOf($varAssignments))")
        }

        // jobs
        if (stage.jobs != null) {
            appendLine("            jobs {")
            for (job in stage.jobs) {
                appendLine("                \"${job.job}\" {")
                job.displayName?.let { appendLine("                    displayName.set(\"$it\")") }
                job.condition?.let { appendLine("                    condition.set(\"$it\")") }

                // steps
                if (job.steps.isNotEmpty()) {
                    appendLine("                    steps {")
                    job.steps.forEachIndexed { index, step ->
                        val stepKey = "step${index + 1}"
                        appendLine("                        \"$stepKey\" {")
                        if (step.script != null) {
                            appendLine("                            script.set(\"${step.script}\")")
                            step.displayName?.let { appendLine("                            displayName.set(\"$it\")") }
                        } else if (step.task != null) {
                            appendLine("                            taskName.set(\"${step.task.name}\")")
                            step.displayName?.let { appendLine("                            displayName.set(\"$it\")") }
                            step.task.inputs?.forEach { (k, v) ->
                                appendLine("                            inputs.put(\"$k\", \"$v\")")
                            }
                        }
                        appendLine("                        }")
                    }
                    appendLine("                    }")
                }

                // job variables
                job.variables?.takeIf { it.isNotEmpty() }?.let { jVars ->
                    val varAssignments = jVars.entries.joinToString(", ") { "\"${it.key}\" to \"${it.value}\"" }
                    appendLine("                    variables.putAll(mapOf($varAssignments))")
                }

                // strategy
                job.strategy?.let { strat ->
                    appendLine("                    strategy {")
                    strat.type?.let { appendLine("                        type.set(\"$it\")") }
                    strat.maxParallel?.let { appendLine("                        maxParallel.set($it)") }
                    if (strat.matrix != null && strat.matrix.isNotEmpty()) {
                        appendLine("                        matrix.putAll(mapOf(")
                        strat.matrix.forEach { (matrixKey, matrixVal) ->
                            // matrixVal is a map
                            val innerMap = matrixVal.entries.joinToString(", ") { "\"${it.key}\" to \"${it.value}\"" }
                            appendLine("                            \"$matrixKey\" to mapOf($innerMap),")
                        }
                        appendLine("                        ))")
                    }
                    appendLine("                    }")
                }

                appendLine("                }") // end job
            }
            appendLine("            }") // end jobs
        }

        appendLine("        }") // end "stageName"
    }
}