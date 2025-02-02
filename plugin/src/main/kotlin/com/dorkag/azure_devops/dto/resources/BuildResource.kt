package com.dorkag.azure_devops.dto.resources

import com.dorkag.azure_devops.dto.triggers.BuildTrigger

data class BuildResource(val source: String, val type: String, val version: String? = null, val branch: String? = null, val trigger: BuildTrigger? = null)