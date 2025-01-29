package com.dorkag.azure_devops.dto.flow

import com.dorkag.azure_devops.dto.Parameter
import com.dorkag.azure_devops.dto.ParameterType
import com.dorkag.azure_devops.dto.Pool
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PipelineTest {

  @Test
  fun `test Pipeline properties`() {
    val stages = listOf(
      Stage(
        stage = "build", jobs = listOf(Job(job = "buildJob", steps = listOf(Step(script = "echo 'Build'"))))
      )
    )
    val pipeline = Pipeline(
      name = "Pipeline1", trigger = listOf("master"), pr = listOf("feature/*"), pool = Pool(vmImage = "default"),  // Mock Pool or provide real implementation
      parameters = listOf(Parameter(name = "param1", displayName = "value1", type = ParameterType.STRING, default = null, values = emptyList())),  // Mock Parameter
      variables = mapOf("key" to "value"), stages = stages, resources = null,  // Mock Resources as needed
      schedules = null, lockBehavior = null, appendCommitMessageToRunName = true
    )

    assertEquals("Pipeline1", pipeline.name)
    assertEquals(listOf("master"), pipeline.trigger)
    assertEquals(listOf("feature/*"), pipeline.pr)
    assertEquals(Pool("default"), pipeline.pool)
    assertEquals(listOf(Parameter(name = "param1", displayName = "value1", type = ParameterType.STRING, default = null, values = emptyList())), pipeline.parameters)
    assertEquals(mapOf("key" to "value"), pipeline.variables)
    assertEquals(stages, pipeline.stages)
    assertNull(pipeline.resources)
    assertNull(pipeline.schedules)
    assertNull(pipeline.lockBehavior)
    assertEquals(true, pipeline.appendCommitMessageToRunName)
  }
}