package com.dorkag.azure_devops.extensions

import com.dorkag.azure_devops.extensions.config.StageConfig
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

open class AzurePipelineSubProjectExtension @Inject constructor(subProject: Project, val objects: ObjectFactory) {
  val name: Property<String> = objects.property(String::class.java)
  val trigger: ListProperty<String> = objects.listProperty(String::class.java)
  val vmImage: Property<String> = objects.property(String::class.java)


  // New DSL support - internal map property
  internal val stagesContainer: MapProperty<String, StageConfig> = objects.mapProperty(String::class.java, StageConfig::class.java)

  // Store these so we don't call subProject at execution time
  val rootHasPlugin: Property<Boolean> = objects.property(Boolean::class.java)
  val rootExtension: Property<AzurePipelineExtension?> = objects.property(AzurePipelineExtension::class.java)

  init {
    trigger.convention(listOf("main"))
    vmImage.convention("ubuntu-latest")

    // Check if root applies
    val root = subProject.rootProject
    val hasRootPlugin = root.plugins.hasPlugin("com.dorkag.azuredevops")
    rootHasPlugin.set(hasRootPlugin)
    if (hasRootPlugin) {
      val rootExt = root.extensions.findByType(AzurePipelineExtension::class.java)
      rootExtension.set(rootExt) // might be null if something is off
    }
  }


  fun stages(action: SubStagesDsl.() -> Unit) {
    val dsl = SubStagesDsl(stagesContainer, objects, rootHasPlugin, rootExtension)
    dsl.action()
  }

  /**
   * The subproject "stages" aggregator
   */
  internal fun getStages(): Map<String, StageConfig> {
    return stagesContainer.get()
  }

  open class SubStagesDsl(private val stages: MapProperty<String, StageConfig>,
                          private val objects: ObjectFactory,
                          private val rootHasPlugin: Property<Boolean>,
                          private val rootExtension: Property<AzurePipelineExtension?>) {
    /**
     * For brand-new stage definitions local to the subproject:
     */
    fun stage(stageName: String, configure: StageConfig.() -> Unit) {
      val stageCfg = objects.newInstance(StageConfig::class.java, objects)
      stageCfg.enabled.set(true)
      stageCfg.configure()
      stages.put(stageName, stageCfg)
    }


    fun declaredStage(stageName: String) {
      if (rootHasPlugin.get()) {
        val re = rootExtension.orNull ?: error("Root extension missing but rootHasPlugin == true??")
        val rootStageNames = re.getStages().keys
        if (!rootStageNames.contains(stageName)) {
          error("Subproject tries to declareStage('$stageName') which does not exist in root.")
        }
      }
      val stageCfg = objects.newInstance(StageConfig::class.java, objects)
      stageCfg.enabled.set(true)
      stageCfg.displayName.set("Stage: $stageName (from root)")

      // Mark that it references an existing root stage => subproject local jobs are optional
      stageCfg.declaredFromRoot.set(true)

      stages.put(stageName, stageCfg)
    }

    /**
     * For referencing multiple root stages at once, e.g. declaredStages("Build","Test")
     */
    @Suppress("unused")
    fun declaredStages(vararg stageNames: String) {
      stageNames.forEach { declaredStage(it) }
    }
  }
}