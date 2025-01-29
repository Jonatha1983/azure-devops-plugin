package com.dorkag.azure_devops.tasks

import com.dorkag.azure_devops.extensions.AzurePipelineExtension
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertTrue

class GenerateRootPipelineTaskTest {

  @Test
  fun `test generate pipeline YAML`() {
    val project = ProjectBuilder.builder().build()

    // Register and retrieve the task instance
    val generateTask = project.tasks.register("generatePipeline", GenerateRootPipelineTask::class.java).get()

    // Create the extension instance
    val extension = AzurePipelineExtension(project.objects).apply {
      name.set("TestPipeline")
    }

    // Assign the extension *via* the property
    generateTask.extensionProperty.set(extension)

    // Optionally override the output file if you want a custom path:
    // generateTask.pipelineYaml.set(project.layout.projectDirectory.file("my-pipeline.yml"))

    // Execute the task action
    generateTask.generate()

    // Retrieve the actual file path from the task property
    val outputFile = generateTask.pipelineYaml.get().asFile
    assertTrue(outputFile.exists(), "Expected the YAML file to be created.")
    assertTrue(outputFile.readText().contains("name: TestPipeline"))
  }
}