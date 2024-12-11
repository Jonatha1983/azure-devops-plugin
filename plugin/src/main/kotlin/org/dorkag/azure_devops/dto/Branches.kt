package org.dorkag.azure_devops.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class Branches(@JsonProperty("include") val include: List<String>)
