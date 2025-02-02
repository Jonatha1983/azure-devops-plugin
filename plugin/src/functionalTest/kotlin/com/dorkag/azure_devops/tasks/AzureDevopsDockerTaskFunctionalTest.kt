package com.dorkag.azure_devops.tasks

import com.dorkag.azure_devops.KoverAnnotation
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@KoverAnnotation
class AzureDevopsDockerTaskFunctionalTest {

  @field:TempDir
  lateinit var projectDir: File

  private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

  @Test
  fun `generates pipeline with Docker task`() {
    settingsFile.writeText("rootProject.name = \"azure-devops-plugin-test\"")

    buildFile.writeText(
      """
            plugins {
                id("com.dorkag.azuredevops")
            }

            azurePipeline {
                name.set("Docker Task Pipeline")
                vmImage.set("ubuntu-latest")

                stages {
                    stage("BuildDockerImageStage") {
                        enabled.set(true)
                        displayName.set("Build Docker Image")
                        jobs {
                            job("DockerJob") {
                                steps {
                                    step("DockerBuildStep") {
                                        task("Docker@2") {
                                            displayName.set("Build Docker image")
                                            inputs.put("command", "build")
                                            inputs.put("dockerfile", "Dockerfile")
                                            inputs.put("containerName", "my-container")
                                            inputs.put("tags", "latest")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            """.trimIndent()
    )

    val result = GradleRunner.create().withProjectDir(projectDir).withArguments("generatePipeline").withPluginClasspath().forwardOutput().build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":generatePipeline")?.outcome)

    val yamlFile = projectDir.resolve("azure-pipelines.yml")
    val yamlContent = yamlFile.readText().trim()

    println("=== Generated YAML ===\n$yamlContent")

    assertTrue(yamlContent.contains("task: Docker@2"), "Expected Docker@2 in the pipeline YAML")
    assertTrue(yamlContent.contains("command: build"), "Expected 'command: build' in the pipeline YAML")
  }
}