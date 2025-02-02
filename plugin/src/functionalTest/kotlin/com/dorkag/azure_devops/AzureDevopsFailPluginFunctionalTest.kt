package com.dorkag.azure_devops

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@KoverAnnotation
class AzureDevopsFailPluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }

    @Test
    fun `fails when extension is not configured`() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """ rootProject.name = "azure-devops-plugin-test" """.trimIndent()
        )

        buildFile.writeText(
            """
            plugins {
                id("com.dorkag.azuredevops")
            }
            """.trimIndent()
        )

        val exception = assertThrows(UnexpectedBuildFailure::class.java) {
            GradleRunner.create()
                .withPluginClasspath()
                .withArguments("generatePipeline") // triggers pipeline generation
                .withProjectDir(projectDir)
                .build()
        }

        // The plugin should throw PipelineConfigurationException
        assertTrue(exception.message?.contains("At least one stage must be configured") == true)
    }

    @Test
    fun `validates pipeline configuration`() {
        buildFile.writeText(
            """
            plugins {
                id("com.dorkag.azuredevops")
            }

            azurePipeline {
                name.set("Test Pipeline")
                trigger.set(listOf("main"))
                vmImage.set("ubuntu-20.04")
                stages {
                    stage("Build") {
                        // Missing required jobs configuration, should fail validation
                    }
                }
            }
            """.trimIndent()
        )

        val exception = assertThrows(UnexpectedBuildFailure::class.java) {
            GradleRunner.create()
                .withPluginClasspath()
                .withArguments("validatePipeline")
                .withProjectDir(projectDir)
                .build()
        }


        assertTrue(exception.message?.contains("Stage 'Build' must contain at least one job") == true)
    }

    @Test
    fun `fails when no stages are defined`() {
        projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"azure-devops-plugin-test\"")

        buildFile.writeText(
            """
        plugins {
            id("com.dorkag.azuredevops")
        }

        azurePipeline {
            // No stages at all
            name.set("Test Pipeline")
        }
        """.trimIndent()
        )

        val exception = assertThrows(UnexpectedBuildFailure::class.java) {
            GradleRunner.create()
                .withPluginClasspath()
                .withArguments("validatePipeline")
                .withProjectDir(projectDir)
                .build()
        }

        assertTrue(
            exception.message?.contains("At least one stage must be configured in the root pipeline.") == true,
            "Expected 'At least one stage must be configured' error"
        )
    }


    @Test
    fun `fails when stage has no jobs`() {
        projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"azure-devops-plugin-test\"")

        buildFile.writeText(
            """
        plugins {
            id("com.dorkag.azuredevops")
        }

        azurePipeline {
            name.set("Test Pipeline")
            stages {
                stage("Build") {
                    // intentionally no jobs to trigger validation
                }
            }
        }
        """.trimIndent()
        )

        val exception = assertThrows(UnexpectedBuildFailure::class.java) {
            GradleRunner.create()
                .withPluginClasspath()
                .withArguments("validatePipeline")
                .withProjectDir(projectDir)
                .build()
        }

        println("Error: ${exception.message}")
        assertTrue(
            exception.message?.contains("Stage 'Build' must contain at least one job.") == true,
            "Expected error about missing jobs in stage 'Build'"
        )
    }
}