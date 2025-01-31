package com.dorkag.azure_devops.utils

import com.dorkag.azure_devops.exceptions.PipelineConfigurationException


object NameValidator {

  // Azure docs restrictions for job/stage:
  //   - Must only contain [a-zA-Z0-9_] (alphanumeric, underscore)
  //   - Must NOT start with a digit
  // Example pattern: ^[a-zA-Z_][a-zA-Z0-9_]*$

  private val VALID_NAME_REGEX = Regex("^[A-Za-z_][A-Za-z0-9_]*$")

  // If we want to disallow certain keywords, e.g. "deployment" or "deployment.":
  private val DISALLOWED_KEYWORDS = setOf("deployment", "deployment.")

  @Suppress("unused")
  enum class EntityType(val value: String) {
    JOB("job"),
    STAGE("stage"),
    STEP("step"),
    TASK("task")
  }

  /**
   * Validate that the given name is:
   *  - 1+ characters
   *  - starts with letter or underscore
   *  - only alphanumeric or underscore after that
   *  - does NOT contain any reserved keywords
   *
   * @throws PipelineConfigurationException if invalid
   * @return the validated name (unchanged) if valid
   */
  fun validateName(name: String, entityType: EntityType): String { // 1) Check regex
    if (!name.matches(VALID_NAME_REGEX)) {
      throw PipelineConfigurationException(
        """
                Invalid ${entityType.value} name: '$name'.
                Restrictions:
                - Must only contain alphanumeric characters and underscores (_).
                - Must not start with a number.
                - Must start with a letter or underscore.
                """.trimIndent()
      )
    }

    // 2) Check disallowed keywords
    //    We can do "equals" or "contains" depending on how strict we are.
    //    Here, I'll do case-insensitive "equals" to the entire keyword.
    DISALLOWED_KEYWORDS.forEach { keyword -> // If you want partial matches, do `name.contains(keyword, ignoreCase = true)`
      if (name.equals(keyword, ignoreCase = true)) {
        throw PipelineConfigurationException(
          "Invalid $entityType name: '$name'. It cannot be or contain the keyword '$keyword'."
        )
      }
    }

    // If all checks pass, return the name as valid
    return name
  }
}