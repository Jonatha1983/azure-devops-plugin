package com.dorkag.azure_devops.extensions.config

import com.dorkag.azure_devops.utils.NameValidator
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

open class StageConfig @Inject constructor(val objects: ObjectFactory) {
  val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
  val displayName: Property<String> = objects.property(String::class.java)
  val dependsOn: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
  val condition: Property<String> = objects.property(String::class.java)
  val variables: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java).convention(emptyMap())

  val jobs: MapProperty<String, JobConfig> = objects.mapProperty(String::class.java, JobConfig::class.java)

  fun jobs(action: JobsDsl.() -> Unit) {
    val dsl = JobsDsl(jobs, objects)
    dsl.action()
  }

  class JobsDsl(private val jobs: MapProperty<String, JobConfig>, private val objects: ObjectFactory) {
    operator fun String.invoke(configuration: JobConfig.() -> Unit) {
      val jobName = NameValidator.validateName(this, "job")
      val job = objects.newInstance(JobConfig::class.java)
      job.configuration()
      jobs.put(jobName, job)
    }
  }
}