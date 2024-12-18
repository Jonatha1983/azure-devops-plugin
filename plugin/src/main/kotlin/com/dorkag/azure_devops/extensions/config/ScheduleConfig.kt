package com.dorkag.azure_devops.extensions.config

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

@Suppress("unused")
open class ScheduleConfig @Inject constructor(objects: ObjectFactory) {
    val cron: Property<String> = objects.property(String::class.java) // e.g., "0 0 * * *"
    val branches: Property<String> = objects.property(String::class.java)
    val always: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
}