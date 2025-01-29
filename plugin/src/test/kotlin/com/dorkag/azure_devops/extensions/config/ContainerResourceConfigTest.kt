package com.dorkag.azure_devops.extensions.config

import kotlin.test.Test
import kotlin.test.assertEquals
import org.gradle.api.model.ObjectFactory
import org.gradle.testfixtures.ProjectBuilder

class ContainerResourceConfigTest {

  @Test
  fun `test toContainerResource`() {
    val project = ProjectBuilder.builder().build()
    val factory: ObjectFactory = project.objects
    val config = ContainerResourceConfig(factory).apply {
      // Use reflection to set private fields if necessary
    }

    val containerResource = config.toContainerResource("sampleContainer")
    assertEquals("sampleContainer", containerResource.container)
    assertEquals("", containerResource.image)
  }
}