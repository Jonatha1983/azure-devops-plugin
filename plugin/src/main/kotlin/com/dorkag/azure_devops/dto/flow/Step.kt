package com.dorkag.azure_devops.dto.flow


data class Step(val script: String? = null,
                val task: String? = null,
                val displayName: String? = null,
                val inputs: Map<String, Any?>? = null,
                val condition: String? = null,
                val continueOnError: Boolean? = null,
                val enabled: Boolean? = null,
                val env: Map<String, String>? = null,
                val name: String? = null,
                val timeoutInMinutes: Int? = null,
                val retryCountOnTaskFailure: Int? = null,
                val target: Any? = null)