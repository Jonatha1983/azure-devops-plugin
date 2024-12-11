package org.dorkag.azure_devops

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AzureDevopsFailPluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }

    @Test
    fun `fails when extension is not configured`() {
        // Add settings file
        projectDir.resolve("settings.gradle.kts").writeText(
            """
        rootProject.name = "azure-devops-plugin-test"
    """.trimIndent()
        )

        buildFile.writeText(
            """
        plugins {
            id("org.dorkag.azuredevops")
        }
    """.trimIndent()
        )

        val exception = assertThrows(UnexpectedBuildFailure::class.java) {
            GradleRunner.create().withPluginClasspath().withArguments("generatePipeline").withProjectDir(projectDir)
                .build()
        }

        assertTrue(exception.message?.contains("At least one stage must be configured") == true)
    }

    @Test
    fun `validates pipeline configuration`() {
        buildFile.writeText(
            """
        plugins {
            id("org.dorkag.azuredevops")
        }

        azurePipeline {
            name = "Test Pipeline"
            triggerBranches = listOf("main")
            vmImage = "ubuntu-20.04"
            stages {
                create("Build") {
                    // Missing required jobs configuration
                }
            }
        }
    """.trimIndent()
        )

        val exception = assertThrows(UnexpectedBuildFailure::class.java) {
            GradleRunner.create().withPluginClasspath().withArguments("validatePipeline").withProjectDir(projectDir)
                .build()
        }

        assertTrue(exception.message?.contains("Stage 'Build' must contain at least one job") == true)
    }
}