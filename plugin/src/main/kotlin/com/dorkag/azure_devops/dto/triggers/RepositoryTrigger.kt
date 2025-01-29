package com.dorkag.azure_devops.dto.triggers

data class RepositoryTrigger(val branches: List<String>? = null, val tags: List<String>? = null, val paths: List<String>? = null)
