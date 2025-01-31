package com.dorkag.azure_devops.extensions.config

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

open class StageConfig @Inject constructor(val objects: ObjectFactory) {
  val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
  val displayName: Property<String> = objects.property(String::class.java)
  val declaredFromRoot: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
  val dependsOn: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
  val condition: Property<String> = objects.property(String::class.java)
  val variables: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java).convention(emptyMap())

  val jobs: MapProperty<String, JobConfig> = objects.mapProperty(String::class.java, JobConfig::class.java)

  fun jobs(action: JobsDsl.() -> Unit) {
    val dsl = JobsDsl(objects, jobs)
    dsl.action()
  }


  open class JobsDsl(private val objects: ObjectFactory, private val jobs: MapProperty<String, JobConfig>) {
    /**
     * job("JobName") { ... }
     */
    fun job(jobName: String, configure: JobConfig.() -> Unit) {
      val jobCfg = objects.newInstance(JobConfig::class.java, objects)
      jobCfg.displayName.convention("$jobName job")
      jobCfg.configure()

      jobs.put(jobName, jobCfg)
    }
  }
}