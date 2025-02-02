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
class AzureDevopsGradleTaskFunctionalTest {

  @field:TempDir
  lateinit var projectDir: File

  private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

  @Test
  fun `generates pipeline with Gradle task`() {
    settingsFile.writeText("rootProject.name = \"azure-devops-plugin-test\"")

    buildFile.writeText(
      """
            plugins {
                id("com.dorkag.azuredevops")
            }

            azurePipeline {
                name.set("Gradle Task Pipeline")
                vmImage.set("ubuntu-latest")

                stages {
                    stage("BuildStage") {
                        enabled.set(true)
                        displayName.set("Build Stage")
                        jobs {
                            job("BuildJob") {
                                steps {
                                    step("GradleStep") {
                                        task("Gradle@3") {
                                            displayName.set("Run Gradle Build")
                                            inputs.put("workingDirectory", "")
                                            inputs.put("options", "")
                                            inputs.put("projects", "**/*.gradle")
                                            inputs.put("javaHomeOption", "JDKVersion")
                                            inputs.put("jdkVersion", "1.8")
                                            inputs.put("gradleWrapperFile", "gradlew")
                                            inputs.put("tasks", "clean build")
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

    val result = GradleRunner.create()
      .withProjectDir(projectDir)
      .withArguments("generatePipeline")
      .withPluginClasspath()
      .forwardOutput()
      .build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":generatePipeline")?.outcome)

    // Read the generated YAML
    val yamlFile = projectDir.resolve("azure-pipelines.yml")
    val yamlContent = yamlFile.readText().trim()

    println("=== Generated YAML ===\n$yamlContent")

    // Just do a quick check that "task: Gradle@3" is present
    assertTrue(yamlContent.contains("task: Gradle@3"), "Expected Gradle@3 in the pipeline YAML")
    assertTrue(yamlContent.contains("projects: '**/*.gradle'"), "Expected 'projects' input in the pipeline YAML")
  }
}