package com.dorkag.azure_devops.tasks

import com.dorkag.azure_devops.dto.flow.Pipeline
import com.dorkag.azure_devops.dto.flow.Stage
import com.dorkag.azure_devops.dto.flow.Job
import com.dorkag.azure_devops.dto.flow.Step
import com.dorkag.azure_devops.utils.AzureDevOpsPipelineConstants
import com.dorkag.azure_devops.utils.DslBuilder
import com.dorkag.azure_devops.utils.YamlUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Enhanced task to generate Azure Pipeline configuration DSL from a YAML file.
 * Provides detailed feedback on unsupported features and validation errors.
 */
abstract class GenerateDslFromYamlTask : DefaultTask() {

  @get:InputFile
  abstract val inputYaml: RegularFileProperty

  @get:OutputFile
  val outputDsl: RegularFileProperty = project.objects.fileProperty().convention(project.layout.buildDirectory.file("generated-dsl.gradle.kts"))

  // Track unsupported features for reporting
  private val unsupportedFeatures = mutableListOf<String>()

  @TaskAction
  fun generateDsl() {
    val yamlFile: File = inputYaml.get().asFile
    require(yamlFile.exists()) { "YAML file does not exist: ${yamlFile.absolutePath}" }

    try {
      val pipelineDto = YamlUtil.fromYaml<Pipeline>(yamlFile.readText())
      validatePipelineFeatures(pipelineDto)

      val dslSnippet = DslBuilder().apply {
        block(AzureDevOpsPipelineConstants.AZURE_PIPELINE) {
          generateBasicConfig(pipelineDto)
          generateTriggerConfig(pipelineDto)
          generateVariablesConfig(pipelineDto)
          generateStagesConfig(pipelineDto)
        }
      }.build()

      writeOutputAndReport(dslSnippet)
    } catch (e: Exception) {
      throw IllegalStateException("Failed to convert YAML to DSL: ${e.message}", e)
    }
  }

  private fun validatePipelineFeatures(pipeline: Pipeline) { // Check for unsupported features
    if (pipeline.resources != null) {
      unsupportedFeatures.add("Pipeline resources configuration")
    }

    if (pipeline.parameters.isNullOrEmpty().not()) {
      unsupportedFeatures.add("Pipeline parameters")
    }

    pipeline.stages.forEach { stage ->
      if (stage.template != null) {
        unsupportedFeatures.add("Stage templates (stage: ${stage.stage})")
      }
    }
  }

  private fun DslBuilder.generateBasicConfig(pipeline: Pipeline) {
    line("name.set(\"${pipeline.name}\")")

    pipeline.pool.vmImage.let {
      line("vmImage.set(\"$it\")")
    }
  }

  private fun DslBuilder.generateTriggerConfig(pipeline: Pipeline) {
    pipeline.trigger?.let { branches ->
      val branchList = branches.joinToString(", ") { "\"$it\"" }
      line("${AzureDevOpsPipelineConstants.TRIGGER}.set(listOf($branchList))")
    }

    pipeline.pr?.let { prBranches ->
      block(AzureDevOpsPipelineConstants.PR) {
        val branchList = prBranches.joinToString(", ") { "\"$it\"" }
        line("${AzureDevOpsPipelineConstants.BRANCHES}.set(listOf($branchList))")
      }
    }
  }

  private fun DslBuilder.generateVariablesConfig(pipeline: Pipeline) {
    pipeline.variables?.takeIf { it.isNotEmpty() }?.let { vars ->
      val varStrings = vars.map { (k, v) -> "\"$k\" to \"$v\"" }
      line("variables.putAll(mapOf(")
      varStrings.forEach { line("    $it,") }
      line("))")
    }
  }

  private fun DslBuilder.generateStagesConfig(pipeline: Pipeline) {
    block(AzureDevOpsPipelineConstants.STAGES) {
      pipeline.stages.forEach { stage ->
        if (stage.template == null) { // Skip template stages
          generateStage(stage)
        }
      }
    }
  }

  private fun DslBuilder.generateStage(stage: Stage) {
    block("${AzureDevOpsPipelineConstants.STAGE}(\"${stage.stage}\")") {
      stage.displayName?.let { line("${AzureDevOpsPipelineConstants.DISPLAY_NAME}.set(\"$it\")") }
      stage.dependsOn?.takeIf { it.isNotEmpty() }?.let { deps ->
        val depList = deps.joinToString(", ") { "\"$it\"" }
        line("${AzureDevOpsPipelineConstants.DEPENDS_ON}.set(listOf($depList))")
      }

      stage.jobs?.let { jobs ->
        block(AzureDevOpsPipelineConstants.JOBS) {
          jobs.forEach { job -> generateJob(job) }
        }
      }
    }
  }

  private fun DslBuilder.generateJob(job: Job) {
    block("${AzureDevOpsPipelineConstants.JOB}(\"${job.job}\")") {
      job.displayName?.let { line("${AzureDevOpsPipelineConstants.DISPLAY_NAME}.set(\"$it\")") }
      job.condition?.let { line("${AzureDevOpsPipelineConstants.CONDITION}.set(\"$it\")") }

      if (job.steps.isNotEmpty()) {
        block(AzureDevOpsPipelineConstants.STEPS) {
          job.steps.forEachIndexed { index, step ->
            generateStep(step, index)
          }
        }
      }
    }
  }

  private fun DslBuilder.generateStep(step: Step, index: Int) {
    val stepName = step.displayName?.replace(Regex("[^A-Za-z0-9]"), "") ?: "${AzureDevOpsPipelineConstants.STEP}${index + 1}"
    block("${AzureDevOpsPipelineConstants.STEP}(\"$stepName\")") {
      step.displayName?.let { line("${AzureDevOpsPipelineConstants.DISPLAY_NAME}.set(\"$it\")") }

      when {
        step.script != null -> {
          line("${AzureDevOpsPipelineConstants.SCRIPT}.set(\"\"\"${step.script}\"\"\")") // Use triple quotes for scripts
        }
        step.task != null -> {
          line("${AzureDevOpsPipelineConstants.TASK}(\"${step.task}\") {")
          step.inputs?.forEach { (k, v) ->
            line("    ${AzureDevOpsPipelineConstants.INPUTS}.put(\"$k\", \"$v\")")
          }
          line("}")
        }
      }
    }
  }

  private fun writeOutputAndReport(dslSnippet: String) {
    val outputFile = outputDsl.get().asFile
    outputFile.writeText(dslSnippet)

    logger.lifecycle("Generated Gradle DSL written to: ${outputFile.absolutePath}")

    if (unsupportedFeatures.isNotEmpty()) {
      logger.warn("The following features were not converted:")
      unsupportedFeatures.forEach { feature ->
        logger.warn("- $feature")
      }
      throw IllegalStateException(
        "YAML contains unsupported features: ${unsupportedFeatures.joinToString(", ")}"
      )
    }
  }
}