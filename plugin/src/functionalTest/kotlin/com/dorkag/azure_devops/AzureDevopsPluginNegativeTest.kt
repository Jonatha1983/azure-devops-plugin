package com.dorkag.azure_devops

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Demonstrates a negative scenario: no stages => fails at validation.
 */
class AzureDevopsPluginNegativeTest {

  @field:TempDir
  lateinit var projectDir: File

  @Test
  fun `root fails if no stages`() {
    projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"failing-test\"")

    projectDir.resolve("build.gradle.kts").writeText(
      """
            plugins {
                id("com.dorkag.azuredevops")
            }

            azurePipeline {
                // name set, but NO stages
                name.set("FailingRoot")
            }
            """.trimIndent()
    )

    val exception = assertThrows(UnexpectedBuildFailure::class.java) {
      GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments("generatePipeline")
        .withPluginClasspath()
        .forwardOutput()
        .build()
    }

    val msg = exception.message ?: ""
    assertTrue(
      msg.contains("At least one stage must be configured in the root pipeline."),
      "Expected error about missing stages in root pipeline, got: $msg"
    )
  }
}