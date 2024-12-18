package com.dorkag.azure_devops

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

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

        // Root project DSL
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
                    "Build" {
                        enabled.set(true)
                        displayName.set("Build Project")
                        jobs {
                            "BuildJob" {
                                displayName.set("Build All Projects")
                                steps {
                                    "BuildStep" {
                                        script.set("./gradlew build")
                                        displayName.set("Run Gradle Build")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            """
        )

        // Subproject that configures its own stage list
        val subprojectDir = projectDir.resolve("subproject").apply { mkdirs() }
        subprojectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("com.dorkag.azuredevops")
            }

            azurePipeline {
                stages.set(listOf("Build", "Test"))
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withArguments("generatePipeline")
            .withProjectDir(projectDir)
            .forwardOutput()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generatePipeline")?.outcome)

        val rootPipelineContent = projectDir.resolve("azure-pipelines.yml").readText().trim()

        // Adjust the final expected YAML as needed. The plugin might or might not integrate
        // the subproject stages automatically. If your plugin merges them, you might see "Build"
        // from the root plus "Test" from subproject. If the subproject generates its own snippet,
        // or if you rely on separate tasks, adapt the expected accordingly.
        val expectedRootContent = """
            name: Root Pipeline
            trigger:
              - main
              - develop
            pool:
              vmImage: ubuntu-20.04
            stages:
              - stage: Build
                displayName: Build Project
                jobs:
                  - job: BuildJob
                    displayName: Build All Projects
                    steps:
                      - script: ./gradlew build
                        displayName: Run Gradle Build
        """.trimIndent()

        assertEquals(expectedRootContent, rootPipelineContent, "Root pipeline YAML content does not match expected.")
    }
}