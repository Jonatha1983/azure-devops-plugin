package com.dorkag.azure_devops.dto.flow

data class Task(
    val name: String, val inputs: Map<String, Any?>? = null
)
