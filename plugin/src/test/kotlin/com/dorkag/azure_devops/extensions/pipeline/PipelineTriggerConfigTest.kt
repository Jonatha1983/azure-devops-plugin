package com.dorkag.azure_devops.extensions.pipeline

import kotlin.test.Test
import kotlin.test.assertEquals

class PipelineTriggerConfigTest {

  @Test
  fun `test toPipelineTrigger with default values`() {
    val pipelineTriggerConfig = PipelineTriggerConfig()

    val trigger = pipelineTriggerConfig.toPipelineTrigger()

    assertEquals(null, trigger.branches)
    assertEquals(null, trigger.tags)
    assertEquals(null, trigger.stages)
  }

  @Test
  fun `test toPipelineTrigger with custom values`() {
    val pipelineTriggerConfig = PipelineTriggerConfig().apply {
      branches = listOf("main", "feature")
      stages = listOf("build", "deploy")
    }

    val trigger = pipelineTriggerConfig.toPipelineTrigger()

    assertEquals(listOf("main", "feature"), trigger.branches)
    assertEquals(null, trigger.tags) // Tags aren't set
    assertEquals(listOf("build", "deploy"), trigger.stages)
  }
}