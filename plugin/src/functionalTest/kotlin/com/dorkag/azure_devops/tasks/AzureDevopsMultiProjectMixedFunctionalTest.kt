package com.dorkag.azure_devops.tasks


import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AzureDevopsMultiProjectMixedFunctionalTest {

  @field:TempDir
  lateinit var projectDir: File

  private val rootBuildFile by lazy { projectDir.resolve("build.gradle.kts") }
  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

  @Test
  fun `multi project with root and subprojects applying plugin`() {
    settingsFile.writeText(
      """
      rootProject.name = "multi-mixed-plugin-test"
      include("serviceA","serviceB")
      """.trimIndent()
    )

    // Root applies plugin
    rootBuildFile.writeText(
      """
      plugins {
          id("com.dorkag.azuredevops")
      }

      azurePipeline {
          name.set("Root Aggregator Pipeline")
          vmImage.set("ubuntu-latest")

          stages {
              stage("RootStage") {
                  displayName.set("Root Stage")
                  jobs {
                      job("rootOnlyJob") {
                          steps {
                              step("rootStep") {
                                  script.set("echo 'Root Only Step'")
                                  displayName.set("Root Step")
                              }
                          }
                      }
                  }
              }
          }
      }
      """.trimIndent()
    )

    // subA
    val serviceADir = projectDir.resolve("serviceA").apply { mkdirs() }
    serviceADir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("com.dorkag.azuredevops")
      }

      azurePipeline {
          name.set("ServiceA Pipeline")
          trigger.set(listOf("main"))
          vmImage.set("ubuntu-latest")

          stages {
              stage("BuildA") {
                  displayName.set("Build ServiceA")
                  jobs {
                      job("jobA") {
                          steps {
                              step("buildA") {
                                  script.set("./gradlew :serviceA:build")
                                  displayName.set("Build ServiceA Step")
                              }
                          }
                      }
                  }
              }
          }
      }
      """.trimIndent()
    )

    // subB
    val serviceBDir = projectDir.resolve("serviceB").apply { mkdirs() }
    serviceBDir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("com.dorkag.azuredevops")
      }

      azurePipeline {
          name.set("ServiceB Pipeline")
          trigger.set(listOf("develop"))
          vmImage.set("ubuntu-latest")

          stages {
              stage("BuildB") {
                  displayName.set("Build ServiceB")
                  jobs {
                      job("jobB") {
                          steps {
                              step("buildB") {
                                  script.set("./gradlew :serviceB:build")
                                  displayName.set("Build ServiceB Step")
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

    // Root pipeline references subprojects
    val rootPipeline = projectDir.resolve("azure-pipelines.yml")
    assertTrue(rootPipeline.exists(), "Root azure-pipelines.yml missing")
    val rootYaml = rootPipeline.readText().trim()
    println("=== Root YAML ===\n$rootYaml")

    // subA pipeline
    val serviceAPipeline = serviceADir.resolve("azure-pipelines.yml")
    assertTrue(serviceAPipeline.exists(), "serviceA azure-pipelines.yml missing")
    val serviceAContent = serviceAPipeline.readText().trim()
    assertTrue(serviceAContent.contains("Build ServiceA"), "Missing stage 'Build ServiceA' in A")

    // subB pipeline
    val serviceBPipeline = serviceBDir.resolve("azure-pipelines.yml")
    assertTrue(serviceBPipeline.exists(), "serviceB azure-pipelines.yml missing")
    val serviceBContent = serviceBPipeline.readText().trim()
    assertTrue(serviceBContent.contains("Build ServiceB"), "Missing stage 'Build ServiceB' in B")

    // Root aggregator might contain references:
    // e.g. "stages: - template: serviceA/azure-pipelines.yml" if aggregator is referencing subproject templates
    assertTrue(
      rootYaml.contains("template: serviceA/azure-pipelines.yml") || rootYaml.contains("template: serviceB/azure-pipelines.yml"),
      "Root aggregator should reference subprojects' templates if your aggregator does that."
    )
  }
}