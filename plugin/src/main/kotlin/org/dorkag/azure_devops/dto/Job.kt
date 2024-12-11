package org.dorkag.azure_devops.dto

data class Job(
    val job: String, val displayName: String? = null, val steps: List<Step>
)
