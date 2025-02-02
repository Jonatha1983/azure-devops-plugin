package com.dorkag.azure_devops.tasks

import com.dorkag.azure_devops.KoverAnnotation
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertContains

@KoverAnnotation
class GenerateDslFromYamlTaskTest {

  @field:TempDir
  lateinit var projectDir: File

  private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
  private val yamlFile by lazy { projectDir.resolve("pipeline.yaml") }

  @BeforeTest
  fun setup() { // Sample minimal pipeline.yaml input (unchanged)
    yamlFile.writeText(
      """
            name: "Test Pipeline"
            trigger:
              - main
              - develop
            pr:
              - feature/*
            pool:
              vmImage: "ubuntu-latest"
            variables:
              ENV: "test"
            stages:
              - stage: Build
                displayName: "Build Stage"
                jobs:
                  - job: build-job
                    steps:
                      - script: "./gradlew build"
                        displayName: "Run Build"
            """.trimIndent()
    )

    buildFile.writeText(
      """
            plugins {
                id("com.dorkag.azuredevops")
            }

            tasks { 
                convertYamlToDsl.configure {
                    inputYaml.set(file("pipeline.yaml"))
                }
            }
            """.trimIndent()
    )
  }

  @Test
  fun `test generateDsl task`() {
    val result = GradleRunner.create().withProjectDir(projectDir).withArguments("convertYamlToDsl").withPluginClasspath().build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":convertYamlToDsl")?.outcome)

    val generatedDsl = projectDir.toPath().resolve("build").resolve("generated-dsl.gradle.kts").toFile()
    assertTrue(generatedDsl.exists(), "The generated DSL file should exist")

    val dslContent = generatedDsl.readText()
    println("Generated DSL Content:\n$dslContent") // Debug output

    // Updated assertions to match current implementation
    assertContains(dslContent, "azurePipeline {")
    assertContains(dslContent, "name.set(\"Test Pipeline\")")
    assertContains(dslContent, "trigger.set(listOf(\"main\", \"develop\"))")
    assertContains(dslContent, "pr {")
    assertContains(dslContent, "branches.set(listOf(\"feature/*\"))")
    assertContains(dslContent, "vmImage.set(\"ubuntu-latest\")")
    assertContains(dslContent, "variables.putAll(mapOf(")
    assertContains(dslContent, "\"ENV\" to \"test\"")
    assertContains(dslContent, "stages {")
    assertContains(dslContent, "stage(\"Build\") {")
    assertContains(dslContent, "displayName.set(\"Build Stage\")")
    assertContains(dslContent, "job(\"build-job\") {")
    assertContains(dslContent, "steps {")
    assertContains(dslContent, "step(\"RunBuild\") {")
    assertContains(dslContent, "script.set(\"\"\"./gradlew build\"\"\")")
  }
}

