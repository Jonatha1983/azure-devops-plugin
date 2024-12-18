package com.dorkag.azure_devops.extensions.config

import com.dorkag.azure_devops.dto.resources.ContainerResource
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class ContainerResourceConfig @Inject constructor(
    // Removing the "val name: String" constructor param
    val objects: ObjectFactory
) {
    var image: String = ""
    var options: String? = null
    var ports: List<String>? = null
    var volumes: List<String>? = null

    /**
     * Accepts containerName from the DSL map key
     */
    internal fun toContainerResource(containerName: String): ContainerResource {
        return ContainerResource(
            container = containerName, image = image, options = options, ports = ports, volumes = volumes
        )
    }
}