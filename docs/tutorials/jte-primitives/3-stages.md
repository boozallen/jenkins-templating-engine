# Stages

## What is a Stage?

Stages are a mechanism for chaining multiple steps together to be invoked through an aliased method name.

As Pipeline Templates mature in complexity and grow to represent branching strategies for application development teams, it's likely that you would want to call the same series of steps multiple times in the template.

To minimize repeating ourselves in Pipeline Templates, the Stage primitive was created to address this use case.

!!! note
    [View the Stage documentation here.](../../concepts/pipeline-primitives/stages.md)

## Define and Use a Stage

A very common Stage to create is a *Continuous Integration* stage.

!!! note
    In general, Continuous Integration represents a series of fast-feedback verification steps that help developers quickly determine if the changes made to the code have broken anything obvious. It's common to run steps like building an artifact, running unit tests, and performing static code analysis on every commit in a source code repository and then once again in the Pull Request job to `main/master` to verify the merged result is still functional.

Our current Pipeline Template specifies to run the `build()` and `static_code_analysis()` steps. Let's group these together into a `continuous_integration()` stage.

### Define the Stage in the Pipeline Configuration

In your `single-job`, update the *Pipeline Configuration* to:

``` groovy title="Pipeline Configuration"
libraries {
    maven
    sonarqube
}

stages {
    continuous_integration {
        build
        static_code_analysis
    }
}
```

!!! important
    All Stages will be defined in the `stages` block of the Pipeline Configuration. Root level keys within this block, in this case `continuous_integration`, will become invoked methods within the Pipeline Template.

    The lines within the `continuous_integration` block outline which steps will be chained together when the stage is invoked.

### Update the Pipeline Template

With the `continuous_integration` stage defined, we can update the Pipeline Template (Jenkinsfile, in your `single-job`) to make use of it.

Update the *Pipeline Template* to:

``` groovy title="Pipeline Template"
continuous_integration() 
```

Then click `Save`.

### Run the Pipeline

From the Pipeline Job's main page, click `Build Now` in the left-hand navigation menu.

When viewing the build logs (click Build Number and then `Console Output`), you should see output similar to:

``` text
Started by user admin
[JTE] Pipeline Configuration Modifications (show)
[JTE] Obtained Pipeline Template from job configuration
[JTE] Loading Library maven (show)
[JTE] Loading Library sonarqube (show)
[JTE] Template Primitives are overwriting Jenkins steps with the following names: (show)
[Pipeline] Start of Pipeline
[JTE][Stage - continuous_integration]
[JTE][Step - maven/build.call()]
[Pipeline] stage
[Pipeline] { (Maven: Build)
[Pipeline] echo
build from the maven library
[Pipeline] }
[Pipeline] // stage
[JTE][Step - sonarqube/static_code_analysis.call()]
[Pipeline] stage
[Pipeline] { (SonarQube: Static Code Analysis)
[Pipeline] echo
static code analysis from the sonarqube library
[Pipeline] }
[Pipeline] // stage
[Pipeline] End of Pipeline
Finished: SUCCESS
```

!!! important
    When reading the build logs of a JTE job, you can identify the start of stages by looking for ``[JTE] [Stage - *]`` in the output.

    In this case, the log output was `[JTE] [Stage - continuous_integration]` indicating a Stage called continuous_integration` is about to be executed.
