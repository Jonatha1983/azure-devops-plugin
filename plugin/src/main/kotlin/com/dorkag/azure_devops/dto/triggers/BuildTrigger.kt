package com.dorkag.azure_devops.dto.triggers

data class BuildTrigger(
    val branches: List<String>? = null,
    val tags: List<String>? = null,
    val stages: List<String>? = null
)
