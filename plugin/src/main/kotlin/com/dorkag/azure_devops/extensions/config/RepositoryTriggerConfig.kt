package com.dorkag.azure_devops.extensions.config

import com.dorkag.azure_devops.dto.triggers.RepositoryTrigger
import javax.inject.Inject

open class RepositoryTriggerConfig @Inject constructor() {
    var branches: List<String>? = null
    private var tags: List<String>? = null
    private var paths: List<String>? = null

    internal fun toRepositoryTrigger() = RepositoryTrigger(
        branches = branches, tags = tags, paths = paths
    )
}