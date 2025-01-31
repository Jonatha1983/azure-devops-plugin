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
class AzureDevopsMavenTaskFunctionalTest {

  @field:TempDir
  lateinit var projectDir: File

  private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

  @Test
  fun `generates pipeline with Maven task`() {
    settingsFile.writeText("rootProject.name = \"azure-devops-plugin-test\"")

    buildFile.writeText(
      """
            plugins {
                id("com.dorkag.azuredevops")
            }

            azurePipeline {
                name.set("Maven Task Pipeline")
                vmImage.set("ubuntu-latest")

                stages {
                    stage("BuildMavenStage") {
                        enabled.set(true)
                        displayName.set("Build Maven Project")
                        jobs {
                            job("MavenJob") {
                                steps {
                                    step("MavenBuildStep") {
                                        task("Maven@3") {
                                            displayName.set("Run Maven Build")
                                            inputs.put("mavenPomFile", "**/pom.xml")
                                            inputs.put("goals", "clean install")
                                            inputs.put("javaHomeOption", "JDKVersion")
                                            inputs.put("jdkVersion", "1.8")
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

    assertTrue(yamlContent.contains("task: Maven@3"), "Expected Maven@3 in the pipeline YAML")
    assertTrue(yamlContent.contains("goals: clean install"), "Expected 'goals: clean install' in the pipeline YAML")
  }
}