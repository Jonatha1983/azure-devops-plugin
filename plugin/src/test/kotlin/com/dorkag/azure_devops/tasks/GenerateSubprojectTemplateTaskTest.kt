package com.dorkag.azure_devops.tasks


import com.dorkag.azure_devops.extensions.AzurePipelineSubProjectExtension
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertTrue

class GenerateSubprojectTemplateTaskTest {
  @Test
  fun `test generate subproject template with stages`() {
    val project = ProjectBuilder.builder().build()
    val generateTask = project.tasks.register(
      "generateSubTemplate", GenerateSubprojectTemplateTask::class.java
    ).get()

    // Configure the extension with some test stages
    val extension = AzurePipelineSubProjectExtension(project, project.objects).apply {
      stages {
        stage("Build") {
          enabled.set(true)
          displayName.set("Build Stage")
          jobs {
            job("buildJob") {
              steps {
                step("build") {
                  script.set("./gradlew build")
                  displayName.set("Build Project")
                }
              }
            }
          }
        }
        stage("Test") {
          enabled.set(true)
          displayName.set("Test Stage")
          jobs {
            job("testJob") {
              steps {
                step("test") {
                  script.set("./gradlew test")
                  displayName.set("Run Tests")
                }
              }
            }
          }
        }
      }
    }
    generateTask.subProjectExtensionProperty.set(extension)
    generateTask.projectName.set("test-project")

    // Execute the task logic
    generateTask.generateSubTemplate()

    // Check the default output file
    val outputFile = generateTask.subprojectYaml.get().asFile
    assertTrue(outputFile.exists(), "Expected the file to be created")
    val content = outputFile.readText()
    assertTrue(content.contains("Build"), "Expected 'Build' in the YAML output")
    assertTrue(content.contains("Test"), "Expected 'Test' in the YAML output")
    assertTrue(content.contains("buildJob"), "Expected 'buildJob' in the YAML output")
    assertTrue(content.contains("testJob"), "Expected 'testJob' in the YAML output")
    assertTrue(content.contains("./gradlew build"), "Expected build command in the YAML output")
    assertTrue(content.contains("./gradlew test"), "Expected test command in the YAML output")
  }

  @Test
  fun `test generate subproject template with no stages`() {
    val project = ProjectBuilder.builder().build()
    val generateTask = project.tasks.register(
      "generateSubTemplate", GenerateSubprojectTemplateTask::class.java
    ).get()

    val extension = AzurePipelineSubProjectExtension(project,project.objects)
    generateTask.subProjectExtensionProperty.set(extension)

    generateTask.generateSubTemplate()

    val outputFile = generateTask.subprojectYaml.get().asFile
    assertTrue(outputFile.exists(), "Expected the file: $outputFile to be created if no stages")
  }
}