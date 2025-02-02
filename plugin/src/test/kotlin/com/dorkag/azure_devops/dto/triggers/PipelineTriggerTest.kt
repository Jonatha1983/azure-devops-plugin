import com.dorkag.azure_devops.dto.triggers.PipelineTrigger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PipelineTriggerTest {

  @Test
  fun `test PipelineTrigger default values`() {
    val trigger = PipelineTrigger()
    assertNull(trigger.branches)
    assertNull(trigger.tags)
    assertNull(trigger.stages)
  }

  @Test
  fun `test PipelineTrigger with values`() {
    val branches = listOf("main", "develop")
    val tags = listOf("v1.0", "v2.0")
    val stages = listOf("build", "test")
    val trigger = PipelineTrigger(branches = branches, tags = tags, stages = stages)

    assertEquals(branches, trigger.branches)
    assertEquals(tags, trigger.tags)
    assertEquals(stages, trigger.stages)
  }

  @Test
  fun `test PipelineTrigger equality`() {
    val trigger1 = PipelineTrigger(branches = listOf("main"), tags = listOf("v1.0"), stages = listOf("build"))
    val trigger2 = PipelineTrigger(branches = listOf("main"), tags = listOf("v1.0"), stages = listOf("build"))

    assertEquals(trigger1, trigger2)
  }
}
