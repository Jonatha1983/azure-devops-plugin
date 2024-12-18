package com.dorkag.azure_devops.dto

import com.dorkag.azure_devops.extensions.config.JobConfig

data class Strategy(
    val type: String? = null,
    val maxParallel: Int? = null,
    val matrix: Map<String, Map<String, String>>? = null,
    val parallel: Map<String, JobConfig>? = null
)
