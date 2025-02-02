package com.dorkag.azure_devops.tasks

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GenerateDslFromYamlBaseTaskTest {

  @field:TempDir
  lateinit var projectDir: File

  private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
  private val yamlFile by lazy { projectDir.resolve("azure-pipelines.yml") }

  @Test
  fun `test conversion of PowerShell module pipeline`() {
    settingsFile.writeText(
      """
            rootProject.name = "yaml-conversion-test"
        """.trimIndent()
    )

    buildFile.writeText(
      """
            plugins {
                id("com.dorkag.azuredevops")
            }
            
            tasks.register<com.dorkag.azure_devops.tasks.GenerateDslFromYamlTask>("convertPipeline") {
                inputYaml.set(file("azure-pipelines.yml"))
                outputDsl.set(layout.buildDirectory.file("converted-pipeline.gradle.kts"))
            }
        """.trimIndent()
    )

    yamlFile.writeText(
      """
            name: "PowerShell Module Pipeline"
            trigger:
              - main
            
            variables:
              buildFolderName: output
              buildArtifactName: output
            
            pool:
              vmImage: windows-latest
            
            stages:
              - stage: Build
                jobs:
                  - job: Package_Module
                    displayName: 'Package Module'
                    steps:
                      - script: |
                          dotnet tool install --global GitVersion.Tool
                        displayName: Calculate ModuleVersion
                      - task: PowerShell@2
                        name: package
                        displayName: 'Build & Package Module'
                        inputs:
                          filePath: './build.ps1'
                          arguments: '-ResolveDependency -tasks pack'
                          pwsh: true
        """.trimIndent()
    )

    val result = GradleRunner.create().withProjectDir(projectDir).withArguments("convertPipeline", "--stacktrace").withPluginClasspath().build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":convertPipeline")?.outcome)

    val generatedDsl = File(projectDir, "build/converted-pipeline.gradle.kts").readText()
    println("Generated DSL Content:\n$generatedDsl")

    assertTrue(generatedDsl.contains("variables.putAll(mapOf("))
    assertTrue(generatedDsl.contains("stage(\"Build\")"))
    assertTrue(generatedDsl.contains("job(\"Package_Module\")"))
    assertTrue(generatedDsl.contains("displayName.set(\"Package Module\")"))
    assertTrue(generatedDsl.contains("vmImage.set(\"windows-latest\")"))
    assertTrue(generatedDsl.contains("task(\"PowerShell@2\")"))
  }

  @Test
  fun `test handling of unsupported features`() {
    settingsFile.writeText("rootProject.name = \"yaml-conversion-test\"")
    buildFile.writeText(
      """
            plugins {
                id("com.dorkag.azuredevops")
            }
            
            tasks.register<com.dorkag.azure_devops.tasks.GenerateDslFromYamlTask>("convertPipeline") {
                inputYaml.set(file("azure-pipelines.yml"))
            }
        """.trimIndent()
    )

    yamlFile.writeText(
      """
            name: "Template Pipeline"
            trigger:
              - main
              
            pool:
              vmImage: ubuntu-latest
              
            resources:
              repositories:
                - repository: templates
                  type: git
                  name: MyProject/Templates
            
            stages:
              - template: templates/build.yml
        """.trimIndent()
    )

    try {
      GradleRunner.create().withProjectDir(projectDir).withPluginClasspath().withArguments("convertPipeline").forwardOutput().build()
      fail("Expected build to fail")
    } catch (e: UnexpectedBuildFailure) {
      assertNotNull(e.message)
      e.message?.let {
        assertTrue(
          it.contains("YAML contains unsupported features") || it.contains("Pipeline resources configuration")
        )
      }

    }
  }
}