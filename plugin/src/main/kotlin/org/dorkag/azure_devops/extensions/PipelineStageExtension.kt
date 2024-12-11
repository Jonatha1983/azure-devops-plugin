package org.dorkag.azure_devops.extensions

import org.dorkag.azure_devops.dto.Stage
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

@AzurePipelineDsl
open class PipelineStageExtension @Inject constructor(
    private val name: String, objects: ObjectFactory
) : Named {

    override fun getName(): String = name

    var displayName: String? = null
    val jobs = objects.domainObjectContainer(JobExtension::class.java)

    fun jobs(action: Action<NamedDomainObjectContainer<JobExtension>>) {
        action.execute(jobs)
    }

    fun toStage(): Stage {
        return Stage(
            stage = name, displayName = displayName, jobs = jobs.map { it.toJob() })
    }
}
