package com.dorkag.azure_devops.extensions.pipeline

import com.dorkag.azure_devops.dto.ParameterType
import org.gradle.api.model.ObjectFactory
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class PipelineParameterTest {

  private val factory: ObjectFactory = ProjectBuilder.builder().build().objects

  @Test
  fun `test default toDto`() {
    val pipelineParameter = object : PipelineParameter(factory) {
      override val displayName = factory.property(String::class.java)
      override val type = factory.property(String::class.java)
      override val default = factory.property(String::class.java)
    }
    pipelineParameter.name = "param1"
    pipelineParameter.displayName.set("Parameter 1")
    pipelineParameter.default.set("defaultValue")
    pipelineParameter.type.set("string")

    val parameter = pipelineParameter.toDto()

    assertEquals("param1", parameter.name)
    assertEquals("Parameter 1", parameter.displayName)
    assertEquals(ParameterType.STRING, parameter.type)
    assertEquals("defaultValue", parameter.default)
    assertEquals(emptyList<String>(), parameter.values) // Expect empty list here
  }


  @Test
  fun `test toDto with custom type and values`() {
    val pipelineParameter = object : PipelineParameter(factory) {
      override val displayName = factory.property(String::class.java)
      override val type = factory.property(String::class.java)
      override val default = factory.property(String::class.java)
    }
    pipelineParameter.name = "param2"
    pipelineParameter.displayName.set("Boolean Param")
    pipelineParameter.default.set("true")
    pipelineParameter.type.set("boolean")
    pipelineParameter.tValues().set(listOf("true", "false"))

    val parameter = pipelineParameter.toDto()

    assertEquals("param2", parameter.name)
    assertEquals("Boolean Param", parameter.displayName)
    assertEquals(ParameterType.BOOLEAN, parameter.type)
    assertEquals("true", parameter.default)
    assertEquals(listOf("true", "false"), parameter.values)
  }

  @Test
  fun `test toDto with fallback type`() {
    val pipelineParameter = object : PipelineParameter(factory) {
      override val displayName = factory.property(String::class.java)
      override val type = factory.property(String::class.java)
      override val default = factory.property(String::class.java)
    }
    pipelineParameter.name = "param3"
    pipelineParameter.displayName.set("Missing Type Param")
    pipelineParameter.default.set("defaultFallback")

    // Type is not provided
    val parameter = pipelineParameter.toDto()

    assertEquals("param3", parameter.name)
    assertEquals("Missing Type Param", parameter.displayName)
    assertEquals(ParameterType.STRING, parameter.type) // Fallback used
    assertEquals("defaultFallback", parameter.default)
  }

}