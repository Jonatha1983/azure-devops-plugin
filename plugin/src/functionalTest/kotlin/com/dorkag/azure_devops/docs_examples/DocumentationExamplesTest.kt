package com.dorkag.azure_devops.docs_examples

import com.dorkag.azure_devops.getResourceAsText
import com.dorkag.azure_devops.stripMetadataComments
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Test class that verifies the documentation examples.
 */
class DocumentationExamplesTest {

  @field:TempDir
  lateinit var projectDir: File

  private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
  private val buildFileContent = getResourceAsText("/codeSnippets/basic/build.gradle.kts")
  private val settingsFileContent = getResourceAsText("/codeSnippets/basic/settings.gradle.kts")

  private val expected = getResourceAsText("/codeSnippets/basic/basic-example.yml")

  @Test
  fun `basic gradle task example`() {

    settingsFile.writeText(settingsFileContent)
    buildFile.writeText(buildFileContent)

    val result = GradleRunner.create().withProjectDir(projectDir).withArguments("generatePipeline").withPluginClasspath().forwardOutput().build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":generatePipeline")?.outcome)

    val yamlFile = projectDir.resolve("azure-pipelines.yml")
    assertTrue(yamlFile.exists())
    assertEquals(expected.trim(), stripMetadataComments(yamlFile.readText().trim()))

  }
}