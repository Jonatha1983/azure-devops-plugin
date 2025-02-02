package com.dorkag.azure_devops.tasks


import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AzureDevopsNegativeScenariosFunctionalTest {

  @field:TempDir
  lateinit var projectDir: File

  private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

  @Test
  fun `invalid DSL no stages`() {
    settingsFile.writeText("rootProject.name = \"invalid-dsl-test\"")

    buildFile.writeText(
      """
      plugins {
          id("com.dorkag.azuredevops")
      }

      azurePipeline {
          // No stages at all
          name.set("No Stages Pipeline")
      }
      """.trimIndent()
    )

    val ex = assertThrows(UnexpectedBuildFailure::class.java) {
      GradleRunner.create().withProjectDir(projectDir).withArguments("generatePipeline").withPluginClasspath().forwardOutput().build()
    }

    val message = ex.message ?: ""
    assertTrue(
      message.contains("At least one stage must be configured"), "Expected error about no stages"
    )
  }

  @Test
  fun `job has no steps`() {
    settingsFile.writeText("rootProject.name = \"invalid-job-test\"")

    buildFile.writeText(
      """
      plugins {
          id("com.dorkag.azuredevops")
      }

      azurePipeline {
          name.set("Job No Steps")
          stages {
              stage("Build") {
                  jobs {
                      job("EmptyJob") {
                          // no steps block => expect a validation error
                      }
                  }
              }
          }
      }
      """.trimIndent()
    )

    val ex = assertThrows(UnexpectedBuildFailure::class.java) {
      GradleRunner.create().withProjectDir(projectDir).withArguments("generatePipeline").withPluginClasspath().forwardOutput().build()
    }
    val message = ex.message ?: ""
    assertTrue(message.contains("Job 'EmptyJob' in stage 'Build' must contain at least one step"), "Expected validation error about missing steps.")
  }
}