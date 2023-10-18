# Create a Pipeline Job

Before we get started, we'll need to create a Pipeline Job in Jenkins that we can play around with.

Feel free to reuse the Pipeline Job created during JTE: The Basics or follow the [same instructions](../jte-the-basics/2-pipeline-job.md) to create a new job for this lab.

When you're finished, you should have:

*1. A pipeline template (Jenkinsfile text box in your job) that reads:*

``` groovy
build()
static_code_analysis()
```

*2. A Pipeline Configuration in your job of:*

``` groovy
libraries {
    maven
    sonarqube
}
```

!!! note
    If you're reusing the same Pipeline Job, your Pipeline Configuration may specify the `gradle` instead of the `maven` library. Either will do for the purposes of this lab, but switch it back to `maven` if it's different.

This Pipeline Job is going to be the playground where we learn about the different Pipeline Primitives for the rest of this lab.
