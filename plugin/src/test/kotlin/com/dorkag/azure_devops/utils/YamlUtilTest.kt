package com.dorkag.azure_devops.utils

import com.dorkag.azure_devops.dto.Pipeline
import com.dorkag.azure_devops.dto.Pool
import com.dorkag.azure_devops.dto.Stage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class YamlUtilTest {

    @Test
    fun `test toYaml and fromYaml`() {
        val pipeline = Pipeline(
            name = "Sample Pipeline",
            trigger = listOf("main", "develop"),
            pr = listOf("feature/*"),
            pool = Pool(vmImage = "ubuntu-latest"),
            variables = mapOf("env" to "prod"),
            stages = listOf(
                Stage(
                    stage = "Build",
                    displayName = "Build Stage",
                    dependsOn = null,
                    condition = "always()",
                    variables = mapOf("BUILD_NUM" to "123"),
                    jobs = null
                )
            )
        )

        val yamlString = YamlUtil.toYaml(pipeline)
        assertNotNull(yamlString, "YAML string should not be null or empty")

        val parsedPipeline = YamlUtil.fromYaml<Pipeline>(yamlString)
        assertEquals(pipeline.name, parsedPipeline.name)
        assertEquals(pipeline.trigger, parsedPipeline.trigger)
        assertEquals(pipeline.pr, parsedPipeline.pr)
        assertEquals(pipeline.pool.vmImage, parsedPipeline.pool.vmImage)
        assertEquals(pipeline.variables, parsedPipeline.variables)
        assertEquals(pipeline.stages.size, parsedPipeline.stages.size)
        assertEquals(pipeline.stages[0].stage, parsedPipeline.stages[0].stage)
    }
}