package org.dorkag.azure_devops.dto


data class Step(
    val script: String? = null, val displayName: String? = null, val task: Task? = null
)


