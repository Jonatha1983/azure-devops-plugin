package com.dorkag.azure_devops.dto.triggers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RepositoryTriggerTest {

  @Test
  fun `test RepositoryTrigger default values`() {
    val trigger = RepositoryTrigger()
    assertNull(trigger.branches)
    assertNull(trigger.tags)
    assertNull(trigger.paths)
  }

  @Test
  fun `test RepositoryTrigger with values`() {
    val branches = listOf("feature")
    val tags = listOf("release")
    val paths = listOf("/src", "/test")
    val trigger = RepositoryTrigger(branches = branches, tags = tags, paths = paths)

    assertEquals(branches, trigger.branches)
    assertEquals(tags, trigger.tags)
    assertEquals(paths, trigger.paths)
  }

  @Test
  fun `test RepositoryTrigger equality`() {
    val trigger1 = RepositoryTrigger(branches = listOf("feature"), tags = listOf("release"), paths = listOf("/src"))
    val trigger2 = RepositoryTrigger(branches = listOf("feature"), tags = listOf("release"), paths = listOf("/src"))

    assertEquals(trigger1, trigger2)
  }
}
