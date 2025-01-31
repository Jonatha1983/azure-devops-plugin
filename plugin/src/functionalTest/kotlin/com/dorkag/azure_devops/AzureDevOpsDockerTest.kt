package com.dorkag.azure_devops

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@KoverAnnotation
class AzureDevOpsDockerTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

    private val expected = getResourceAsText("/com/dorkag/azure_devops/docker-azure-pipelines.yml")


    @Test
    fun `generates pipeline matching Docker snippet`() {
        settingsFile.writeText("rootProject.name = \"azure-devops-plugin-test\"")

      buildFile.writeText(
        """
        plugins {
            id("com.dorkag.azuredevops")
        }
      
        azurePipeline {
            // This DSL produces a single stage "BuildDocker" with nested jobs
            name.set("DockerBuildPipeline")
            trigger.set(listOf("main", "develop"))
            pr {
                branches.set(listOf("main", "develop"))
            }
            vmImage.set("ubuntu-latest")
      
            stages {
                stage("BuildDocker") {
                    enabled.set(true)
                    displayName.set("Build Docker image")
                    jobs {
                        job("GradleBuild") {
                            displayName.set("Gradlew build")
                            steps {
                                step("gradleStep") {
                                    script.set("./gradlew bootJar :test --info --build-cache")
                                    displayName.set("Gradlew build")
                                }
                            }
                        }
      
                        job("DockerLogin") {
                            displayName.set("Login to Azure registry")
                            steps {
                                step("dockerLogin") {
                                    script.set("docker login -u ... -p ... myregistry.azurecr.io")
                                    displayName.set("Login to Azure registry")
                                }
                            }
                        }
      
                        job("DockerBuildPush") {
                            displayName.set("Build and push image to container registry")
                            steps {
                                step("dockerBuildPush") {
                                    script.set("docker build . -t myregistry.azurecr.io/repository:latest && docker push myregistry.azurecr.io/repository:latest")
                                    displayName.set("Build and push image")
                                }
                            }
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

        val generatedYaml = projectDir.resolve("azure-pipelines.yml").readText()
        // Strip metadata comments from generated YAML before comparison
        val strippedGeneratedYaml = stripMetadataComments(generatedYaml).trim()
        val expectedYaml = expected.trim()

        println("=== Generated YAML (without metadata) ===\n$strippedGeneratedYaml")
        println("=== Expected YAML ===\n$expectedYaml")

        assertEquals(expectedYaml, strippedGeneratedYaml,
            "Generated YAML content (excluding metadata) does not match expected content.")

        // Additional substring checks (these should still work since the content is still there)
        assertTrue(generatedYaml.contains("DockerBuildPipeline"), "Pipeline name not found")
        assertTrue(generatedYaml.contains("vmImage: ubuntu-latest"), "vmImage not found")
        assertTrue(
            generatedYaml.contains("docker login"), "Docker login step expected"
        )
    }
}