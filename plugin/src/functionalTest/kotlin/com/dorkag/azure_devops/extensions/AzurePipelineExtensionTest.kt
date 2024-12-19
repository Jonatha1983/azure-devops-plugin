package com.dorkag.azure_devops.extensions

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AzurePipelineExtensionTest {

    @Test
    fun `test default extension values`() {
        val project = ProjectBuilder.builder().build()
        val extension = project.extensions.create(
            "azurePipeline", AzurePipelineExtension::class.java, project.objects
        )

        // Check default values
        assertEquals("", extension.name.get())
        assertTrue(extension.trigger.get().isEmpty())
        assertEquals("", extension.vmImage.get())
        assertTrue(extension.variables.get().isEmpty())
    }

    @Test
    fun `test setting simple properties`() {
        val project = ProjectBuilder.builder().build()
        val extension = project.extensions.create(
            "azurePipeline", AzurePipelineExtension::class.java, project.objects
        )

        extension.name.set("MyPipeline")
        extension.vmImage.set("ubuntu-latest")
        extension.variables.put("ENV", "prod")

        assertEquals("MyPipeline", extension.name.get())
        assertEquals("ubuntu-latest", extension.vmImage.get())
        assertEquals(mapOf("ENV" to "prod"), extension.variables.get())
    }

}