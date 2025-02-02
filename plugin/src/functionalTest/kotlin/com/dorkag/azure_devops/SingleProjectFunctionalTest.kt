package com.dorkag.azure_devops

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Scenario 1: Single project only (no subprojects).
 */
class SingleProjectFunctionalTest {

  @field:TempDir
  lateinit var projectDir: File

  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
  private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }

  @Test
  fun `single project with root pipeline only`() { // No subprojects => empty settings
    settingsFile.writeText("rootProject.name = \"single-project-test\"")

    // Root build config
    buildFile.writeText(
      """
            plugins {
                id("com.dorkag.azuredevops")
            }
            
            azurePipeline {
                name.set("SingleProjectPipeline")
                trigger.set(listOf("main"))
                vmImage.set("ubuntu-latest")

                stages {
                    stage("Build") {
                        displayName.set("Build the project")
                        jobs {
                            job("buildJob") {
                                displayName.set("BuildJob")
                                steps {
                                    step("runBuild") {
                                        script.set("./gradlew build")
                                        displayName.set("Run Build")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            """.trimIndent()
    )

    // Run "generatePipeline" which triggers root & aggregator
    val result = GradleRunner.create().withProjectDir(projectDir).withArguments("generatePipeline").withPluginClasspath().forwardOutput().build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":generatePipeline")?.outcome)

    // Check generated root pipeline
    val rootPipelineFile = projectDir.resolve("azure-pipelines.yml")
    assertTrue(rootPipelineFile.exists(), "Expected root azure-pipelines.yml to be created")

    val content = rootPipelineFile.readText() // Check for root pipeline name and stage
    assertTrue(content.contains("name: SingleProjectPipeline"), "Missing pipeline name")
  }
}