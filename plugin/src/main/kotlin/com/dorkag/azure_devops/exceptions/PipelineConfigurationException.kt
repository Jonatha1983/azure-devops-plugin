package com.dorkag.azure_devops.exceptions

import org.gradle.api.InvalidUserDataException

/**
 * Exception thrown when the pipeline configuration is invalid.
 * @param message The exception message.
 *
 */
class PipelineConfigurationException(message: String) : InvalidUserDataException(message)