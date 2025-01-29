package com.dorkag.azure_devops.dto.flow

import kotlin.test.Test
import kotlin.test.assertEquals

class JobTest {

  @Test
  fun `test Job properties`() {
    val steps = listOf(Step(script = "echo 'Hello'"))
    val job = Job(
      job = "job1",
      displayName = "Test Job",
      dependsOn = listOf("previousJob"),
      condition = "always()",
      continueOnError = true,
      timeoutInMinutes = 120,
      strategy = null,  // Mock or provide Strategy value as needed
      variables = mapOf("key" to "value"),
      steps = steps
    )

    assertEquals("job1", job.job)
    assertEquals("Test Job", job.displayName)
    assertEquals(listOf("previousJob"), job.dependsOn)
    assertEquals("always()", job.condition)
    assertEquals(true, job.continueOnError)
    assertEquals(120, job.timeoutInMinutes)
    assertEquals(mapOf("key" to "value"), job.variables)
    assertEquals(steps, job.steps)
  }
}