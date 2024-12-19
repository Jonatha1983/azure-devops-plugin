package com.dorkag.azure_devops.dto.flow


data class Step(
    val script: String? = null,
    val displayName: String? = null,
    val task: Task? = null,
)
