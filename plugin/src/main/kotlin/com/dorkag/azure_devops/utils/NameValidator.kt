package com.dorkag.azure_devops.utils

import com.dorkag.azure_devops.exceptions.PipelineConfigurationException


object NameValidator {
  private val validNamePattern = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")

  fun validateName(name: String, entityType: String): String {
    if (!name.matches(validNamePattern)) {
      throw PipelineConfigurationException(
        """
                Invalid $entityType name: '$name'.
                Valid names:
                - Must start with a letter or underscore
                - Can only contain alphanumeric characters and underscores
                - Cannot start with a number
                """.trimIndent()
      )
    }
    return name
  }
}