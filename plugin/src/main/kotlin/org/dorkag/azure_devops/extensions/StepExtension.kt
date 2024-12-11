package org.dorkag.azure_devops.extensions

import org.dorkag.azure_devops.dto.Step
import org.gradle.api.Named
import javax.inject.Inject


@AzurePipelineDsl
open class StepExtension @Inject constructor(
    private val name: String
) : Named {

    override fun getName(): String = name

    var script: String? = null
    var displayName: String? = null

    fun toStep(): Step {
        return Step(
            script = script,
            displayName = displayName
        )
    }
}