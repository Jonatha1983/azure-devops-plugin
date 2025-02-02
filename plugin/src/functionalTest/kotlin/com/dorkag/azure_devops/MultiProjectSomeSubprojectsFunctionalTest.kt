package com.dorkag.azure_devops

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Scenario 3: Multi-project, some subprojects apply the plugin, some do not.
 * Subprojects re-use the root's "Build" stage if they apply the plugin.
 */
class MultiProjectSomeSubprojectsFunctionalTest {

  @field:TempDir
  lateinit var projectDir: File

  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
  private val rootBuildFile by lazy { projectDir.resolve("build.gradle.kts") }

  @Test
  fun `some subprojects apply plugin - reusing root stage`() {
    settingsFile.writeText(
      """
            rootProject.name = "multi-some-subprojects"
            include("subA", "subB")
            """.trimIndent()
    )

    // Root
    rootBuildFile.writeText(
      """
            plugins {
                id("com.dorkag.azuredevops")
            }

            azurePipeline {
                name.set("Root Pipeline")
                stages {
                    stage("Build") {
                        displayName.set("Root Build Stage")
                        jobs {
                            job("rootBuildJob") {
                                displayName.set("RootBuildJob")
                                steps {
                                    step("rootBuildStep") {
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

    // subA => applies plugin
    val subADir = projectDir.resolve("subA").apply { mkdirs() }
    subADir.resolve("build.gradle.kts").writeText(
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

    // subB => does NOT apply plugin
    val subBDir = projectDir.resolve("subB").apply { mkdirs() }
    subBDir.resolve("build.gradle.kts").writeText(
      """
            plugins {
                kotlin("jvm") version "1.8.0"
            }

            // No 'com.dorkag.azuredevops' => no azure-pipelines.yml
            """.trimIndent()
    )

    val result = GradleRunner.create().withProjectDir(projectDir).withArguments("generatePipeline").withPluginClasspath().forwardOutput().build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":generatePipeline")?.outcome)

    // Root pipeline
    val rootPipelineFile = projectDir.resolve("azure-pipelines.yml")
    assertTrue(rootPipelineFile.exists(), "Root azure-pipelines.yml should be generated")
    val rootContent = rootPipelineFile.readText()
    assertTrue(rootContent.contains("Root Pipeline"), "Should mention 'Root Pipeline' in root YAML")

    // subA => has plugin => file generated
    val subAPipelineFile = subADir.resolve("azure-pipelines.yml")
    assertTrue(subAPipelineFile.exists(), "subA azure-pipelines.yml should be generated")
    val subAContent = subAPipelineFile.readText()
    assertTrue(subAContent.contains("- stage: Build"), "subA references 'Build' stage")

    // subB => no plugin => no file
    val subBPipelineFile = subBDir.resolve("azure-pipelines.yml")
    assertFalse(subBPipelineFile.exists(), "subB does not apply plugin => no azure-pipelines.yml")
  }
}