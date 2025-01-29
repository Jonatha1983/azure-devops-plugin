package com.dorkag.azure_devops.extensions

import com.dorkag.azure_devops.dto.LockBehavior
import com.dorkag.azure_devops.dto.resources.Resources
import com.dorkag.azure_devops.extensions.config.PullRequestTriggerConfig
import com.dorkag.azure_devops.extensions.config.ResourcesConfig
import com.dorkag.azure_devops.extensions.config.ScheduleConfig
import com.dorkag.azure_devops.extensions.config.StageConfig
import com.dorkag.azure_devops.extensions.pipeline.PipelineParameter
import com.dorkag.azure_devops.utils.NameValidator
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Azure Pipeline extension DSL.
 * This is the main entry point for configuring an Azure Pipeline plugin.
 *
 * @param objects The Gradle object factory.
 */
open class AzurePipelineExtension @Inject constructor(private val objects: ObjectFactory) {
  // General settings
  val name: Property<String> = objects.property(String::class.java).convention("")
  val trigger: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
  val vmImage: Property<String> = objects.property(String::class.java).convention("")
  val variables: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java).convention(emptyMap())

  // Triggers
  val pr: Property<PullRequestTriggerConfig> = objects.property(PullRequestTriggerConfig::class.java)
  private val scheduleTriggers: NamedDomainObjectContainer<ScheduleConfig> = objects.domainObjectContainer(ScheduleConfig::class.java)

  // A single unified resources DSL
  private val resourcesConfig: Property<ResourcesConfig> = objects.property(ResourcesConfig::class.java)

  // Parameters (Pipeline inputs)
  val parameters: NamedDomainObjectContainer<PipelineParameter> = objects.domainObjectContainer(
    PipelineParameter::class.java
  ) { paramName ->
    val param = objects.newInstance(PipelineParameter::class.java)
    param.name = paramName // link the container's name to the param
    param
  }

  // Nested DSL for stages
  val stages: MapProperty<String, StageConfig> = objects.mapProperty(String::class.java, StageConfig::class.java)

  fun stages(action: StagesDsl.() -> Unit) {
    val dsl = StagesDsl(objects, stages)
    dsl.action()
  }

  fun getStages(): Map<String, StageConfig> {
    return stages.get()
  }

  class StagesDsl(private val objects: ObjectFactory, private val stages: MapProperty<String, StageConfig>) {
    operator fun String.invoke(configuration: StageConfig.() -> Unit) {
      val stageName = NameValidator.validateName(this, "stage")
      val stage = objects.newInstance(StageConfig::class.java, objects)
      stage.enabled.set(true)
      stage.configuration()
      stages.put(stageName, stage)
    }
  }

  // Other pipeline-level configurations
  val lockBehavior: Property<LockBehavior> = objects.property(LockBehavior::class.java)
  val appendCommitMessageToRunName: Property<Boolean> = objects.property(Boolean::class.java)

  @Suppress("unused")
  val enableRootPipeline: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

  /**
   * Unified resources DSL. This covers repositories, containers, pipeline references, etc.
   */
  fun resources(action: Action<ResourcesConfig>) {
    val cfg = objects.newInstance(ResourcesConfig::class.java)
    action.execute(cfg)
    resourcesConfig.set(cfg)
  }

  internal fun getResources(): Resources? = resourcesConfig.orNull?.toResources()

  // DSL for PR triggers
  @Suppress("unused")
  fun pr(action: Action<PullRequestTriggerConfig>) {
    val cfg = objects.newInstance(PullRequestTriggerConfig::class.java)
    action.execute(cfg)
    pr.set(cfg)
  }

  fun triggers(action: Action<NamedDomainObjectContainer<ScheduleConfig>>) {
    action.execute(scheduleTriggers)
  }
}