package com.dorkag.azure_devops.dto.flow

import kotlin.test.Test
import kotlin.test.assertEquals

class StageTest {

  @Test
  fun `test Stage properties`() {
    val jobs = listOf(
      Job(
        "job1",
        steps = listOf(Step(script = "echo 'Job 1'"))
      )
    )
    val stage = Stage(
      stage = "stage1",
      displayName = "Test Stage",
      dependsOn = listOf("previousStage"),
      condition = "succeeded()",
      variables = mapOf("var1" to "value1"),
      template = "template.yml",
      parameters = mapOf("param1" to "value"),
      jobs = jobs
    )

    assertEquals("stage1", stage.stage)
    assertEquals("Test Stage", stage.displayName)
    assertEquals(listOf("previousStage"), stage.dependsOn)
    assertEquals("succeeded()", stage.condition)
    assertEquals(mapOf("var1" to "value1"), stage.variables)
    assertEquals("template.yml", stage.template)
    assertEquals(mapOf("param1" to "value"), stage.parameters)
    assertEquals(jobs, stage.jobs)
  }
}