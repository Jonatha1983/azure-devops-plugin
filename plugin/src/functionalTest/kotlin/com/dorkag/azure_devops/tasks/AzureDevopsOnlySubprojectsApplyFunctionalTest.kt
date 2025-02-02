package com.dorkag.azure_devops.tasks


import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AzureDevopsOnlySubprojectsApplyFunctionalTest {

  @field:TempDir
  lateinit var projectDir: File

  private val rootBuildFile by lazy { projectDir.resolve("build.gradle.kts") }
  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

  @Test
  fun `multi project, only subprojects apply plugin`() {
    settingsFile.writeText(
      """
      rootProject.name = "only-subprojects-apply-plugin"
      include("api","service")
      """.trimIndent()
    )

    // Root does not apply plugin
    rootBuildFile.writeText(
      """
      plugins {
          id("java")
      }
      """.trimIndent()
    )

    // subproject 1 => apply plugin
    val apiDir = projectDir.resolve("api").apply { mkdirs() }
    apiDir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("com.dorkag.azuredevops")
      }

      azurePipeline {
          name.set("API Pipeline")
          trigger.set(listOf("main"))
          vmImage.set("ubuntu-latest")

          stages {
              stage("BuildApi") {
                  displayName.set("Build API")
                  jobs {
                      job("buildJob") {
                          steps {
                              step("buildApiStep") {
                                  script.set("./gradlew :api:build")
                                  displayName.set("API Build Step")
                              }
                          }
                      }
                  }
              }
          }
      }
      """.trimIndent()
    )

    // subproject 2 => apply plugin
    val serviceDir = projectDir.resolve("service").apply { mkdirs() }
    serviceDir.resolve("build.gradle.kts").writeText(
      """
      plugins {
          id("com.dorkag.azuredevops")
      }

      azurePipeline {
          name.set("Service Pipeline")
          trigger.set(listOf("develop"))
          vmImage.set("ubuntu-latest")

          stages {
              stage("BuildService") {
                  displayName.set("Build Service")
                  jobs {
                      job("serviceJob") {
                          steps {
                              step("buildServiceStep") {
                                  script.set("./gradlew :service:build")
                                  displayName.set("Service Build Step")
                              }
                          }
                      }
                  }
              }
          }
      }
      """.trimIndent()
    )

    // Run aggregator? Or just run "generatePipeline"?
    // Depends on your plugin logic. Usually "generatePipeline" is at root,
    // but root doesn't apply plugin => aggregator might not exist.
    // So we run each subproject's "generateSubprojectTemplate".
    val apiResult = GradleRunner.create().withProjectDir(projectDir).withArguments(":api:generateSubprojectTemplate").withPluginClasspath().forwardOutput().build()
    val serviceResult = GradleRunner.create().withProjectDir(projectDir).withArguments(":service:generateSubprojectTemplate").withPluginClasspath().forwardOutput().build()

    assertEquals(TaskOutcome.SUCCESS, apiResult.task(":api:generateSubprojectTemplate")?.outcome)
    assertEquals(TaskOutcome.SUCCESS, serviceResult.task(":service:generateSubprojectTemplate")?.outcome)

    // Check api pipeline
    val apiPipelineFile = apiDir.resolve("azure-pipelines.yml")
    assertTrue(apiPipelineFile.exists(), "api azure-pipelines.yml missing")
    val apiContent = apiPipelineFile.readText().trim()
    assertTrue(apiContent.contains("API Pipeline"), "Missing API pipeline name")
    assertTrue(apiContent.contains("BuildApi"), "Missing BuildApi stage")

    // Check the service pipeline
    val servicePipelineFile = serviceDir.resolve("azure-pipelines.yml")
    assertTrue(servicePipelineFile.exists(), "service azure-pipelines.yml missing")
    val serviceContent = servicePipelineFile.readText().trim()
    assertTrue(serviceContent.contains("Service Pipeline"), "Missing Service pipeline name")
    assertTrue(serviceContent.contains("Build Service"), "Missing stage displayName 'Build Service'")
  }
}