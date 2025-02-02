plugins {
  id("com.dorkag.azuredevops")
}

azurePipeline {
  name.set("Gradle Task Pipeline")
  vmImage.set("ubuntu-latest")
  trigger.set(listOf("main"))

  stages {
    stage("BuildStage") {
      displayName.set("Build Stage")
      jobs {
        job("BuildJob") {
          steps {
            step("GradleStep") {
              task("Gradle@3") {
                displayName.set("Run Gradle Build")
                inputs.put("tasks", "clean build")
              }
            }
          }
        }
      }
    }
  }
}