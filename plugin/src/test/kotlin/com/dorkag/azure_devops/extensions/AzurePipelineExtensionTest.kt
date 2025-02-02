package com.dorkag.azure_devops.extensions

import kotlin.test.Test
import kotlin.test.assertEquals
import org.gradle.testfixtures.ProjectBuilder

class AzurePipelineExtensionTest {

  @Test
  fun `test AzurePipelineExtension properties`() {
    val project = ProjectBuilder.builder().build()
    val extension = AzurePipelineExtension(project.objects)

    extension.name.set("Test Pipeline")
    extension.trigger.set(listOf("main", "develop"))

    assertEquals("Test Pipeline", extension.name.get())
    assertEquals(listOf("main", "develop"), extension.trigger.get())
  }

  @Test
  fun `test stages DSL`() {
    val project = ProjectBuilder.builder().build()
    val extension = AzurePipelineExtension(project.objects)

    extension.stages {
      stage("build") { // Custom stage configuration
      }
    }

    assertEquals(1, extension.stages.get().size)
    assert(extension.stages.get().containsKey("build"))
  }
}