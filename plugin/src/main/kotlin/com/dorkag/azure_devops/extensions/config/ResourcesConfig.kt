package com.dorkag.azure_devops.extensions.config

import com.dorkag.azure_devops.dto.resources.*
import com.dorkag.azure_devops.extensions.pipeline.PipelineResourceConfig
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import javax.inject.Inject

open class ResourcesConfig @Inject constructor(val objects: ObjectFactory) {
    // Example: Repositories, containers, pipeline references, etc.
    val repositories: MapProperty<String, RepositoryResourceConfig> =
        objects.mapProperty(String::class.java, RepositoryResourceConfig::class.java)
    val containers: MapProperty<String, ContainerResourceConfig> =
        objects.mapProperty(String::class.java, ContainerResourceConfig::class.java)

    // Pipelines DSL
    val pipelines: MapProperty<String, PipelineResourceConfig> =
        objects.mapProperty(String::class.java, PipelineResourceConfig::class.java)

    @Suppress("unused")
    fun repositories(action: ReposDsl.() -> Unit) {
        val dsl = ReposDsl(repositories, objects)
        dsl.action()
    }

    @Suppress("unused")
    fun containers(action: ContainersDsl.() -> Unit) {
        val dsl = ContainersDsl(containers, objects)
        dsl.action()
    }

    fun pipelines(action: PipelinesDsl.() -> Unit) {
        val dsl = PipelinesDsl(pipelines, objects)
        dsl.action()
    }

    class ReposDsl(
        private val repos: MapProperty<String, RepositoryResourceConfig>, private val objects: ObjectFactory
    ) {
        operator fun String.invoke(configuration: RepositoryResourceConfig.() -> Unit) {
            val repoCfg = objects.newInstance(RepositoryResourceConfig::class.java)
            repoCfg.configuration()
            repos.put(this, repoCfg)
        }
    }

    class ContainersDsl(
        private val containers: MapProperty<String, ContainerResourceConfig>, private val objects: ObjectFactory
    ) {
        operator fun String.invoke(configuration: ContainerResourceConfig.() -> Unit) {
            val containerCfg = objects.newInstance(ContainerResourceConfig::class.java)
            containerCfg.configuration()
            containers.put(this, containerCfg)
        }
    }

    class PipelinesDsl(
        private val pipelines: MapProperty<String, PipelineResourceConfig>, private val objects: ObjectFactory
    ) {
        operator fun String.invoke(configuration: PipelineResourceConfig.() -> Unit) {
            val pipelineCfg = objects.newInstance(PipelineResourceConfig::class.java, this)
            pipelineCfg.configuration()
            pipelines.put(this, pipelineCfg)
        }
    }

    internal fun toResources(): Resources {
        // Convert DSL objects into the final Resources DTO
        val repoResources = repositories.get().map { (name, cfg) -> cfg.toRepositoryResource(name) }
        val containerRes = containers.get().map { (name, cfg) -> cfg.toContainerResource(name) }
        val pipelineRes = pipelines.get().map { (_, cfg) -> cfg.toPipelineResource() }

        return Resources(
            repositories = repoResources.ifEmpty { null },
            containers = containerRes.ifEmpty { null },
            pipelines = pipelineRes.ifEmpty { null },
            builds = null // if you have build resources
        )
    }
}