# Keywords

## What is a Keyword?

Keywords allow you to define variables in the Pipeline Configuration that can be referenced in your Pipeline Template. This allows you to keep templates as readable as possible by externalizing the definition of complex variables out of the template.

The most common use case so far for Keywords is storing regular expressions that map to common branch names in the [GitFlow Branching Strategy](https://datasift.github.io/gitflow/IntroducingGitFlow.html) to be used in evaluating whether or not aspects of the pipeline should execute based on a matching branch.

In this example, we'll use a Keyword as a feature flag externalized from the Pipeline Template to conditionally determine if a manual gate is required before the deployment to Production.

!!! note
    View the Keyword documentation [here](../../concepts/pipeline-primitives/keywords.md).

## Define and Use a Keyword

### Define the Keyword in the Pipeline Configuration

For your `single-job` configuration, update the *Pipeline Configuration* to:

``` groovy title="Pipeline Configuration"
libraries {
    maven
    sonarqube
    ansible
}

stages {
    continuous_integration {
        build
        static_code_analysis
    }
}

application_environments {
    dev {
        ip_addresses = [ "0.0.0.1", "0.0.0.2" ]
    }
    prod {
        long_name = "Production" 
        ip_addresses = [ "0.0.1.1", "0.0.1.2", "0.0.1.3", "0.0.1.4" ]
    }
}

keywords {
    requiresApproval = true 
}
```

!!! important
    All Keywords will be defined in the `keywords` block of the Pipeline Configuration.

    Traditional variable setting syntax of `a = b` is used to define Keywords.

### Update the Pipeline Template

With the `continuous_integration` stage defined, we can update the pipeline template to make use of it.

Update the *Pipeline Template* (`Jenkinsfile` in your single-job) to:

``` groovy title="Jenkinsfile"
continuous_integration() 
deploy_to dev 

if(requiresApproval) {
    timeout(time: 5, unit: 'MINUTES') {
        input 'Approve the deployment?'
    }
}

deploy_to prod
```

!!! important
    This is an example to demonstrate the use of a Keyword in a Pipeline Template and *not* how we would recommend you enable this sort of gate in a production pipeline.

    We would recommend that, in practice, the deployment library inherits this manual gate approval so that `requiresApproval` could be set on each Application Environment individually.

### Run the Pipeline

From the Pipeline Job's main page, click `Build Now` in the left-hand navigation menu.

When viewing the build logs (click Build number, then `Console Output`), you should see output similar to:

``` text
Started by user admin
[JTE] Pipeline Configuration Modifications (show)
[JTE] Obtained Pipeline Template from job configuration
[JTE] Loading Library maven (show)
[JTE] Loading Library sonarqube (show)
[JTE] Loading Library ansible (show)
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
[JTE][Step - ansible/deploy_to.call(ApplicationEnvironment)]
[Pipeline] stage
[Pipeline] { (Deploy to: dev)
[Pipeline] echo
Performing a deployment through Ansible..
[Pipeline] echo
Deploying to 0.0.0.1
[Pipeline] echo
Deploying to 0.0.0.2
[Pipeline] }
[Pipeline] // stage
[Pipeline] timeout
Timeout set to expire in 5 min 0 sec
[Pipeline] {
[Pipeline] input
Approve the deployment?
Proceed or Abort
```

Click the `Proceed` link and the job should continue, showing `Approved by admin`:

``` text
Approved by admin
[Pipeline] }
[Pipeline] // timeout
[JTE][Step - ansible/deploy_to.call(ApplicationEnvironment)]
[Pipeline] stage
[Pipeline] { (Deploy to: Production)
[Pipeline] echo
Performing a deployment through Ansible..
[Pipeline] echo
Deploying to 0.0.1.1
[Pipeline] echo
Deploying to 0.0.1.2
[Pipeline] echo
Deploying to 0.0.1.3
[Pipeline] echo
Deploying to 0.0.1.4
[Pipeline] }
[Pipeline] // stage
[Pipeline] End of Pipeline
Finished: SUCCESS
```

!!! important
    When reading the build logs of a JTE job, you can identify the start of stages by looking for `[JTE] [Stage - *]` in the output.

    In this case, the log output was `[JTE] [Stage - continuous_integration]` indicating a Stage called `continuous_integration` is about to be executed.

!!! note
    The exercise of setting `requiresApproval = false` and seeing the difference is left to the reader.
