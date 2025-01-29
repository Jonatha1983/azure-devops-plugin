fun properties(key: String) = providers.gradleProperty(key)

plugins {
  base
}


group = properties("azdpp.group").get()
version = properties("azdpp.version").get()