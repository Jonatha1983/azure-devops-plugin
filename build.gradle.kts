fun properties(key: String) = providers.gradleProperty(key)

plugins {
    base
}

repositories {
    mavenCentral()
}

group = properties("azdpp.group").get()
version = properties("azdpp.version").get()