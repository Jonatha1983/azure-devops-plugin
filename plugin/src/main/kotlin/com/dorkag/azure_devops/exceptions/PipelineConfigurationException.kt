package com.dorkag.azure_devops.exceptions

import org.gradle.api.InvalidUserDataException

class PipelineConfigurationException(message: String) : InvalidUserDataException(message) {
}