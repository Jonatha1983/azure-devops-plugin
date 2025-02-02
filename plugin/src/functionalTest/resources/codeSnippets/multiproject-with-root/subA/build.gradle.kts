plugins {
  id("com.dorkag.azuredevops")
}

azurePipeline {
  stages {
    declaredStage("Build")  // Reuse root's Build stage
    stage("TestA") {
      displayName.set("TestA Stage")
      jobs {
        job("testAJob") {
          steps {
            step("testAStep") {
              script.set("./gradlew test")
              displayName.set("Run Tests")
            }
          }
        }
      }
    }
  }
}