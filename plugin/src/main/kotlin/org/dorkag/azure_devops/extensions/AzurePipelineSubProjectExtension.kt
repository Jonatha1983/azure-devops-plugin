package org.dorkag.azure_devops.extensions

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class AzurePipelineSubProjectExtension @Inject constructor(
    objects: ObjectFactory
) {
    var enabled: Boolean = true
    var stages: List<String> = emptyList()
}