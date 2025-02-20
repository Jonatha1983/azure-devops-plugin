package com.dorkag.azure_devops.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object AzureCommentMetadataGenerator {


  fun generateMetadataComment(pluginVersion: String, gradleVersion: String): String {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd:HH:mm:ss.SSS"))

    return """
            # This file was generated by the Azure DevOps Pipeline Gradle Plugin
            # Generation time: $timestamp
            # Plugin version: $pluginVersion
            # Gradle version: $gradleVersion
        """.trimIndent()
  }
}