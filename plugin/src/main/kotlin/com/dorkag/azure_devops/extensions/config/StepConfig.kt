package com.dorkag.azure_devops.extensions.config

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

open class StepConfig @Inject constructor(objects: ObjectFactory) {
  // -- Distinguish script vs. task
  val script: Property<String> = objects.property(String::class.java)
  val taskName: Property<String> = objects.property(String::class.java)

  // Display name
  val displayName: Property<String> = objects.property(String::class.java)

  // Inputs (for "task" steps)
  val inputs: MapProperty<String, Any?> = objects.mapProperty(String::class.java, Any::class.java)

  // Additional common properties:
  val condition: Property<String> = objects.property(String::class.java)            // e.g. 'always()'
  val continueOnError: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
  val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
  val env: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)
  val name: Property<String> = objects.property(String::class.java)                 // step ID
  val timeoutInMinutes: Property<Int> = objects.property(Int::class.java).convention(0)
  val retryCountOnTaskFailure: Property<Int> = objects.property(Int::class.java).convention(0)
  val target: Property<String> = objects.property(String::class.java)               // or if you want an object, define another config

  /**
   * DSL convenience for a "task" step, e.g.:
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
   * DSL convenience for a "script" step
   */
  fun script(scriptCommand: String, displayName: String? = null) {
    script.set(scriptCommand)
    displayName?.let { this.displayName.set(it) }
  }
}