plugins {
  id("com.dorkag.azuredevops")
}

azurePipeline {
  stages {
    declaredStage("Build")  // Reuse root's Build stage
  }
}