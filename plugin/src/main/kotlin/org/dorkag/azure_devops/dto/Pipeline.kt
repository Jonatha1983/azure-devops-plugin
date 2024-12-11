package org.dorkag.azure_devops.dto


data class Pipeline(
    val name: String,
    val trigger: Trigger,
    val pool: Pool,
    val parameters: Map<String, String>? = null,
    val variables: Map<String, String>? = null,
    val stages: List<Stage>
)





