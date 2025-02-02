package com.dorkag.azure_devops.dto.resources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResourcesTest {

  @Test
  fun `test Resources initialization without any fields`() {
    val resources = Resources()

    assertNull(resources.builds)
    assertNull(resources.containers)
    assertNull(resources.pipelines)
    assertNull(resources.repositories)
  }

  @Test
  fun `test Resources initialization with all fields`() {
    val buildResource = BuildResource(source = "source1", type = "type1")
    val repositoryResource = RepositoryResource(repository = "repo1", type = "git", name = "Repo Name")

    val resources = Resources(
      builds = listOf(buildResource),
      containers = emptyList(), // Assuming a ContainerResource class exists
      pipelines = emptyList(), // Assuming a PipelineResource class exists
      repositories = listOf(repositoryResource)
    )

    assertEquals(1, resources.builds?.size)
    assertEquals(buildResource, resources.builds?.first())

    assertEquals(1, resources.repositories?.size)
    assertEquals(repositoryResource, resources.repositories?.first())

    assertEquals(0, resources.containers?.size)
    assertEquals(0, resources.pipelines?.size)
  }
}
