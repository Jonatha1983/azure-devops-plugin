package com.dorkag.azure_devops.tasks


import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AzureDevopsGradleSecondTaskFunctionalTest {

  @field:TempDir
  lateinit var projectDir: File

  private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

  @Test
  fun `multiple Gradle steps in single project`() {
    settingsFile.writeText("rootProject.name = \"azure-devops-plugin-test\"")

    buildFile.writeText(
      """
      plugins {
          id("com.dorkag.azuredevops")
      }

      azurePipeline {
          name.set("Multiple Gradle Steps Pipeline")
          vmImage.set("ubuntu-latest")

          stages {
              stage("BuildStage") {
                  displayName.set("Build Stage")
                  jobs {
                      job("GradleBuildJob") {
                          steps {
                              step("GradleClean") {
                                  task("Gradle@3") {
                                      displayName.set("Run Gradle Clean")
                                      inputs.put("tasks", "clean")
                                  }
                              }
                              step("GradleAssemble") {
                                  task("Gradle@3") {
                                      displayName.set("Run Gradle Assemble")
                                      inputs.put("tasks", "assemble")
                                  }
                              }
                              step("GradleTest") {
                                  task("Gradle@3") {
                                      displayName.set("Run Gradle Test")
                                      inputs.put("tasks", "test")
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
    val content = yamlFile.readText().trim()
    println("=== Generated YAML ===\n$content")

    assertTrue(content.contains("task: Gradle@3"), "Expected 'Gradle@3' tasks in the pipeline YAML")
    assertTrue(content.contains("tasks: clean"), "Expected 'tasks: clean'")
    assertTrue(content.contains("tasks: assemble"), "Expected 'tasks: assemble'")
    assertTrue(content.contains("tasks: test"), "Expected 'tasks: test'")
  }
}