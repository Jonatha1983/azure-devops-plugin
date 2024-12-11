package org.dorkag.azure_devops.dto

data class Task(
    val name: String, val inputs: Map<String, Any?>? = null
)
