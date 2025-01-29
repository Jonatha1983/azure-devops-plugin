package com.dorkag.azure_devops


import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class OnlySubprojectsPluginTest {

  @field:TempDir
  lateinit var projectDir: File

  private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

  @Test
  fun `single subproject applying plugin`() {
    settingsFile.writeText(
      """
            rootProject.name = "azure-devops-plugin-test"
            include("api")
        """.trimIndent()
    )

    // Root project doesn't apply plugin
    buildFile.writeText(
      """
            plugins {
                id("java")
            }
        """.trimIndent()
    )

    // Subproject applies plugin and defines its pipeline
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
                    "Build" {
                        displayName.set("Build API")
                        jobs {
                            "buildJob" {
                                steps {
                                    "build" {
                                        script.set("./gradlew :api:build")
                                        displayName.set("Build API Module")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()
    )

    val result = GradleRunner.create().withProjectDir(projectDir).withArguments(":api:generateSubprojectTemplate").withPluginClasspath().forwardOutput().build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":api:generateSubprojectTemplate")?.outcome)

    val apiPipelineFile = apiDir.resolve("azure-pipelines.yml")
    assertTrue(apiPipelineFile.exists(), "Expected api/azure-pipelines.yml to be created")

    val content = stripMetadataComments(apiPipelineFile.readText()).trim()
    val expectedContent = """
            name: API Pipeline
            trigger:
              - main
            pool:
              vmImage: ubuntu-latest
            stages:
              - stage: Build
                displayName: Build API
                jobs:
                  - job: buildJob
                    displayName: buildJob job
                    steps:
                      - script: ./gradlew :api:build
                        displayName: Build API Module
        """.trimIndent()

    assertEquals(expectedContent, content, "Generated YAML content does not match expected")
  }

  @Test
  fun `multiple subprojects applying plugin`() {
    settingsFile.writeText(
      """
            rootProject.name = "azure-devops-plugin-test"
            include("api", "web")
        """.trimIndent()
    )

    // Root project doesn't apply plugin
    buildFile.writeText(
      """
            plugins {
                id("java")
            }
        """.trimIndent()
    )

    // API subproject
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
                    "Build" {
                        displayName.set("Build API")
                        jobs {
                            "buildJob" {
                                steps {
                                    "build" {
                                        script.set("./gradlew :api:build")
                                        displayName.set("Build API Module")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()
    )

    // Web subproject
    val webDir = projectDir.resolve("web").apply { mkdirs() }
    webDir.resolve("build.gradle.kts").writeText(
      """
            plugins {
                id("com.dorkag.azuredevops")
            }

            azurePipeline {
                name.set("Web Pipeline")
                trigger.set(listOf("main"))
                vmImage.set("ubuntu-latest")
                
                stages {
                    "Build" {
                        displayName.set("Build Web")
                        jobs {
                            "buildJob" {
                                steps {
                                    "build" {
                                        script.set("./gradlew :web:build")
                                        displayName.set("Build Web Module")
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

    // Check API pipeline
    val apiPipelineFile = apiDir.resolve("azure-pipelines.yml")
    assertTrue(apiPipelineFile.exists(), "Expected api/azure-pipelines.yml to be created")
    val apiContent = stripMetadataComments(apiPipelineFile.readText()).trim()
    val expectedApiContent = """
            name: API Pipeline
            trigger:
              - main
            pool:
              vmImage: ubuntu-latest
            stages:
              - stage: Build
                displayName: Build API
                jobs:
                  - job: buildJob
                    displayName: buildJob job
                    steps:
                      - script: ./gradlew :api:build
                        displayName: Build API Module
        """.trimIndent()
    assertEquals(expectedApiContent, apiContent, "API YAML content does not match expected")

    // Check Web pipeline
    val webPipelineFile = webDir.resolve("azure-pipelines.yml")
    assertTrue(webPipelineFile.exists(), "Expected web/azure-pipelines.yml to be created")
    val webContent = stripMetadataComments(webPipelineFile.readText()).trim()
    val expectedWebContent = """
            name: Web Pipeline
            trigger:
              - main
            pool:
              vmImage: ubuntu-latest
            stages:
              - stage: Build
                displayName: Build Web
                jobs:
                  - job: buildJob
                    displayName: buildJob job
                    steps:
                      - script: ./gradlew :web:build
                        displayName: Build Web Module
        """.trimIndent()
    assertEquals(expectedWebContent, webContent, "Web YAML content does not match expected")
  }
}