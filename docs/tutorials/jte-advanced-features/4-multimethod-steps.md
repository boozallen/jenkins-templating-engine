# Multi-Method Steps

While learning about Pipeline Lifecycle Hooks, we created a step that:

* Implemented multiple methods.
* Implemented a step without a `call` method.

In this section, we're going to dive into multi-method steps in a little more detail.

!!! important
    Have you ever wondered why Library Steps create a method named `call`? This is because, in the Groovy scripting language, `something()` gets translated to `something.call()`.

If we understand this concept, then it would make sense that we could define other methods within our steps and invoke them by their full name.

## When to use Multi-Method Steps

The most common use case for defining multiple methods inside one step file is when you're creating some utility functionality.

To demonstrate this, let's create a mock `git` utility that can `add`, `commit`, and `push`.

## Create the Git Library

In the same Pipeline Configuration Repository we used for JTE: The Basics, create a `git` library.

Because we're creating a git utility, add a file called `git.groovy` in `libraries/git` in your Library Sources repo with the contents:

``` groovy title="./libraries/git/git.groovy"
/*
    takes an arraylist of files to pass to git add 
*/
void add(ArrayList files) {
    println "git add ${files.join(" ")}"
}

/*
    takes a string commit message to pass to git commit 
*/
void commit(String message) {
    println "git commit -m ${message}" 
}

/*
    performs the git push
*/
void push() {
    println "git push" 
}
```

In this example, we're creating a step that serves as a utility wrapper. These are typically *not* invoked directly by Pipeline Templates but rather consumed by other steps. That is why it is okay, in this case, to accept input parameters for these methods.

We will be invoking this functionality directly from the Pipeline Template to demonstrate its usefulness.

!!! important
    The file structure for your Pipeline Configuration libraries directory should now be:

    ``` text
    .
    ├── libraries
        ├── ansible
        │   └── steps
        │       └── deploy_to.groovy
        ├── git
        │   └── steps
        │       └── git.groovy
        ├── gradle
        │   └── steps
        │       └── build.groovy
        ├── maven
        │   └── steps
        │       └── build.groovy
        ├── sonarqube
        │   └── steps
        │       └── static_code_analysis.groovy
        └── splunk
            └── steps
                ├── splunk_pipeline_end.groovy
                ├── splunk_pipeline_start.groovy
                └── splunk_step_watcher.groovy
    ```

## Update the Pipeline Configuration

Update the Pipeline Configuration in your `single-job` to load the `git` library.

The `libraries` portion of the Pipeline Configuration should now be:

``` groovy title="Pipeline Configuration"
libraries {
    maven
    sonarqube
    ansible
    splunk{
        afterSteps = [ "static_code_analysis", "unit_test" ]
    }
    git
}
```

## Use the New Git Utility

Prepend to the existing Jenkinsfile/Template for `single-job` (before `continuous_integration()`):

``` groovy title="Jenkinsfile"
git.add(["a", "b", "c"])
git.commit "my commit message" 
git.push()
```

!!! important
    When invoking a non-call method defined within a step, you do so by `<step_name>.<method_name>(<arguments>)`.

## Run the Pipeline

Run the `single-job` again and you will see logs similar to:

``` text
Started by user admin
[JTE] Pipeline Configuration Modifications (show)
[JTE] Obtained Pipeline Template from job configuration
[JTE] Loading Library maven (show)
[JTE] Loading Library sonarqube (show)
[JTE] Loading Library ansible (show)
[JTE] Loading Library splunk (show)
[JTE] Loading Library git (show)
[JTE] Creating step unit_test from the default step implementation.
[JTE] Template Primitives are overwriting Jenkins steps with the following names: (show)
[Pipeline] Start of Pipeline
[JTE][@Init - splunk/splunk_pipeline_start.call]
[Pipeline] echo
Splunk: beginning of the pipeline!
[JTE][@BeforeStep - splunk/splunk_step_watcher.before]
[Pipeline] echo
Splunk: running before the git library's git step
[JTE][Step - git/git.add(ArrayList)]
[Pipeline] echo
git add a b c
[JTE][@BeforeStep - splunk/splunk_step_watcher.before]
[Pipeline] echo
Splunk: running before the git library's git step
[JTE][Step - git/git.commit(String)]
[Pipeline] echo
git commit -m my commit message
[JTE][@BeforeStep - splunk/splunk_step_watcher.before]
[Pipeline] echo
Splunk: running before the git library's git step
[JTE][Step - git/git.push()]
[Pipeline] echo
git push
[JTE][Stage - continuous_integration]
[JTE][@BeforeStep - splunk/splunk_step_watcher.before]
[Pipeline] echo
Splunk: running before the null library's unit_test step
[JTE][Step - null/unit_test.call()]
[Pipeline] stage
[Pipeline] { (Unit Test)
[Pipeline] node
Running on Jenkins in /var/jenkins_home/workspace/single-job
[Pipeline] {
[Pipeline] isUnix
[Pipeline] withEnv
[Pipeline] {
[Pipeline] sh
+ docker inspect -f . maven
.
[Pipeline] }
[Pipeline] // withEnv
[Pipeline] withDockerContainer
Jenkins seems to be running inside container f8a61ccd04d1fd2e436dc0ccbc3f5ad59cd95b6a736420fb3ef808b9da5b7dec
$ docker run -t -d -u 0:0 -w /var/jenkins_home/workspace/single-job --volumes-from f8a61ccd04d1fd2e436dc0ccbc3f5ad59cd95b6a736420fb3ef808b9da5b7dec -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** -e ******** maven cat
$ docker top 8a28e4d78d0bb343f652e5420c00767b50a4e87f12f075f420ecfd5ce73a32d3 -eo pid,comm
[Pipeline] {
[Pipeline] unstash
[Pipeline] sh
+ mvn -v
Apache Maven 3.8.7 (b89d5959fcde851dcb1c8946a785a163f14e1e29)
Maven home: /usr/share/maven
Java version: 17.0.5, vendor: Eclipse Adoptium, runtime: /opt/java/openjdk
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "5.10.104-linuxkit", arch: "amd64", family: "unix"
[Pipeline] }
$ docker stop --time=1 8a28e4d78d0bb343f652e5420c00767b50a4e87f12f075f420ecfd5ce73a32d3
$ docker rm -f --volumes 8a28e4d78d0bb343f652e5420c00767b50a4e87f12f075f420ecfd5ce73a32d3
[Pipeline] // withDockerContainer
[Pipeline] }
[Pipeline] // node
[Pipeline] }
[Pipeline] // stage
[JTE][@AfterStep - splunk/splunk_step_watcher.after]
[Pipeline] echo
Splunk: running after the null library's unit_test step
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
[JTE][@BeforeStep - splunk/splunk_step_watcher.before]
[Pipeline] echo
Splunk: running before the sonarqube library's static_code_analysis step
[JTE][Step - sonarqube/static_code_analysis.call()]
[Pipeline] stage
[Pipeline] { (SonarQube: Static Code Analysis)
[Pipeline] echo
static code analysis from the sonarqube library
[Pipeline] }
[Pipeline] // stage
[JTE][@AfterStep - splunk/splunk_step_watcher.after]
[Pipeline] echo
Splunk: running after the sonarqube library's static_code_analysis step
[JTE][@BeforeStep - splunk/splunk_step_watcher.before]
[Pipeline] echo
Splunk: running before the ansible library's deploy_to step
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
[JTE][@BeforeStep - splunk/splunk_step_watcher.before]
[Pipeline] echo
Splunk: running before the ansible library's deploy_to step
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
[JTE][@CleanUp - splunk/splunk_pipeline_end.call]
[Pipeline] echo
Splunk: end of the pipeline!
[Pipeline] End of Pipeline
Finished: SUCCESS
```

You learned in this lesson that we can call steps in a very programmatic way from our template, this opens the door to new and creative ways to create a governed pipeline that allows flexibility for different step implementations.
