package org.dorkag.azure_devops

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AzureDevopsSimplePluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val expectedRootPipelineFile by lazy { projectDir.resolve("azure-pipelines.yml") }

    @Test
    fun `generates root pipeline file`() {
        // Set up the test build
        settingsFile.writeText(
            """
            rootProject.name = "azure-devops-plugin-test"
        """.trimIndent()
        )

        buildFile.writeText(
            """
            plugins {
                id("org.dorkag.azuredevops")
            }

            azurePipeline {
                name = "Test Pipeline"
                triggerBranches = listOf("main", "develop")
                vmImage = "ubuntu-20.04"
                parameters = mapOf("param1" to "value1", "param2" to "value2")
                variables = mapOf("var1" to "value1", "var2" to "value2")
                 stages {
                    create("ValidatePipeline") {
                        displayName = "Validate Pipeline"
                        jobs {
                            create("Validate") {
                                displayName = "Validation Job"
                                steps {
                                    create("Step1") {
                                        script = "./gradlew validatePipeline"
                                        displayName = "Validate Generated Pipeline"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        )

        // Run the generatePipeline task
        val runner =
            GradleRunner.create().withPluginClasspath().withArguments("generatePipeline").withProjectDir(projectDir)
                .forwardOutput()
        val result = runner.build()

        // Verify the task outcome
        assertEquals(TaskOutcome.SUCCESS, result.task(":generatePipeline")?.outcome)

        // Verify the generated file exists
        assertTrue(expectedRootPipelineFile.exists(), "The root pipeline file should exist.")

        // Verify the content of the YAML file
        val generatedYaml = expectedRootPipelineFile.readText()
        val expectedContent = """
            name: Test Pipeline
            trigger:
              branches:
                include:
                  - main
                  - develop
            pool:
              vmImage: ubuntu-20.04
            parameters:
              param1: value1
              param2: value2
            variables:
              var1: value1
              var2: value2
            stages:
              - stage: ValidatePipeline
                displayName: Validate Pipeline
                jobs:
                  - job: Validate
                    displayName: Validation Job
                    steps:
                      - script: ./gradlew validatePipeline
                        displayName: Validate Generated Pipeline
        """.trimIndent()

        assertEquals(expectedContent, generatedYaml.trim(), "Generated YAML content does not match expected content.")
    }
}

