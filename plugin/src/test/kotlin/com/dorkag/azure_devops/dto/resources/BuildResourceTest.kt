package com.dorkag.azure_devops.dto.resources

import com.dorkag.azure_devops.dto.triggers.BuildTrigger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BuildResourceTest {

  @Test
  fun `test BuildResource initialization without optional fields`() {
    val buildResource = BuildResource(source = "source1", type = "type1")

    assertEquals("source1", buildResource.source)
    assertEquals("type1", buildResource.type)
    assertNull(buildResource.version)
    assertNull(buildResource.branch)
    assertNull(buildResource.trigger)
  }

  @Test
  fun `test BuildResource initialization with all fields`() {
    val buildTrigger = BuildTrigger(
      branches = listOf("main"), tags = listOf("release"), stages = listOf("deploy")
    )

    val buildResource = BuildResource(
      source = "source1", type = "type1", version = "1.0", branch = "main", trigger = buildTrigger
    )

    assertEquals("source1", buildResource.source)
    assertEquals("type1", buildResource.type)
    assertEquals("1.0", buildResource.version)
    assertEquals("main", buildResource.branch)
    assertEquals(buildTrigger, buildResource.trigger)
  }
}

