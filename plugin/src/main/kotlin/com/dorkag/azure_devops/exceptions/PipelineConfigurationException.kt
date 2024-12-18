package com.dorkag.azure_devops.exceptions

import org.gradle.api.InvalidUserDataException

class PipelineConfigurationException : InvalidUserDataException {
    constructor(message: String) : super(message)
}