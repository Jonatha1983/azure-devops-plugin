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
 * Test class that verifies the multi-project with root documentation example.
 */
class MultiProjectWithRootDocumentationTest {

  @field:TempDir
  lateinit var projectDir: File

  private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
  private val buildFileContent = getResourceAsText("/codeSnippets/multiproject-with-root/build.gradle.kts")
  private val settingsFileContent = getResourceAsText("/codeSnippets/multiproject-with-root/settings.gradle.kts")

  private val expectedRoot = getResourceAsText("/codeSnippets/multiproject-with-root/azure-pipelines.yml")
  private val expectedSubA = getResourceAsText("/codeSnippets/multiproject-with-root/subA/azure-pipelines.yml")
  private val expectedSubB = getResourceAsText("/codeSnippets/multiproject-with-root/subB/azure-pipelines.yml")

  @Test
  fun `multi-project with root pipeline example`() { // Create project structure
    settingsFile.writeText(settingsFileContent)
    buildFile.writeText(buildFileContent)

    // Create subproject A
    val subADir = projectDir.resolve("subA").apply { mkdirs() }
    val subABuildFile = subADir.resolve("build.gradle.kts")
    subABuildFile.writeText(getResourceAsText("/codeSnippets/multiproject-with-root/subA/build.gradle.kts"))

    // Create subproject B
    val subBDir = projectDir.resolve("subB").apply { mkdirs() }
    val subBBuildFile = subBDir.resolve("build.gradle.kts")
    subBBuildFile.writeText(getResourceAsText("/codeSnippets/multiproject-with-root/subB/build.gradle.kts"))

    // Run the pipeline generation
    val result = GradleRunner.create().withProjectDir(projectDir).withArguments("generatePipeline").withPluginClasspath().forwardOutput().build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":generatePipeline")?.outcome)

    // Verify root pipeline
    val rootYamlFile = projectDir.resolve("azure-pipelines.yml")
    assertTrue(rootYamlFile.exists(), "Root pipeline file should exist")
    assertEquals(
      expectedRoot.trim(), stripMetadataComments(rootYamlFile.readText().trim()), "Root pipeline content should match"
    )

    // Verify subproject A pipeline
    val subAYamlFile = subADir.resolve("azure-pipelines.yml")
    assertTrue(subAYamlFile.exists(), "SubA pipeline file should exist")
    assertEquals(
      expectedSubA.trim(), stripMetadataComments(subAYamlFile.readText().trim()), "SubA pipeline content should match"
    )

    // Verify subproject B pipeline
    val subBYamlFile = subBDir.resolve("azure-pipelines.yml")
    assertTrue(subBYamlFile.exists(), "SubB pipeline file should exist")
    assertEquals(
      expectedSubB.trim(), stripMetadataComments(subBYamlFile.readText().trim()), "SubB pipeline content should match"
    )
  }
}