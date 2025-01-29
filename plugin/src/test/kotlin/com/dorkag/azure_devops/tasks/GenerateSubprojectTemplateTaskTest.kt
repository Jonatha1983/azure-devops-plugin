package com.dorkag.azure_devops.tasks

import com.dorkag.azure_devops.extensions.AzurePipelineSubProjectExtension
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertTrue

class GenerateSubprojectTemplateTaskTest {

  @Test
  fun `test generate subproject template with no stages`() {
    val project = ProjectBuilder.builder().build()
    val generateTask = project.tasks.register("generateSubTemplate", GenerateSubprojectTemplateTask::class.java).get()
    generateTask.subProjectExtension = AzurePipelineSubProjectExtension(project.objects)

    generateTask.generateSubTemplate()
  }

  @Test
  fun `test generate subproject template with stages`() {
    val project = ProjectBuilder.builder().build()
    val generateTask = project.tasks.register("generateSubTemplate", GenerateSubprojectTemplateTask::class.java).get()
    generateTask.subProjectExtension = AzurePipelineSubProjectExtension(project.objects).apply {
      stages.set(listOf("stage1", "stage2"))
    }

    generateTask.generateSubTemplate()

    val outputFile = project.file("azure-pipelines.yml")
    assertTrue(outputFile.exists())
    assertTrue(outputFile.readText().contains("stage1"))
    assertTrue(outputFile.readText().contains("stage2"))
  }
}