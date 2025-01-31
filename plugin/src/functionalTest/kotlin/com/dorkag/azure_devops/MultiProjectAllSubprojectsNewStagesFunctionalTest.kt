package com.dorkag.azure_devops


import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Scenario 4: All subprojects apply plugin, root has a 'Build' stage,
 * subprojects reuse 'Build' + declare new stage(s).
 */
class MultiProjectAllSubprojectsNewStagesFunctionalTest {

  @field:TempDir
  lateinit var projectDir: File

  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
  private val rootBuildFile by lazy { projectDir.resolve("build.gradle.kts") }

  @Test
  fun `all subprojects apply plugin - root stage plus new subproject stages`() {
    settingsFile.writeText(
      """
            rootProject.name = "multi-all-plus-new-stages"
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
                vmImage.set("ubuntu-latest")
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
            
            // subA reuses 'Build' + declares new 'TestA'
            azurePipeline {
                stages {
                    declaredStage("Build")
                    stage("TestA") {
                         displayName.set("TestA Stage")
                          jobs {
                              job("TestAJob") {
                                  displayName.set("TestAJob")
                                  steps {
                                      step("TestAStep") {
                                          script.set("./gradlew test")
                                          displayName.set("TestA Step")
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
    val subBDir = projectDir.resolve("subB").apply { mkdirs() }
    subBDir.resolve("build.gradle.kts").writeText(
      """
            plugins {
                id("com.dorkag.azuredevops")
            }
            
            // subB reuses 'Build' + declares new 'TestB'
            azurePipeline {
                stages {
                  declaredStage("Build")
                  stage("TestB") { 
                      displayName.set("TestB Stage")
                      jobs {
                          job("TestBJob") {
                              displayName.set("TestBJob")
                              steps {
                                  step("TestBStep") {
                                      script.set("./gradlew test")
                                      displayName.set("TestB Step")
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

    // Root pipeline
    val rootPipelineContent = projectDir.resolve("azure-pipelines.yml").readText()
    assertTrue(rootPipelineContent.contains("Root Pipeline"), "Root pipeline name missing")

    // subA => has azure-pipelines.yml with "Build" + "TestA"
    val subAPipeline = subADir.resolve("azure-pipelines.yml").readText()
    assertTrue(subAPipeline.contains("- stage: Build"), "subA should mention 'Build'")
    assertTrue(subAPipeline.contains("- stage: TestA"), "subA should mention 'TestA'")

    // subB => has azure-pipelines.yml with "Build" + "TestB"
    val subBPipeline = subBDir.resolve("azure-pipelines.yml").readText()
    assertTrue(subBPipeline.contains("- stage: Build"), "subB should mention 'Build'")
    assertTrue(subBPipeline.contains("- stage: TestB"), "subB should mention 'TestB'")
  }
}