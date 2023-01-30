# Swap Libraries

The purpose of the Jenkins Templating Engine is three fold:

1. **Optimize pipeline code reuse**.

    Now organizations can coalesce around a portfolio of centralized, reusable Pipeline Libraries representing different tool integrations for their CI/CD pipelines.

2. **Simplify pipeline maintainability**.

    Separating a pipeline into templates, configuration files, and Pipeline Libraries can also be thought of as separating the *business logic* of your pipeline from the *technical implementation*. In our experience, it is significantly easier to manage a template backed by modularized Pipeline Libraries than it is to manage application-specific Jenkinsfiles.

3. **Provide organizational governance**.

    With the traditional Jenkinsfile defined within, and duplicated across, source code repositories it can be very challenging to ensure the same process is being followed by disparate application development teams and confirm that organizational policies around code quality and security are being met. JTE brings a level of governance to your pipelines by centralizing the definition of the pipeline workflow to a common place.

To demonstrate the reusability of pipeline templates, what would happen if the development team switched to using Gradle?

All we would have to do is create a `gradle` library that implements the `build()` step of the pipeline template.

## Create the Gradle Library

Remember that a library is just a subdirectory within the `Base Directory` configured as part of the Library Source in Jenkins. So to create the `gradle` library we should create a `gradle` directory under the `libraries` directory.

Then, to implement the `build()` step for the pipeline, create a `build.groovy` file within the newly created `gradle` subdirectory.

When you have created the new `gradle` directory and `build.groovy` file, your repository file structure will now be:

``` text
.
└── libraries
    ├── gradle
    │   └── steps
    │       └── build.groovy
    ├── maven
    │   └── steps
    │       └── build.groovy
    └── sonarqube
        └── steps
            └── static_code_analysis.groovy
```

The contents of `build.groovy` should be:

``` groovy title="./libraries/gradle/steps/build.groovy"
void call() {
    stage("Gradle: Build") {
        println "build from the gradle library"
    }
}
```

!!! important
    Please make sure you've pushed this change to the main/master branch of your library repository.

## Swap from Maven to Gradle

Now that we have a modifiable implementation for the `build()` step of the pipeline template, switching from Maven to Gradle is as easy as changing the libraries listed in the Pipeline Configuration.

Going back to the job configuration (click the `single-job` from Jenkins home page, then `Configure`), in the `Pipeline Configuration` text box, swap the `maven` line to `gradle` and click `Save`.

The Pipeline Configuration should now be:

``` groovy
libraries {
    gradle
    sonarqube
}
```

## Run the Pipeline

Follow the same steps as before to run the job again and checkout the build logs (`Console Output` after clicking the build number):

``` text
Started by user admin
[JTE] Pipeline Configuration Modifications (show)
[JTE] Obtained Pipeline Template from job configuration
[JTE] Loading Library gradle (show)
[JTE] Loading Library sonarqube (show)
[JTE] Template Primitives are overwriting Jenkins steps with the following names: (show)
[Pipeline] Start of Pipeline
[JTE][Step - gradle/build.call()]
[Pipeline] stage
[Pipeline] { (Gradle: Build)
[Pipeline] echo
build from the gradle library
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

Congrats! You've demonstrated the value of being able to quickly substitute one library for another to fulfill the same step in the Pipeline Template.
