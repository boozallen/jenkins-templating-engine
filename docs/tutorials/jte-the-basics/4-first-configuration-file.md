# Configure the Pipeline

With the libraries created now discoverable by JTE, we can configure the pipeline and run it!

From the main Jenkins page, click the job created earlier in this lab. In the left-hand navigation menu, click `Configure`.

Scrolling down to the `Pipeline` portion of the job configuration, you should still see the Pipeline Template created earlier in the `Jenkinsfile` text box:

``` groovy
build()
static_code_analysis() 
```

It's now time to configure the pipeline by providing a Pipeline Configuration.

Check the box for `Provide pipeline configuration`. In the `Pipeline Configuration` text box enter:

``` groovy
libraries {
    maven
    sonarqube
}
```

Click `Save`.

!!! important
    The `libraries` portion of the Pipeline Configuration file will read much like an application's technical stack. In this case, we're telling JTE during the initialization of the pipeline that it should load the `maven` and `sonarqube` libraries.

    The `maven` library will provide the `build()` step and the `sonarqube` library will provide the `static_code_analysis()` step.

With these steps now implemented, we can run the pipeline.

## Run the Pipeline

After clicking `Save` you'll be directed back to the main page for the job.

To run the pipeline, click `Build Now` in the left-hand navigation menu.

Refresh the page and you should see build number 1 in the `Build History` on the bottom left-hand side of the screen. Click the link to go to the Build's page. If you see a red X, something is wrong with your configuration. Look at the logs and see if they give any meaningful information; there may be something wrong with your library code or your branch may be wrong in the Library Sources configuration you set up earlier.

In the left-hand navigation menu, click `Console Output` to view the build logs for this run of the pipeline.

``` text
Started by user admin
[JTE] Pipeline Configuration Modifications (show)
[JTE] Obtained Pipeline Template from job configuration
[JTE] Loading Library maven (show)
[JTE] Loading Library sonarqube (show)
[JTE] Template Primitives are overwriting Jenkins steps with the following names: 
[JTE]   1. build
[Pipeline] Start of Pipeline
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

As expected, JTE loaded the `maven` and `sonarqube` libraries and then executed the template.

!!! note
    A couple points to note about the build log:

    * Any line that starts with `[JTE]` is a log coming from the Jenkins Templating Engine.
    * Pieces of JTE log output are hidden by default. Clicking `show` will expand these sections.
    * The first part of the pipeline output shows the initialization process:
        * Pipeline Configuration files being aggregated.
        * Libraries being loaded.
    * Before steps are executed, JTE will tell you which step is being run and what library contributed the step.
