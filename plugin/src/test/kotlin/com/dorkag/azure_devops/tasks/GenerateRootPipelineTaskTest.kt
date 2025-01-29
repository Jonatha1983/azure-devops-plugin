package com.dorkag.azure_devops.tasks

import com.dorkag.azure_devops.extensions.AzurePipelineExtension
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertTrue

class GenerateRootPipelineTaskTest {

  @Test
  fun `test generate pipeline YAML`() {
    val project = ProjectBuilder.builder().build()

    // Use 'register' instead of 'create', and then retrieve the instance with 'get()'
    val generateTask = project.tasks.register("generatePipeline", GenerateRootPipelineTask::class.java).get()

    generateTask.extension = AzurePipelineExtension(project.objects).apply {
      name.set("TestPipeline")
    }

    generateTask.generate()

    val outputFile = project.file("azure-pipelines.yml")
    assertTrue(outputFile.exists())
    assertTrue(outputFile.readText().contains("name: TestPipeline"))
  }
}