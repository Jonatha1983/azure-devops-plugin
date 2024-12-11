package org.dorkag.azure_devops.extensions

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

@AzurePipelineDsl
open class AzurePipelineExtension @Inject constructor(
    objects: ObjectFactory
) {

    var name: String = "Azure DevOps Pipeline"
    var triggerBranches: List<String> = listOf("main")
    var vmImage: String = "ubuntu-latest"
    var parameters: Map<String, String> = emptyMap()
    var variables: Map<String, String> = emptyMap()
    val stages = objects.domainObjectContainer(PipelineStageExtension::class.java)

    // Expose configuration methods for DSL
    fun stages(action: Action<NamedDomainObjectContainer<PipelineStageExtension>>) {
        action.execute(stages)
    }


}