package com.dorkag.azure_devops.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DslBuilderTest {

    @Test
    fun `test empty builder`() {
        val builder = DslBuilder()
        val result = builder.build()
        assertTrue(result.isBlank(), "Empty builder should produce empty output")
    }

    @Test
    fun `test single line`() {
        val builder = DslBuilder()
        builder.line("hello")
        val result = builder.build()
        assertEquals("hello\n", result)
    }

    @Test
    fun `test block indentation`() {
        val builder = DslBuilder(indentSize = 2)
        builder.block("outer") {
            line("content")
            block("inner") {
                line("more content")
            }
        }
        val result = builder.build()

        /*
        Expected structure:
         outer {
           content
           inner {
             more content
           }
         }*/

        val lines = result.lines()
        assertEquals("outer {", lines[0])
        assertEquals("  content", lines[1])
        assertEquals("  inner {", lines[2])
        assertEquals("    more content", lines[3])
        assertEquals("  }", lines[4])
        assertEquals("}", lines[5])
    }

    @Test
    fun `test multiple blocks and lines`() {
        val builder = DslBuilder()
        builder.block("root") {
            line("val x = 42")
            block("nested") {
                line("println(x)")
            }
        }
        val result = builder.build().trimEnd()

        val expected = """
root {
    val x = 42
    nested {
        println(x)
    }
}
        """.trimIndent()
        assertEquals(expected, result)
    }
}