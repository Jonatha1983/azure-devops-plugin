package com.dorkag.azure_devops

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@KoverAnnotation
class AzureDevOpsMultipleStagesWithJobs {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }

    @Test
    fun `generates pipeline with multiple stages and jobs`() {
        projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"azure-devops-plugin-test\"")

        buildFile.writeText(
            """
        plugins {
            id("com.dorkag.azuredevops")
        }

        azurePipeline {
            name.set("Multi-Stage Pipeline")
            vmImage.set("ubuntu-latest")

            stages {
                "Build" {
                    enabled.set(true)
                    jobs {
                        "Compile" {
                            step("./gradlew compileJava", "Compile Java")
                        }
                    }
                }
                "Test" {
                    enabled.set(true)
                    dependsOn.set(listOf("Build"))
                    jobs {
                        "UnitTest" {
                            step("./gradlew test", "Run Unit Tests")
                        }
                    }
                }
            }
        }
        """.trimIndent()
        )

        val result =
            GradleRunner.create().withPluginClasspath().withArguments("generatePipeline").withProjectDir(projectDir)
                .forwardOutput().build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generatePipeline")?.outcome)

        val pipelineYaml = projectDir.resolve("azure-pipelines.yml").readText()
        println("Generated multi-stage pipeline:\n$pipelineYaml")

        assertTrue(pipelineYaml.contains("Build"), "Should contain a 'Build' stage in YAML")
        assertTrue(pipelineYaml.contains("Test"), "Should contain a 'Test' stage in YAML")
        assertTrue(pipelineYaml.contains("Compile Java"), "Should have compile job steps")
        assertTrue(pipelineYaml.contains("Run Unit Tests"), "Should have test job steps")
    }
}