package com.dorkag.azure_devops.dto


data class Branches(
    val include: List<String> = emptyList(), val exclude: List<String> = emptyList()
)