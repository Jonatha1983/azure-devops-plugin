package com.dorkag.azure_devops.extensions.config

import com.dorkag.azure_devops.utils.NameValidator
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

open class JobConfig @Inject constructor(val objects: ObjectFactory) {
  val displayName: Property<String> = objects.property(String::class.java)
  val dependsOn: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
  val condition: Property<String> = objects.property(String::class.java)
  val continueOnError: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
  val timeoutInMinutes: Property<Int> = objects.property(Int::class.java).convention(60)
  val strategy: Property<StrategyConfig> = objects.property(StrategyConfig::class.java)
  val variables: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java).convention(emptyMap())

  /**
   * For a multistep DSL, let's store steps as a Map of stepName -> StepConfig.
   * If you prefer a list, adapt it accordingly.
   */
  val steps: MapProperty<String, StepConfig> = objects.mapProperty(String::class.java, StepConfig::class.java)

  fun steps(action: StepsDsl.() -> Unit) {
    val dsl = StepsDsl(objects, steps)
    dsl.action()
  }

  /**
   * A single-step convenience method.
   * E.g.:
   *   step("mvn cleanly compile")
   */
  fun step(scriptCommand: String, name: String? = null) {
    val stepCfg = StepConfig(objects)
    stepCfg.script.set(scriptCommand)
    if (!name.isNullOrEmpty()) {
      stepCfg.displayName.set(name)
    }
    steps.put("step${steps.get().size + 1}", stepCfg)
  }

  class StepsDsl(private val objects: ObjectFactory, private val steps: MapProperty<String, StepConfig>) {
    operator fun String.invoke(configuration: StepConfig.() -> Unit) {
      val stepName = NameValidator.validateName(this, "step")
      val stepCfg = objects.newInstance(StepConfig::class.java)
      stepCfg.configuration()
      steps.put(stepName, stepCfg)
    }
  }
}