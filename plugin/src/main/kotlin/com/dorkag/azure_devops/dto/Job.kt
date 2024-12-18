package com.dorkag.azure_devops.dto


data class Job(
    val job: String,
    val displayName: String? = null,
    val dependsOn: List<String>? = null,
    val condition: String? = null,
    val continueOnError: Boolean? = null,
    val timeoutInMinutes: Int? = null,
    val strategy: Strategy? = null,
    val variables: Map<String, String>? = null,
    val steps: List<Step>
)

