package com.dorkag.azure_devops.dto.flow



data class Stage(
    val stage: String,
    val displayName: String? = null,
    val dependsOn: List<String>? = null,
    val condition: String? = null,
    val variables: Map<String, String>? = null,
    val template: String? = null,
    val parameters: Map<String, Any?>? = null,
    val jobs: List<Job>? = null  // <---- Add this
)
