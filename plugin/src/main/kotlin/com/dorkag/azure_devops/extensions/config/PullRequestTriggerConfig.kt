package com.dorkag.azure_devops.extensions.config

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import javax.inject.Inject

@Suppress("unused")
open class PullRequestTriggerConfig @Inject constructor(objects: ObjectFactory) {
    val branches: ListProperty<String> = objects.listProperty(String::class.java)
    val includeDrafts: ListProperty<Boolean> = objects.listProperty(Boolean::class.java)
}