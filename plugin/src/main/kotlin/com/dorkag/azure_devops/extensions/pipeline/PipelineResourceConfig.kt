package com.dorkag.azure_devops.extensions.pipeline

import com.dorkag.azure_devops.dto.resources.PipelineResource
import org.gradle.api.Action
import javax.inject.Inject

open class PipelineResourceConfig @Inject constructor(
    val name: String
) {
    private var source: String = ""
    private var version: String? = null
    private var branch: String? = null
    private var triggerConfig: PipelineTriggerConfig? = null

    fun trigger(action: Action<PipelineTriggerConfig>) {
        triggerConfig = PipelineTriggerConfig()
        action.execute(triggerConfig!!)
    }

    internal fun toPipelineResource() = PipelineResource(
        pipeline = name,
        source = source,
        version = version,
        branch = branch,
        trigger = triggerConfig?.toPipelineTrigger()
    )
}