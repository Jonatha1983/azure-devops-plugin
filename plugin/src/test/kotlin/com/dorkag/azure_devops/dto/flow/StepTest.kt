package com.dorkag.azure_devops.dto.flow


import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StepTest {

  @Test
  fun `test Step default values`() {
    val step = Step()

    assertNull(step.script)
    assertNull(step.displayName)
    assertNull(step.task)
  }

  @Test
  fun `test Step properties`() {
    val task = Task("taskName", mapOf("key" to "value"))
    val step = Step(
      script = "echo 'Hello'", displayName = "Greet", task = task.name, inputs = task.inputs
    )

    assertEquals("echo 'Hello'", step.script)
    assertEquals("Greet", step.displayName)
    assertEquals(task.inputs, step.inputs)
    assertNull(step.name)
  }
}