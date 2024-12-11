package org.dorkag.azure_devops

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
        // Set up the test build with a subproject
        settingsFile.writeText("""
        rootProject.name = "azure-devops-plugin-test"
        include("subproject")
    """.trimIndent())

        // Root project configuration
        buildFile.writeText("""
        plugins {
            id("org.dorkag.azuredevops")
        }

        azurePipeline {
            name = "Root Pipeline"
            triggerBranches = listOf("main", "develop")
            vmImage = "ubuntu-20.04"
            stages {
                create("Build") {
                    displayName = "Build Project"
                    jobs {
                        create("BuildJob") {
                            displayName = "Build All Projects"
                            steps {
                                create("BuildStep") {
                                    script = "./gradlew build"
                                    displayName = "Run Gradle Build"
                                }
                            }
                        }
                    }
                }
            }
        }
    """.trimIndent())

        // Subproject configuration
        val subprojectDir = projectDir.resolve("subproject")
        subprojectDir.mkdirs()
        subprojectDir.resolve("build.gradle.kts").writeText("""
        plugins {
            id("org.dorkag.azuredevops")
        }

        azurePipeline {
            enabled = true
            stages = listOf("Build", "Test")
        }
    """.trimIndent())

        // Run the generation task
        val result = GradleRunner.create()
            .withPluginClasspath()
            .withArguments("generatePipeline")  // Only generate the root pipeline for now
            .withProjectDir(projectDir)
            .forwardOutput()
            .build()

        // Verify task outcome
        assertEquals(TaskOutcome.SUCCESS, result.task(":generatePipeline")?.outcome)

        // Verify root pipeline content
        val rootPipelineContent = projectDir.resolve("azure-pipelines.yml").readText()

        val expectedRootContent = """
        name: Root Pipeline
        trigger:
          branches:
            include:
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

        assertEquals(expectedRootContent, rootPipelineContent.trim())
    }

}