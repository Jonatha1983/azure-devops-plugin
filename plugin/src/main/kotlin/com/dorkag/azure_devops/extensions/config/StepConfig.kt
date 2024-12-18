package com.dorkag.azure_devops.extensions.config

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

open class StepConfig @Inject constructor(objects: ObjectFactory) {
    // If it's a script-based step
    val script: Property<String> = objects.property(String::class.java)
    val displayName: Property<String> = objects.property(String::class.java)

    // If it's a "task", e.g. Docker@2, Gradle@3, etc.
    val taskName: Property<String> = objects.property(String::class.java)
    val inputs: MapProperty<String, Any?> = objects.mapProperty(String::class.java, Any::class.java)

    /**
     * DSL convenience method for a "task" step, e.g.:
     *
     *   task("Gradle@3") {
     *       displayName.set("Run Gradle Build")
     *       inputs.put("tasks", "clean build")
     *       ...
     *   }
     */
    fun task(taskId: String, configure: StepConfig.() -> Unit = {}) {
        taskName.set(taskId)
        this.configure()
    }

    /**
     * DSL convenience method for a "script" step, if needed
     */
    fun script(scriptCommand: String, displayName: String? = null) {
        script.set(scriptCommand)
        displayName?.let { this.displayName.set(it) }
    }
}