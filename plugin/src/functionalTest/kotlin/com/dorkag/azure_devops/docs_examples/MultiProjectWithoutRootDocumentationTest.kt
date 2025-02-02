package com.dorkag.azure_devops.docs_examples


import com.dorkag.azure_devops.getResourceAsText
import com.dorkag.azure_devops.stripMetadataComments
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Test class that verifies the multi-project without root documentation example.
 */
class MultiProjectWithoutRootDocumentationTest {

  @field:TempDir
  lateinit var projectDir: File

  private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
  private val buildFileContent = getResourceAsText("/codeSnippets/multiproject-without-root/build.gradle.kts")
  private val settingsFileContent = getResourceAsText("/codeSnippets/multiproject-without-root/settings.gradle.kts")
  private val expectedSubA = getResourceAsText("/codeSnippets/multiproject-without-root/subA/azure-pipelines.yml")

  @Test
  fun `multi-project without root pipeline example`() { // Create project structure
    settingsFile.writeText(settingsFileContent)
    buildFile.writeText(buildFileContent)

    // Create subproject A
    val subADir = projectDir.resolve("subA").apply { mkdirs() }
    val subABuildFile = subADir.resolve("build.gradle.kts")
    subABuildFile.writeText(getResourceAsText("/codeSnippets/multiproject-without-root/subA/build.gradle.kts"))

    // Run the pipeline generation
    val result = GradleRunner.create().withProjectDir(projectDir).withArguments("generatePipeline").withPluginClasspath().forwardOutput().build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":generatePipeline")?.outcome)

    // Verify root has no pipeline
    val rootYamlFile = projectDir.resolve("azure-pipelines.yml")
    assertFalse(rootYamlFile.exists(), "Root should not have pipeline file")

    // Verify subproject A pipeline
    val subAYamlFile = subADir.resolve("azure-pipelines.yml")
    assertTrue(subAYamlFile.exists(), "SubA pipeline file should exist")
    assertEquals(
      expectedSubA.trim(), stripMetadataComments(subAYamlFile.readText().trim()), "SubA pipeline content should match"
    )
  }
}