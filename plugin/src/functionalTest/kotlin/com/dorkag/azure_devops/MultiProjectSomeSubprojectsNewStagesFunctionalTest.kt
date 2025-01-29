package com.dorkag.azure_devops

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Scenario 5: Some subprojects apply plugin, root has a 'Build' stage,
 * subprojects reuse 'Build' + declare new stages,
 * subprojects that don't apply plugin get no pipeline.
 */
class MultiProjectSomeSubprojectsNewStagesFunctionalTest {

  @field:TempDir
  lateinit var projectDir: File

  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
  private val rootBuildFile by lazy { projectDir.resolve("build.gradle.kts") }

  @Test
  fun `some subprojects apply plugin - root stage plus new subproject stage`() {
    settingsFile.writeText(
      """
            rootProject.name = "multi-some-plus-new-stages"
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
                    "Build" {
                        jobs {
                            "rootBuildJob" {
                                steps {
                                    "rootBuildStep" {
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

    // subA => applies plugin, has "Build" + "TestA"
    val subADir = projectDir.resolve("subA").apply { mkdirs() }
    subADir.resolve("build.gradle.kts").writeText(
      """
            plugins {
                id("com.dorkag.azuredevops")
            }

            azurePipeline {
                stages.set(listOf("Build", "TestA"))
            }
            """.trimIndent()
    )

    // subB => does NOT apply plugin
    val subBDir = projectDir.resolve("subB").apply { mkdirs() }
    subBDir.resolve("build.gradle.kts").writeText(
      """
            plugins {
                id("java")
            }
            """.trimIndent()
    )

    val result = GradleRunner.create().withProjectDir(projectDir).withArguments("generatePipeline").withPluginClasspath().forwardOutput().build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":generatePipeline")?.outcome)

    // Root pipeline
    val rootPipelineContent = projectDir.resolve("azure-pipelines.yml").readText()
    assertTrue(rootPipelineContent.contains("Root Pipeline"), "Missing root pipeline name")

    // subA => has azure-pipelines.yml with "Build" + "TestA"
    val subAPipelineFile = subADir.resolve("azure-pipelines.yml")
    assertTrue(subAPipelineFile.exists(), "subA azure-pipelines.yml should exist")
    val subAContent = subAPipelineFile.readText()
    assertTrue(subAContent.contains("- stage: Build"), "subA pipeline should mention 'Build'")
    assertTrue(subAContent.contains("- stage: TestA"), "subA pipeline should mention 'TestA'")

    // subB => no plugin => no azure-pipelines.yml
    val subBPipelineFile = subBDir.resolve("azure-pipelines.yml")
    assertFalse(subBPipelineFile.exists(), "subB does not apply plugin => no pipeline file")
  }
}