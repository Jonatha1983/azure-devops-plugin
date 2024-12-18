package com.dorkag.azure_devops.dto.triggers

data class PipelineTrigger(
    val branches: List<String>? = null,
    val tags: List<String>? = null,
    val stages: List<String>? = null
)
