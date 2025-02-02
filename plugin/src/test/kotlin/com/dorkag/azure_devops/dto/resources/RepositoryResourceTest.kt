package com.dorkag.azure_devops.dto.resources

import com.dorkag.azure_devops.dto.triggers.RepositoryTrigger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RepositoryResourceTest {

  @Test
  fun `test RepositoryResource initialization without optional fields`() {
    val repositoryResource = RepositoryResource(
      repository = "repo1", type = "git", name = "Repo Name"
    )

    assertEquals("repo1", repositoryResource.repository)
    assertEquals("git", repositoryResource.type)
    assertEquals("Repo Name", repositoryResource.name)
    assertNull(repositoryResource.ref)
    assertNull(repositoryResource.endpoint)
    assertNull(repositoryResource.trigger)
  }

  @Test
  fun `test RepositoryResource initialization with all fields`() {
    val repositoryTrigger = RepositoryTrigger(
      branches = listOf("develop"), tags = listOf("v1.0"), paths = listOf("/src", "/docs")
    )

    val repositoryResource = RepositoryResource(
      repository = "repo1", type = "git", name = "Repo Name", ref = "refs/heads/main", endpoint = "https://example.com", trigger = repositoryTrigger
    )

    assertEquals("repo1", repositoryResource.repository)
    assertEquals("git", repositoryResource.type)
    assertEquals("Repo Name", repositoryResource.name)
    assertEquals("refs/heads/main", repositoryResource.ref)
    assertEquals("https://example.com", repositoryResource.endpoint)
    assertEquals(repositoryTrigger, repositoryResource.trigger)
  }
}