package com.dorkag.azure_devops.extensions

import com.dorkag.azure_devops.extensions.config.StageConfig
import com.dorkag.azure_devops.utils.NameValidator
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

open class AzurePipelineSubProjectExtension @Inject constructor(val objects: ObjectFactory) {
  val name: Property<String> = objects.property(String::class.java)
  val trigger: ListProperty<String> = objects.listProperty(String::class.java)
  val vmImage: Property<String> = objects.property(String::class.java)

  // For backward compatibility - public list property
  val stages: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())

  // New DSL support - internal map property
  internal val stagesContainer: MapProperty<String, StageConfig> = objects.mapProperty(String::class.java, StageConfig::class.java)

  init {
    trigger.convention(listOf("main"))
    vmImage.convention("ubuntu-latest")
  }

  fun stages(action: StagesDsl.() -> Unit) {
    val dsl = StagesDsl(stagesContainer, objects)
    dsl.action()
  }

  internal fun getStages(): Map<String, StageConfig> { // If using the old list-based API, convert to StageConfig
    if (stages.get().isNotEmpty()) {
      val stageMap = mutableMapOf<String, StageConfig>()
      stages.get().forEach { stageName ->
        val validatedName = NameValidator.validateName(stageName, "stage")
        val stageConfig = objects.newInstance(StageConfig::class.java, objects)
        stageConfig.enabled.set(true)
        stageConfig.displayName.set("Stage: $validatedName") // Add a default job and step with validated names
        stageConfig.jobs {
          "${validatedName}_job" {  // Using underscore instead of hyphen
            displayName.set("$validatedName job")
            steps {
              "${validatedName}_step" {  // Using underscore instead of hyphen
                script.set("echo 'Executing $validatedName'")
                displayName.set("Run $validatedName")
              }
            }
          }
        }
        stageMap[validatedName] = stageConfig
      }
      return stageMap
    } // Otherwise use the new DSL-based configuration
    return stagesContainer.get()
  }

  class StagesDsl(private val stages: MapProperty<String, StageConfig>, private val objects: ObjectFactory) {
    operator fun String.invoke(configuration: StageConfig.() -> Unit) {
      val stage = objects.newInstance(StageConfig::class.java, objects)
      stage.enabled.set(true)
      stage.configuration()
      stages.put(this, stage)
    }
  }
}