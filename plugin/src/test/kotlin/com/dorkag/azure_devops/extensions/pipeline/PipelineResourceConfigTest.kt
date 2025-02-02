package com.dorkag.azure_devops.extensions.pipeline

import kotlin.test.Test
import kotlin.test.assertEquals

class PipelineResourceConfigTest {

  @Test
  fun `test toPipelineResource with defaults`() {
    val pipelineResourceConfig = PipelineResourceConfig("resource1")

    val resource = pipelineResourceConfig.toPipelineResource()

    assertEquals("resource1", resource.pipeline)
    assertEquals("", resource.source)
    assertEquals(null, resource.version)
    assertEquals(null, resource.branch)
    assertEquals(null, resource.trigger)
  }

  @Test
  fun `test toPipelineResource with custom values`() {
    val pipelineResourceConfig = PipelineResourceConfig("resource2").apply configApply@{
      this::class.java.getDeclaredField("source").apply fieldApply@{
        isAccessible = true
        set(this@configApply, "customSource")
      }
      this::class.java.getDeclaredField("version").apply versionApply@{
        isAccessible = true
        set(this@configApply, "v1.0")
      }
      this::class.java.getDeclaredField("branch").apply branchApply@{
        isAccessible = true
        set(this@configApply, "main")
      }
    }

    val resource = pipelineResourceConfig.toPipelineResource()

    assertEquals("resource2", resource.pipeline)
    assertEquals("customSource", resource.source)
    assertEquals("v1.0", resource.version)
    assertEquals("main", resource.branch)
    assertEquals(null, resource.trigger)
  }

  @Test
  fun `test trigger configuration`() {
    val pipelineResourceConfig = PipelineResourceConfig("resource3")
    pipelineResourceConfig.trigger {
      it.branches = listOf("main", "develop")
      it.stages = listOf("build", "test")
    }

    val resource = pipelineResourceConfig.toPipelineResource()

    assertEquals("resource3", resource.pipeline)
    assertEquals("", resource.source) // Expect empty string, not null
    assertEquals(null, resource.version)
    assertEquals(null, resource.branch)
    assertEquals(listOf("main", "develop"), resource.trigger?.branches)
    assertEquals(listOf("build", "test"), resource.trigger?.stages)
  }
}