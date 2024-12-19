package com.dorkag.azure_devops.utils


class DslBuilder(private val indentSize: Int = 4) {
    private val sb = StringBuilder()
    private var indentLevel = 0

    private fun currentIndent(): String = " ".repeat(indentLevel * indentSize)

    fun line(content: String = "") {
        sb.appendLine("${currentIndent()}$content")
    }

    fun block(name: String, body: DslBuilder.() -> Unit) {
        line("$name {")
        indentLevel++
        body()
        indentLevel--
        line("}")
    }

    fun build(): String = sb.toString()
}