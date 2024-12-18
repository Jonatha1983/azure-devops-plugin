import org.jetbrains.changelog.date

fun properties(key: String) = providers.gradleProperty(key)

plugins {
    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.3.0"

    // Apply the Kotlin JVM plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.changelog)
    alias(libs.plugins.qodana)
    alias(libs.plugins.kover)
}

group = properties("azdpp.group").get()
version = properties("azdpp.version").get()

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

}

testing {
    @Suppress("UnstableApiUsage") suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            useKotlinTest("2.0.20")
        }

        // Create a new test suite
        @Suppress("unused") val functionalTest by registering(JvmTestSuite::class) {
            useKotlinTest("2.0.20")

            dependencies {
                // functionalTest test suite depends on the production code in tests
                implementation(project())
            }

            targets {
                all {
                    // This test suite should run after the built-in test suite has run its tests
                    testTask.configure { shouldRunAfter(test) }
                }
            }
        }
    }
}

gradlePlugin {
    vcsUrl = "https://github.com/Jonatha1983/azure-devops-plugin"
    // Define the plugin
    @Suppress("unused") val azuredevops by plugins.creating {
        id = "com.dorkag.azuredevops"
        implementationClass = "com.dorkag.azure_devops.AzureDevopsPluginPlugin"
        displayName = "Azure DevOps Pipelines Plugin"
        description = "A plugin for generating Azure DevOps pipelines from Gradle configuration"
        tags = listOf("azure", "devops", "pipelines", "generator")
    }
}

gradlePlugin.testSourceSets.add(sourceSets["functionalTest"])

@Suppress("UnstableApiUsage") tasks.named<Task>("check") {
    // Include functionalTest as part of the check lifecycle
    dependsOn(testing.suites.named("functionalTest"))
}


kover {
    reports {
        total {
            xml {
                onCheck = true
            }
            html {
                onCheck = true
            }
        }
    }
    currentProject {
        sources {
            excludedSourceSets.addAll(listOf("test", "functionalTest"))
        }

    }
}


changelog {
    repositoryUrl.set(properties("repositoryUrl"))
    version.set(properties("azdpp.version"))
    path.set(file("CHANGELOG.md").canonicalPath)
    header.set(provider { "[${version.get()}] - ${date()}" })
    headerParserRegex.set("""(\d+\.\d+\.\d+)""".toRegex())
    introduction.set(
        """
        Azure DevOps Pipelines Plugin
        """.trimIndent()
    )
    itemPrefix.set("-")
    keepUnreleasedSection.set(true)
    unreleasedTerm.set("[Unreleased]")
    groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
    lineSeparator.set("\n")
    combinePreReleases.set(true)
    repositoryUrl = properties("repositoryUrl")
}

