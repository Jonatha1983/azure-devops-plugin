# Azure DevOps Pipeline Plugin

[![Build](https://github.com/Jonatha1983/azure-devops-plugin/actions/workflows/build.yaml/badge.svg?branch=main)](https://github.com/Jonatha1983/azure-devops-plugin/actions/workflows/build.yaml)
[![codecov](https://codecov.io/gh/Jonatha1983/azure-devops-plugin/graph/badge.svg?token=7011WZ10IR)](https://codecov.io/gh/Jonatha1983/azure-devops-plugin)

The Azure DevOps Pipeline Plugin is a [Gradle](https://docs.gradle.org/current/userguide/userguide.html) plugin that
allows you to generate Azure DevOps pipeline YAML files from your Gradle project.

<img src=".idea/icon.png" alt="Azure DevOps Pipelines Plugin" style="width: 600px; height: 350px;" />

## Features

- Generate Azure DevOps pipeline YAML files from Gradle DSL.
- Support for a multi-project builds with template inheritance.
- Pipeline validation task to ensure generated YAML matches expectations.
- YAML to DSL conversion utility for existing pipelines.

## Diagram

```mermaid
classDiagram
  class AzureDevopsPluginPlugin {
    +apply(project: Project)
    -checkGradleVersion()
    -applyToRootProject()
    -applyToSubproject()
    -validateRootAfterEvaluate()
  }

  class AzurePipelineExtension {
    +name: Property_String_
    +trigger: ListProperty_String_
    +vmImage: Property_String_
    +stages: MapProperty_String_StageConfig_
    +stages(action: StagesDsl.() -> Unit)
    +resources(action: Action_ResourcesConfig_)
  }

  class AzurePipelineSubProjectExtension {
    +name: Property_String_
    +trigger: ListProperty_String_
    +vmImage: Property_String_
    -stagesContainer: MapProperty_String_StageConfig_
    +stages(action: SubStagesDsl.() -> Unit)
  }

  class Tasks {
    +GenerateRootPipelineTask
    +GenerateSubprojectTemplateTask
    +ValidatePipelineTask
    +GenerateDslFromYamlTask
  }

  class DTOs {
    +Pipeline
    +Stage
    +Job
    +Step
    +Task
  }

  AzureDevopsPluginPlugin --> AzurePipelineExtension: creates
  AzureDevopsPluginPlugin --> AzurePipelineSubProjectExtension: creates
  AzureDevopsPluginPlugin --> Tasks: registers
  Tasks --> DTOs: uses
  AzurePipelineExtension --> DTOs: configures
  AzurePipelineSubProjectExtension --> DTOs: configures
```

### See the full [documentation](https://jonatha1983.github.io/azure-devops-plugin/azure-devops-pipeline-plugin.html) for more information.

