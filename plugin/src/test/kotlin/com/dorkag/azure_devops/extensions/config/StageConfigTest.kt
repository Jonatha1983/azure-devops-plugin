package com.dorkag.azure_devops.extensions.config

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StageConfigTest {

  @Test
  fun `test StageConfig default values`() {
    val objects = ProjectBuilder.builder().build().objects
    val stageConfig = StageConfig(objects)

    assertEquals(false, stageConfig.enabled.get(), "Default value for enabled should be false")
    assertTrue(stageConfig.dependsOn.get().isEmpty(), "Default value for dependsOn should be an empty list")
    assertTrue(stageConfig.variables.get().isEmpty(), "Default value for variables should be an empty map")
    assertTrue(stageConfig.jobs.get().isEmpty(), "Default value for jobs should be an empty map")
  }

  @Test
  fun `test setting StageConfig properties`() {
    val objects = ProjectBuilder.builder().build().objects
    val stageConfig = StageConfig(objects)

    stageConfig.enabled.set(true)
    stageConfig.displayName.set("Test Stage")
    stageConfig.dependsOn.set(listOf("previousStage"))
    stageConfig.condition.set("always()")
    stageConfig.variables.put("KEY", "VALUE")

    assertEquals(true, stageConfig.enabled.get())
    assertEquals("Test Stage", stageConfig.displayName.get())
    assertEquals(listOf("previousStage"), stageConfig.dependsOn.get())
    assertEquals("always()", stageConfig.condition.get())
    assertEquals(mapOf("KEY" to "VALUE"), stageConfig.variables.get())
  }

  @Test
  fun `test StageConfig jobs DSL`() {
    val objects = ProjectBuilder.builder().build().objects
    val stageConfig = StageConfig(objects)

    stageConfig.jobs {
      job("buildJob") {
        displayName.set("Build Job")
      }
      job("testJob") {
        displayName.set("Test Job")
      }
    }

    val jobs = stageConfig.jobs.get()
    assertEquals(2, jobs.size, "There should be two jobs configured")
    assertNotNull(jobs["buildJob"], "buildJob should be in the jobs map")
    assertNotNull(jobs["testJob"], "testJob should be in the jobs map")
    assertEquals("Build Job", jobs["buildJob"]!!.displayName.get())
    assertEquals("Test Job", jobs["testJob"]!!.displayName.get())
  }

  @Test
  fun `test StageConfig variables`() {
    val objects = ProjectBuilder.builder().build().objects
    val stageConfig = StageConfig(objects)

    stageConfig.variables.putAll(mapOf("VAR1" to "Value1", "VAR2" to "Value2"))

    val variables = stageConfig.variables.get()
    assertEquals(2, variables.size, "There should be two variables configured")
    assertEquals("Value1", variables["VAR1"])
    assertEquals("Value2", variables["VAR2"])
  }

  @Test
  fun `test StageConfig dependsOn`() {
    val objects = ProjectBuilder.builder().build().objects
    val stageConfig = StageConfig(objects)

    stageConfig.dependsOn.add("stage1")
    stageConfig.dependsOn.add("stage2")

    val dependsOn = stageConfig.dependsOn.get()
    assertEquals(2, dependsOn.size, "There should be two stages in dependsOn")
    assertTrue(dependsOn.contains("stage1"), "dependsOn should contain 'stage1'")
    assertTrue(dependsOn.contains("stage2"), "dependsOn should contain 'stage2'")
  }
}