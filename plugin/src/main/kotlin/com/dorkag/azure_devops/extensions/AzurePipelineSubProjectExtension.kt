package com.dorkag.azure_devops.extensions

import com.dorkag.azure_devops.extensions.config.JobConfig
import com.dorkag.azure_devops.extensions.config.StageConfig
import com.dorkag.azure_devops.extensions.config.StepConfig
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
  private val stagesContainer: MapProperty<String, StageConfig> = objects.mapProperty(String::class.java, StageConfig::class.java)

  // Store these so we don't call subProject at execution time
  private val rootHasPlugin: Property<Boolean> = objects.property(Boolean::class.java)
  private val rootExtension: Property<AzurePipelineExtension?> = objects.property(AzurePipelineExtension::class.java)

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


    @Suppress("MemberVisibilityCanBePrivate")
    fun declaredStage(stageName: String) {
      if (rootHasPlugin.get()) {
        val rootExt = rootExtension.orNull ?: error("Root extension missing but rootHasPlugin == true??")

        // 1) Get the root stage config
        val rootStage = rootExt.getStages()[stageName] ?: error("Subproject tries to declareStage('$stageName'), but root does not define stage '$stageName'.")

        // 2) Create a local copy
        val cloned = copyStageConfig(rootStage, objects) // If you want subproject displayName to differ, you can do:
        cloned.displayName.set(stageName)
        cloned.declaredFromRoot.set(true)

        stages.put(stageName, cloned)
      } else { // Root does not apply plugin => we let subproject do an empty reference or fail
        error("You said declareStage('$stageName') but the root plugin is not applied.")
      }
    }

    /**
     * For referencing multiple root stages at once, e.g., declaredStages("Build","Test")
     */
    @Suppress("unused")
    fun declaredStages(vararg stageNames: String) {
      stageNames.forEach { declaredStage(it) }
    }
  }
}

/**
 * These functions are top-level and private to this file.
 * They deep-copy the entire stage → job → step structure from the root.
 */
private fun copyStageConfig(src: StageConfig, objects: ObjectFactory): StageConfig {
  val dst = objects.newInstance(StageConfig::class.java, objects)
  dst.enabled.set(src.enabled.get())
  dst.displayName.set(src.displayName.orNull)
  dst.dependsOn.set(src.dependsOn.get())
  dst.condition.set(src.condition.orNull)
  dst.variables.putAll(src.variables.get())

  // Copy jobs
  src.jobs.get().forEach { (jobName, jobCfg) ->
    dst.jobs {
      job(jobName) {
        copyJobConfig(jobCfg, this)
      }
    }
  }

  return dst
}

private fun copyJobConfig(src: JobConfig, dst: JobConfig) {
  dst.displayName.set(src.displayName.orNull)
  dst.dependsOn.set(src.dependsOn.get())
  dst.condition.set(src.condition.orNull)
  dst.continueOnError.set(src.continueOnError.get())
  dst.timeoutInMinutes.set(src.timeoutInMinutes.get())
  dst.variables.putAll(src.variables.get())

  // Copy steps
  src.steps.get().forEach { (stepName, stepCfg) ->
    dst.steps {
      step(stepName) {
        copyStepConfig(stepCfg, this)
      }
    }
  }
}

private fun copyStepConfig(src: StepConfig, dst: StepConfig) {
  dst.script.set(src.script.orNull)
  dst.taskName.set(src.taskName.orNull)
  dst.displayName.set(src.displayName.orNull)
  dst.inputs.putAll(src.inputs.get())
  dst.condition.set(src.condition.orNull)
  dst.continueOnError.set(src.continueOnError.get())
  dst.enabled.set(src.enabled.get())
  dst.env.putAll(src.env.get())
  dst.name.set(src.name.orNull)
  dst.timeoutInMinutes.set(src.timeoutInMinutes.get())
  dst.retryCountOnTaskFailure.set(src.retryCountOnTaskFailure.get())
  dst.target.set(src.target.orNull)
}