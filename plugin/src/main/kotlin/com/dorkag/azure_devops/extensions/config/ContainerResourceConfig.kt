package com.dorkag.azure_devops.extensions.config

import com.dorkag.azure_devops.dto.resources.ContainerResource
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * Configuration for a container resource.
 */
open class ContainerResourceConfig @Inject constructor(val objects: ObjectFactory) {
    private var image: String = ""
    private var options: String? = null
    private var ports: List<String>? = null
    private var volumes: List<String>? = null

    /**
     * Accepts containerName from the DSL map key
     */
    internal fun toContainerResource(containerName: String): ContainerResource {
        return ContainerResource(
            container = containerName, image = image, options = options, ports = ports, volumes = volumes
        )
    }
}