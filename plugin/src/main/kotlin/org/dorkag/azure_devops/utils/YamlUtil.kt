package org.dorkag.azure_devops.utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.KotlinModule

object YamlUtil {
    private val mapper: ObjectMapper = ObjectMapper(
        YAMLFactory().apply {
            disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            enable(YAMLGenerator.Feature.INDENT_ARRAYS)
            disable(YAMLGenerator.Feature.SPLIT_LINES)
            configure(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR, true)
            configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true)
        }).apply {
        registerModule(KotlinModule.Builder().build())
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        setSerializationInclusion(JsonInclude.Include.NON_EMPTY) // Add this line
        writer().with(DefaultPrettyPrinter().apply {
            indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
            indentObjectsWith(DefaultIndenter("  ", "\n"))
        })
    }

    fun toYaml(data: Any): String = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(data)
}


