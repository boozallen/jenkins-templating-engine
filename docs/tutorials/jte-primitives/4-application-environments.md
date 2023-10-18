# Application Environments

## What is an Application Environment?

Performing an automated deployment is a ubiquitous step in continuous delivery pipelines.

Libraries can implement steps to perform deployments and, in doing so, need a mechanism to tell the deployment step which Application Environment is being deployed to.

The Application Environment acts to encapsulate the contextual information that identifies and differentiates an application's environments, such as dev, test, and production.

In general, Library Steps shouldn't accept input parameters. Deployment steps are one of the few exceptions to this rule.

!!! note
    View the Application Environments documentation [here](../../concepts/pipeline-primitives/application-environments.md).

### A Word on Input Parameters to Library Steps

Understanding _why_, in general, Library Steps shouldn't accept input parameters is fundamental to understanding the goals of the Jenkins Templating Engine.

Let's say we had some teams in an organization leveraging SonarQube for static code analysis and others using Fortify. If the `static_code_analysis` step implemented by the `sonarqube` library took input parameters - it would then require that _every_ library that implements `static_code_analysis` take the same input parameters, lest you break the interchangeability of libraries to use the same template.

This would mean that the `fortify` library's implementation would have to take the _same_ input parameters as the `sonarqube`'s implementation - otherwise switching between the two would break the code.

Being able to swap implementations of steps in and out through different libraries is the primary mechanism through which JTE supports creating reusable, tool-agnostic Pipeline Templates.

It's understandable that Library Steps require some externalized configuration to avoid hard-coding dependencies like server locations, thresholds for failure, etc. This is why library configuration is done through the Pipeline Configuration file and passed directly to the steps through the JTE plugin as opposed to directly via input parameters.

Deployment steps are different. It is safe to assume that every step that performs a deployment needs to know some information about the environment.

## Define and Use an Application Environment

### Create a Deployment Library

Let's actually create a mock deployment library to demonstrate the utility of Application Environments.

In the same library repository used during JTE: The Basics, add a new library called `ansible` with a step in a directory called `steps`. Call the file `deploy_to.groovy`.

!!! note
    Like Gradle, Maven, and SonarQube, you don't need to know much about Ansible to complete this lab, but there is a wealth of information about it available online if you are interested and it is a tool commonly used by DevSecOps teams.

    Remember that libraries are just subdirectories within a source code repository and that steps are just Groovy files in those subdirectories that typically implement a `call` method.

For the sake of our pretend `ansible` library, let's assume that it needs to know a list of IP addresses relevant to the environment it's deploying to.

``` groovy title="./libraries/ansible/steps/deploy_to.groovy"
void call(app_env) {
    stage("Deploy to: ${app_env.long_name}") {
        println 'Performing a deployment through Ansible..'
        app_env.ip_addresses.each { ip ->
            println "Deploying to ${ip}"
        }
    }
}
```

This step will announce it's performing an Ansible deployment and then iterate over the IP addresses provided for the Application Environment and print out the target server.

!!! note
    The file structure within your `libraries` directory should now be:

    ``` text
    .
    └── libraries
        ├── ansible
        │   └── steps
        │       └── deploy_to.groovy
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

!!! important
    Make sure you've pushed these changes to your global library repo's `main/master` branch before proceeding.

### Define the Application Environments in the Pipeline Configuration

We now need to load the `ansible` library and define the Application Environments.

In your `single-job`, go to `Configure` and change the _Pipeline Configuration_ to:

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
```

!!! important
    Application Environments are defined in the `application_environments` block within the Pipeline Configuration.

    Each key defined in this block will represent an Application Environment and a variable will be made available in the pipeline template based upon this name.

    The only two keys that Application Environments explicitly define are `short_name` and `long_name`. These values both default to the key defining the Application Environment (i.e. `long_name` would have been `prod` and not `Production` if we had not declared it) in the Pipeline Configuration, but can be overridden.

### Update the Pipeline Template

Now that we have a library that performs a deployment step and Application Environments defined in the Pipeline Configuration, let's update the Pipeline Template to pull it all together.

Update the _Jenkinsfile_ (your default pipeline template in your `single-job`) to:

``` groovy title="Jenkinsfile (in your single-job)"
continuous_integration() 
deploy_to dev 
deploy_to prod 
```

!!! note
    These variables `dev` and `prod` come directly from the Applications Environments we just defined in the Pipeline Configuration.

### Run the Pipeline

Save your configuration. From the Pipeline job's main page, click `Build Now` in the left-hand navigation menu.

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

Notice the output was different for the deployment to the `dev` environment vs the deployment to `prod`. This is because different values were stored in each Application Environment and the library was able to use this contextual information and respond accordingly.
