package org.dorkag.azure_devops.exceptions

import org.gradle.api.InvalidUserDataException

class PipelineConfigurationException : InvalidUserDataException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}