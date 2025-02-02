package com.dorkag.azure_devops.tasks

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AzureDevopsOnlyRootAppliesFunctionalTest {

  @field:TempDir
  lateinit var projectDir: File

  private val rootBuildFile by lazy { projectDir.resolve("build.gradle.kts") }
  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

  @Test
  fun `multi project, only root applies plugin`() {
    settingsFile.writeText(
      """
      rootProject.name = "root-only-plugin-test"
      include("sub1","sub2")
      """.trimIndent()
    )

    // Root build script: applies plugin
    rootBuildFile.writeText(
      """
      plugins {
          id("com.dorkag.azuredevops")
      }

      azurePipeline {
          name.set("Root Only Pipeline")
          trigger.set(listOf("main"))
          vmImage.set("ubuntu-latest")

          stages {
              stage("RootStage") {
                  displayName.set("Root Stage Only")
                  jobs {
                      job("rootJob") {
                          steps {
                              step("rootStep") {
                                  script.set("./gradlew build")
                                  displayName.set("Root Build Step")
                              }
                          }
                      }
                  }
              }
          }
      }
      """.trimIndent()
    )

    // sub1, sub2 do NOT apply the plugin
    val sub1 = projectDir.resolve("sub1").apply { mkdirs() }
    sub1.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("java")
      }
      """.trimIndent()
    )
    val sub2 = projectDir.resolve("sub2").apply { mkdirs() }
    sub2.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("java")
      }
      """.trimIndent()
    )

    val result = GradleRunner.create().withProjectDir(projectDir).withArguments("generatePipeline").withPluginClasspath().forwardOutput().build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":generatePipeline")?.outcome)

    val pipelineFile = projectDir.resolve("azure-pipelines.yml")
    assertTrue(pipelineFile.exists(), "azure-pipelines.yml should exist at root")

    val content = pipelineFile.readText().trim()
    println("=== Generated YAML ===\n$content")

    // Expect no references to sub1 or sub2 as templates, just root's pipeline
    assertTrue(content.contains("Root Only Pipeline"), "Should mention pipeline name")
    assertTrue(content.contains("RootStage"), "Should contain stage 'RootStage'")
    assertTrue(content.contains("rootJob"), "Should contain job 'rootJob'")
    assertFalse(content.contains("template: sub1/azure-pipelines.yml"), "No sub1 template reference")
    assertFalse(content.contains("template: sub2/azure-pipelines.yml"), "No sub2 template reference")
  }
}