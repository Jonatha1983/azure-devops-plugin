package com.dorkag.azure_devops.dto

data class Schedule(
    val cron: String,
    val branches: Branches,
    val displayName: String? = null,
    val always: Boolean = false
)
