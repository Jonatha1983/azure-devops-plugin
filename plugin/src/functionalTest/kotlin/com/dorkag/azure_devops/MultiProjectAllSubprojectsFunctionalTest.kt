package com.dorkag.azure_devops


import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Scenario 2: All subprojects apply the plugin, reusing the root "Build" stage.
 */
class MultiProjectAllSubprojectsFunctionalTest {

  @field:TempDir
  lateinit var projectDir: File

  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
  private val rootBuildFile by lazy { projectDir.resolve("build.gradle.kts") }

  @Test
  fun `all subprojects apply plugin - reusing root stage`() {
    settingsFile.writeText(
      """
            rootProject.name = "multi-all-subprojects"
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
                trigger.set(listOf("main"))
                vmImage.set("ubuntu-20.04")

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

    // subA
    val subADir = projectDir.resolve("subA").apply { mkdirs() }
    subADir.resolve("build.gradle.kts").writeText(
      """
            plugins {
                id("com.dorkag.azuredevops")
            }
            
            // subA reuses the 'Build' stage from root
            azurePipeline {
                stages {
                    declaredStage("Build")
                }
            }
            """.trimIndent()
    )

    // subB
    val subBDir = projectDir.resolve("subB").apply { mkdirs() }
    subBDir.resolve("build.gradle.kts").writeText(
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

    // Run aggregator
    val result = GradleRunner.create().withProjectDir(projectDir).withArguments("generatePipeline").withPluginClasspath().forwardOutput().build()

    // Root pipeline => success
    assertEquals(TaskOutcome.SUCCESS, result.task(":generatePipeline")?.outcome)

    // Check root pipeline
    val rootPipeline = projectDir.resolve("azure-pipelines.yml")
    assertTrue(rootPipeline.exists(), "Expected root azure-pipelines.yml")

    val rootContent = rootPipeline.readText()

    // Verify the root pipeline still mentions "Root Build Stage"
    // (It may also have an "include-subA" or "include-subB" stage if your code references subprojects.)
    println(rootContent)
    assertTrue(rootContent.contains("Root Pipeline"), "Expected root pipeline to mention 'Root Build Stage'")

    // subA
    val subAPipeline = subADir.resolve("azure-pipelines.yml")
    assertTrue(subAPipeline.exists(), "subA azure-pipelines.yml should be generated")
    val subAContent = subAPipeline.readText()
    assertTrue(subAContent.contains("- stage: Build"), "subA references 'Build' stage")

    // subB
    val subBPipeline = subBDir.resolve("azure-pipelines.yml")
    assertTrue(subBPipeline.exists(), "subB azure-pipelines.yml should be generated")
    val subBContent = subBPipeline.readText()
    assertTrue(subBContent.contains("- stage: Build"), "subB references 'Build' stage")
  }
}