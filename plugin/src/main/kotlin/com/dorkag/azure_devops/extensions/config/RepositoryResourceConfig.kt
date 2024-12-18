package com.dorkag.azure_devops.extensions.config

import com.dorkag.azure_devops.dto.resources.RepositoryResource
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class RepositoryResourceConfig @Inject constructor(
    val objects: ObjectFactory
) {
    var type: String = ""
    var repoName: String = ""
    var ref: String? = null
    var endpoint: String? = null
    private var triggerConfig: RepositoryTriggerConfig? = null

    fun trigger(action: Action<RepositoryTriggerConfig>) {
        triggerConfig = RepositoryTriggerConfig()
        action.execute(triggerConfig!!)
    }

    /**
     * Accepts repoNameKey from the DSL map key
     */
    internal fun toRepositoryResource(repoNameKey: String): RepositoryResource {
        return RepositoryResource(
            repository = repoNameKey, type = type, name = repoName,           // e.g. actual VCS repo
            ref = ref, endpoint = endpoint, trigger = triggerConfig?.toRepositoryTrigger()
        )
    }
}