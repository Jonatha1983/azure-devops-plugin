package com.dorkag.azure_devops.dto.resources

import com.dorkag.azure_devops.dto.triggers.PipelineTrigger

data class PipelineResource(
    val pipeline: String,
    val source: String,
    val version: String? = null,
    val branch: String? = null,
    val trigger: PipelineTrigger? = null
)
