package org.dorkag.azure_devops.extensions

import org.dorkag.azure_devops.dto.Job
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

@AzurePipelineDsl
open class JobExtension @Inject constructor(
    private val name: String, objects: ObjectFactory
) : Named {

    override fun getName(): String = name

    var displayName: String? = null
    val steps = objects.domainObjectContainer(StepExtension::class.java)

    fun steps(action: Action<NamedDomainObjectContainer<StepExtension>>) {
        action.execute(steps)
    }

    fun toJob(): Job {
        return Job(
            job = name, displayName = displayName, steps = steps.map { it.toStep() })
    }
}