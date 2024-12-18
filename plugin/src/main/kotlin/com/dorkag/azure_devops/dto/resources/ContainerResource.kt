package com.dorkag.azure_devops.dto.resources

data class ContainerResource(
    val container: String,
    val type: String = "container",
    val image: String,
    val options: String? = null,
    val ports: List<String>? = null,
    val volumes: List<String>? = null
)
