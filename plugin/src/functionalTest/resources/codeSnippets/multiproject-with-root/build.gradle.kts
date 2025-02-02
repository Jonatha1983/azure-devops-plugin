plugins {
  id("com.dorkag.azuredevops")
}

azurePipeline {
  name.set("Root Pipeline")
  vmImage.set("ubuntu-latest")
  trigger.set(listOf("main"))

  stages {
    stage("Build") {
      displayName.set("Build")
      jobs {
        job("rootBuildJob") {
          displayName.set("Build")
          steps {
            step("rootBuildStep") {
              script.set("./gradlew build")
              displayName.set("Build")
            }
          }
        }
      }
    }
  }
}