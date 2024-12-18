package com.dorkag.azure_devops.dto.resources

data class Resources(
    val builds: List<BuildResource>? = null,
    val containers: List<ContainerResource>? = null,
    val pipelines: List<PipelineResource>? = null,
    val repositories: List<RepositoryResource>? = null,
//    val webhooks: List<WebhookResource>? = null,
//    val packages: List<PackageResource>? = null
)