package com.dorkag.azure_devops.dto.flow

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TaskTest {

  @Test
  fun `test Task default values`() {
    val task = Task("build")

    assertEquals("build", task.name)
    assertNull(task.inputs)
  }

  @Test
  fun `test Task properties`() {
    val task = Task(
      name = "testTask",
      inputs = mapOf("input1" to "value1", "input2" to 42)
    )

    assertEquals("testTask", task.name)
    assertEquals(mapOf("input1" to "value1", "input2" to 42), task.inputs)
  }
}