package com.dorkag.azure_devops.extensions

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import javax.inject.Inject

open class AzurePipelineSubProjectExtension @Inject constructor(
    objects: ObjectFactory
) {
    /**
     * A simple ListProperty to hold user-defined stage names for the subproject.
     */
    val stages: ListProperty<String> = objects.listProperty(String::class.java)

    init {
        // Default can be empty or pre-populated if desired.
        stages.convention(emptyList())
    }
}