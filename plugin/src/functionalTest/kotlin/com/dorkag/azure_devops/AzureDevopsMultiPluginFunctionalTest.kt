package com.dorkag.azure_devops

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@KoverAnnotation
class AzureDevopsMultiPluginFunctionalTest {

  @field:TempDir
  lateinit var projectDir: File

  private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

  @Test
  fun `generates pipeline with subproject configuration`() {
    settingsFile.writeText(
      """
            rootProject.name = "azure-devops-plugin-test"
            include("subproject")
        """.trimIndent()
    )

    // Root project DSL - even though we define stages, they shouldn't appear in output
    // because a subproject applies the plugin
    buildFile.writeText(
      """
            plugins {
                id("com.dorkag.azuredevops")
            }

            azurePipeline {
                name.set("Root Pipeline")
                trigger.set(listOf("main", "develop"))
                vmImage.set("ubuntu-20.04")

                stages {
                    stage("Build") {
                        enabled.set(true)
                        displayName.set("Build Project")
                        jobs {
                            job("BuildJob") {
                                displayName.set("Build All Projects")
                                steps {
                                    step("BuildStep") {
                                        script.set("./gradlew build")
                                        displayName.set("Run Gradle Build")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()
    )

    // Subproject that configures its own stage list
    val subprojectDir = projectDir.resolve("subproject").apply { mkdirs() }
    subprojectDir.resolve("build.gradle.kts").writeText(
      """
            plugins {
                id("com.dorkag.azuredevops")
            }

            azurePipeline {
                stages {
                  declaredStage("Build")
                }
            }
        """.trimIndent()
    )

    val result = GradleRunner.create().withPluginClasspath().withArguments("generatePipeline").withProjectDir(projectDir).forwardOutput().build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":generatePipeline")?.outcome)

    val rootPipelineContent = projectDir.resolve("azure-pipelines.yml").readText()
    val strippedContent = stripMetadataComments(rootPipelineContent).trim()

    // Expect only the pipeline basics and template reference, no root stages
    val expectedRootContent = """
            name: Root Pipeline
            trigger:
              - main
              - develop
            pool:
              vmImage: ubuntu-20.04
            stages:
              - template: subproject/azure-pipelines.yml
        """.trimIndent()

    println("=== Generated YAML (without metadata) ===\n$strippedContent")
    println("=== Expected YAML ===\n$expectedRootContent")

    assertEquals(expectedRootContent, strippedContent, "Root pipeline YAML content does not match expected.")
  }
}