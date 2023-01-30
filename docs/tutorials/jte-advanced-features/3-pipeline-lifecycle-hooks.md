# Pipeline Lifecycle Hooks

There can be interdependent functionalities that present a challenge to the Jenkins Templating Engine.

For example, let's say we wanted to introduce Splunk(link) monitoring by sending events as part of the pipeline.

How do you:

* Maintain a clean, easy-to-read Pipeline Template?
* Maintain a separation of duties between libraries as to not hard-code a Splunk integration into every Library Step?

It would be great if there was a seamless way to inject functionality in response to different phases of the pipeline without having to tightly couple that functionality to existing Library Steps or Pipeline Templates...

*There is!* The Jenkins Templating Engine has a neat feature we call Pipeline Lifecycle Hooks that were made for just these situations.

We'll walk through the Splunk use case to demonstrate this functionality.

!!! note
    Read the [entire Pipeline Lifecycle Hook documentation](../../concepts/library-development/lifecycle-hooks.md).

## Create a Splunk Library

Methods defined within steps are able to register themselves to correspond to specific lifecycle events via annotations. As such, these steps are typically not invoked directly by other steps or from the Pipeline Template.

Because of this, the name of the step is inconsequential but can't conflict with other step names that are loaded.

Therefore, we typically recommend following a naming convention of prepending the step name with the library name.

!!! important
    It doesn't matter what you call steps that only contain Pipeline Lifecycle Hook-annotated methods. But to avoid collisions of everyone naming their hook steps `beforeStep.groovy` - we recommend `<libraryName>_<action>` as we'll demonstrate in this lab.

### Notify of Pipeline Start

Within the same Library Source repo you created during JTE: The Basics, create a step called `splunk_pipeline_start.groovy` within `libraries/splunk`:

``` groovy title="./libraries/splunk/steps/splunk_pipeline_start.groovy"
@Init 
void call() {
    println "Splunk: beginning of the pipeline!" 
}
```

Breaking down this step, the `@Init` registers the `call` method defined in this step to be invoked at the beginning of the pipeline.

### Update the Pipeline Configuration

In the `single-job` again, update the Pipeline Configuration to load the `splunk` library we just created.

``` groovy title="Pipeline Configuration"
libraries {
    maven
    sonarqube
    ansible
    splunk
}
```

That's it! Just by loading the library, JTE will be able to find the methods within steps annotated with a Pipeline Lifecycle Hook.

Run the job and you should see output in the logs similar to:

``` text
[JTE][@Init - splunk/splunk_pipeline_start.call]
[Pipeline] echo
Splunk: beginning of the pipeline!
```

### Add Before and After Step Execution Hooks

Let's add some hooks that inject themselves both before and after each step is executed in the pipeline. Add an additional step file to your Splunk library:

``` groovy title="./libraries/splunk/steps/splunk_step_watcher.groovy"
@BeforeStep
void before() {
    println "Splunk: running before the ${hookContext.library} library's ${hookContext.step} step" 
}

@AfterStep
void after() {
    println "Splunk: running after the ${hookContext.library} library's ${hookContext.step} step" 
}
```

Take notice of the JTE-native `hookContext` variable. This variable provides runtime context for the hook based on the "event" that is triggering the hook to run.

!!! note
    Make sure you push your code to the `main/master` branch before running.

    Here, we're defining two different methods in a single step. In the next section we'll talk about this in more detail. For right now, the important piece is that the method's have the `@BeforeStep` and `@AfterStep` annotations.

Rerunning the pipeline, we can now see these hooks get executed before and after (Maven in this snippet):

``` text
[JTE][@BeforeStep - splunk/splunk_step_watcher.before]
[Pipeline] echo
Splunk: running before the maven library's build step
[JTE][Step - maven/build.call()]
[Pipeline] stage
[Pipeline] { (Maven: Build)
[Pipeline] echo
build from the maven library
[Pipeline] }
[Pipeline] // stage
[JTE][@AfterStep - splunk/splunk_step_watcher.after]
[Pipeline] echo
Splunk: running after the maven library's build step
```

### Notify of End of Pipeline Execution

Let's try out one more hook to get executed when the pipeline has finished, create a third step file:

``` groovy title="./libraries/splunk/splunk_pipeline_end.groovy"
@CleanUp
void call(context) {
    println "Splunk: end of the pipeline!" 
}
````

Push your code, then run the pipeline again and you should see logs at the end similar to:

``` text
[JTE][@CleanUp - splunk/splunk_pipeline_end.call]
[Pipeline] echo
Splunk: end of the pipeline!
[Pipeline] End of Pipeline
Finished: SUCCESS
```

## Restricting Hook Execution

What if we only wanted the `@AfterStep` hook to be executed after the `static_code_analysis` step?

Pipeline Lifecycle Hook annotations accept a *Closure* parameter. This Closure will be executed, and if the return of the Closure is non-false the step will be executed.

!!! important
    Remember: Groovy has implicit return statements. The last statement made becomes the return object by default.

We call this functionality *Conditional Hook Execution*.

### Update the `@AfterStep` Annotation

Let's see it in action.

Update the line with `@AfterStep` to:

``` groovy title="./libraries/splunk/steps/splunk_step_watcher.groovy"
@BeforeStep
void before() {
    println "Splunk: running before the ${hookContext.library} library's ${hookContext.step} step" 
}

@AfterStep({ hookContext.step.equals("static_code_analysis") })
void after() {
    println "Splunk: running after the ${hookContext.library} library's ${hookContext.step} step" 
}
```

Push your code, re-run the pipeline and notice that now, the hook has been restricted to only run after the desired step.

!!! important
    When the `Closure` parameter is invoked, it will have access to the `hookContext` variable as well as the library configuration that is stored via the `config` variable.

### Taking It A Step Further

It would be even better if we could externalize the configuration of exactly which steps the `@AfterStep` hook should be triggered.

To do this, update the `@AfterStep` annotation again to be:

``` groovy title="./libraries/splunk/steps/splunk_step_watcher.groovy"
@BeforeStep
void before() {
    println "Splunk: running before the ${hookContext.library} library's ${hookContext.step} step" 
}

@AfterStep({ hookContext.step in config.afterSteps })
void after() {
    println "Splunk: running after the ${hookContext.library} library's ${hookContext.step} step" 
}
```

Now, we can conditionally execute the hook by checking if the name of the step that was just executed is in an array called `afterSteps` defined as part of the `splunk` library in the Pipeline Configuration!

Update the `splunk` portion of the `single-job` Pipeline Configuration to:

``` groovy title="Pipeline Configuration"
libraries {
    maven
    sonarqube
    ansible
    splunk {
        afterSteps = [ "static_code_analysis", "unit_test"  ]
    }
}
```

Run the pipeline again and notice that the hook was only executed after the steps defined in the Pipeline Configuration.

!!! note
    Conditional Execution Closure Parameters can be passed to any Pipeline Lifecycle Hook annotation. As long as the Closure returns a non-false value, the hook will be invoked.

!!! important
    Remember to read through the [Pipeline Lifecycle Hook documentation](../../concepts/library-development/lifecycle-hooks.md) to see all the annotations available.
