package com.dorkag.azure_devops.dto


data class Parameter(
    val name: String, val displayName: String, val type: ParameterType, val default: String?, val values: List<String>?
)
