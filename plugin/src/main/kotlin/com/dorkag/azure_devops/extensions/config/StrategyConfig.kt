package com.dorkag.azure_devops.extensions.config

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

@Suppress("UNCHECKED_CAST", "unused")
open class StrategyConfig @Inject constructor(objects: ObjectFactory) {
    val type: Property<String> = objects.property(String::class.java)
    val maxParallel: Property<Int> = objects.property(Int::class.java)
    val matrix: MapProperty<String, Map<String, String>> =
        objects.mapProperty(String::class.java, Map::class.java as Class<Map<String, String>>)
}