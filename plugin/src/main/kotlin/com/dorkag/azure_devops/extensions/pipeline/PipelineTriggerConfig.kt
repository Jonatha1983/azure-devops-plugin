package com.dorkag.azure_devops.extensions.pipeline

import com.dorkag.azure_devops.dto.triggers.PipelineTrigger
import javax.inject.Inject

open class PipelineTriggerConfig @Inject constructor() {
    var branches: List<String>? = null
    var tags: List<String>? = null
    var stages: List<String>? = null

    internal fun toPipelineTrigger() = PipelineTrigger(
        branches = branches, tags = tags, stages = stages
    )
}