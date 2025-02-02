package com.dorkag.azure_devops.tasks

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AzureDevopsNpmTaskFunctionalTest {

  @field:TempDir
  lateinit var projectDir: File

  private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

  @Test
  fun `single project with NPM task`() {
    settingsFile.writeText("rootProject.name = \"azure-devops-plugin-test\"")

    // Root build script (single project applying plugin)
    buildFile.writeText(
      """
      plugins {
          id("com.dorkag.azuredevops")
      }

      azurePipeline {
          name.set("NPM Task Pipeline")
          vmImage.set("ubuntu-latest")

          stages {
              stage("NpmStage") {
                  displayName.set("NPM Stage")
                  jobs {
                      job("NpmJob") {
                          steps {
                              step("NpmBuild") {
                                  task("Npm@1") {
                                      displayName.set("Run NPM Build")
                                      inputs.put("command", "install")
                                      inputs.put("workingDir", "./")
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

    val yamlFile = projectDir.resolve("azure-pipelines.yml")
    assertTrue(yamlFile.exists(), "Expected azure-pipelines.yml to be created")

    val content = yamlFile.readText().trim()
    println("=== Generated YAML ===\n$content")

    assertTrue(content.contains("task: Npm@1"), "Expected 'Npm@1' in the pipeline YAML")
    assertTrue(content.contains("command: install"), "Expected 'command: install' input in the pipeline YAML")
  }
}