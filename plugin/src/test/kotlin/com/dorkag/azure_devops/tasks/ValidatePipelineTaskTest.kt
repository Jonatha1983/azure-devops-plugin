package com.dorkag.azure_devops.tasks

import com.dorkag.azure_devops.exceptions.PipelineConfigurationException
import com.dorkag.azure_devops.extensions.AzurePipelineExtension
import com.dorkag.azure_devops.extensions.config.JobConfig
import com.dorkag.azure_devops.extensions.config.StageConfig
import com.dorkag.azure_devops.extensions.config.StepConfig
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ValidatePipelineTaskTest {

  @Test
  fun `test validate pipeline with empty stages throws exception`() {
    val project = ProjectBuilder.builder().build()
    val validateTask = project.tasks.register("validate", ValidatePipelineTask::class.java).get()
    validateTask.extension = AzurePipelineExtension(project.objects)

    assertFailsWith<PipelineConfigurationException> {
      validateTask.validate()
    }.let { exception ->
      assertEquals("At least one stage must be configured in the root pipeline.", exception.message)
    }
  }

  @Test
  fun `test validate pipeline with empty jobs throws exception`() {
    val project = ProjectBuilder.builder().build()
    val validateTask = project.tasks.register("validate", ValidatePipelineTask::class.java).get()
    val extension = AzurePipelineExtension(project.objects)

    // Create a stage with no jobs
    val stageConfig = project.objects.newInstance(StageConfig::class.java).apply {
      jobs.putAll(emptyMap()) // Fixed: Use `putAll` for empty Map instead of `set`
    }

    extension.stages.put("stage1", stageConfig) // Correctly add the stage
    validateTask.extension = extension

    assertFailsWith<PipelineConfigurationException> {
      validateTask.validate()
    }.let { exception ->
      assertEquals("Stage 'stage1' must contain at least one job.", exception.message)
    }
  }

  @Test
  fun `test validate pipeline with valid configuration`() {
    val project = ProjectBuilder.builder().build()
    val validateTask = project.tasks.register("validate", ValidatePipelineTask::class.java).get()
    val extension = AzurePipelineExtension(project.objects)

    // Create a valid stage configuration with jobs and steps
    val stageConfig = project.objects.newInstance(StageConfig::class.java).apply {
      jobs.put(
        "job1",
        project.objects.newInstance(JobConfig::class.java).apply {
          steps.putAll(
            mapOf(
              "step1" to project.objects.newInstance(StepConfig::class.java)
            )
          )
        }
      )
    }

    extension.stages.put("stage1", stageConfig) // Correctly add the stage
    validateTask.extension = extension

    // Validate the pipeline; no exception should be thrown
    validateTask.validate()
  }
}