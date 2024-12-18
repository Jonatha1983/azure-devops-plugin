package com.dorkag.azure_devops.dto.resources

import com.dorkag.azure_devops.dto.triggers.RepositoryTrigger

data class RepositoryResource(
    val repository: String,
    val type: String,
    val name: String,
    val ref: String? = null,
    val endpoint: String? = null,
    val trigger: RepositoryTrigger? = null
)
