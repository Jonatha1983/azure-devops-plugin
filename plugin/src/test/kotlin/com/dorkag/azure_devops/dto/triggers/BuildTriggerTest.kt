package com.dorkag.azure_devops.dto.triggers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


class BuildTriggerTest {

  @Test
  fun `test BuildTrigger default values`() {
    val trigger = BuildTrigger()
    assertNull(trigger.branches)
    assertNull(trigger.tags)
    assertNull(trigger.stages)
  }

  @Test
  fun `test BuildTrigger with values`() {
    val branches = listOf("hotfix")
    val tags = listOf("urgent")
    val stages = listOf("deploy")
    val trigger = BuildTrigger(branches = branches, tags = tags, stages = stages)

    assertEquals(branches, trigger.branches)
    assertEquals(tags, trigger.tags)
    assertEquals(stages, trigger.stages)
  }

  @Test
  fun `test BuildTrigger equality`() {
    val trigger1 = BuildTrigger(branches = listOf("hotfix"), tags = listOf("urgent"), stages = listOf("deploy"))
    val trigger2 = BuildTrigger(branches = listOf("hotfix"), tags = listOf("urgent"), stages = listOf("deploy"))

    assertEquals(trigger1, trigger2)
  }
}
