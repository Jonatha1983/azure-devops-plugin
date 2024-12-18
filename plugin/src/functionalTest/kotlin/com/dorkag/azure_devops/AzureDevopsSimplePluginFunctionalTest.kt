package com.dorkag.azure_devops

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
        settingsFile.writeText("rootProject.name = \"azure-devops-plugin-test\"")

        buildFile.writeText(
            """
        plugins {
            id("com.dorkag.azuredevops")
        }

        azurePipeline {
            name.set("Test Pipeline")
            trigger.set(listOf("main", "develop"))
            vmImage.set("ubuntu-20.04")

            parameters {
                create("param1") {
                    displayName.set("Parameter 1")
                    type.set("string")    // This will eventually uppercase
                    default.set("value1")
                }
                create("param2") {
                    displayName.set("Parameter 2")
                    type.set("string")
                    default.set("value2")
                }
            }

            variables.putAll(mapOf("var1" to "value1", "var2" to "value2"))

            stages {
                "ValidatePipeline" {
                    enabled.set(true)
                    displayName.set("Validate Pipeline")
                    condition.set("always()")   // Force a condition so it appears in YAML
                    jobs {
                        "Validate" {
                            displayName.set("Validation Job")
                            steps {
                                "Step1" {
                                    script.set("./gradlew validatePipeline")
                                    displayName.set("Validate Generated Pipeline")
                                }
                            }
                        }
                    }
                }
            }
        }
        """.trimIndent()
        )

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withArguments("generatePipeline")
            .withProjectDir(projectDir)
            .forwardOutput()
            .build()

        println("=== Gradle Build Output ===\n${result.output}")

        val yamlContent = expectedRootPipelineFile.readText().trim()
        println("=== Generated YAML ===\n$yamlContent")

        // If your plugin always uppercases "STRING", change the test expectation to match
        val expectedContent = """
        name: Test Pipeline
        trigger:
          - main
          - develop
        pool:
          vmImage: ubuntu-20.04
        parameters:
          - name: param1
            displayName: Parameter 1
            type: STRING
            default: value1
          - name: param2
            displayName: Parameter 2
            type: STRING
            default: value2
        variables:
          var1: value1
          var2: value2
        stages:
          - stage: ValidatePipeline
            displayName: Validate Pipeline
            condition: always()
            jobs:
              - job: Validate
                displayName: Validation Job
                steps:
                  - script: ./gradlew validatePipeline
                    displayName: Validate Generated Pipeline
    """.trimIndent()

        assertEquals(expectedContent, yamlContent, "Generated YAML content does not match expected content.")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generatePipeline")?.outcome)
    }
}