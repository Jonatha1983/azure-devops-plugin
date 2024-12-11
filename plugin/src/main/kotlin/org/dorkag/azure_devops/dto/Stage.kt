package org.dorkag.azure_devops.dto


data class Stage(
    val stage: String, val displayName: String? = null, val jobs: List<Job>
)
