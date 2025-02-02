plugins {
  id("com.dorkag.azuredevops")
}

azurePipeline {
  name.set("SubA Pipeline")
  vmImage.set("ubuntu-latest")
  trigger.set(listOf("main"))

  stages {
    stage("Build") {
      displayName.set("Build Stage")
      jobs {
        job("buildJob") {
          displayName.set("Build")
          steps {
            step("buildStep") {
              script.set("./gradlew build")
              displayName.set("Build")
            }
          }
        }
      }
    }
    stage("Test") {
      displayName.set("Test Stage")
      jobs {
        job("testJob") {
          displayName.set("Test")
          steps {
            step("testStep") {
              script.set("./gradlew test")
              displayName.set("Run Tests")
            }
          }
        }
      }
    }
  }
}