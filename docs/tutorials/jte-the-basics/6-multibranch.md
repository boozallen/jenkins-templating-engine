# Apply to a GitHub Repository

So far we've learned:

* What a Pipeline Template is (the business logic of your pipeline).
* How to create some mock Pipeline Libraries (just Groovy files implementing a `call()` method inside a directory in a repository).
* What the Pipeline Configuration does (defines libraries to implement the template so it actually does things).
* How to use the same pipeline template with two different tech stacks by modifying the Pipeline Configuration.

Next, we're going to learn how to apply a Pipeline Template to an entire GitHub repository.

This is a more realistic scenario and it has the added benefit of taking the Pipeline Template and Pipeline Configuration file out of the Jenkins UI and storing them in a Pipeline Configuration repository.

## Move the Pipeline Template to a Repository

When creating libraries, we created a GitHub JTE library repository and stored the libraries in a subdirectory called `libraries`. In this example, we can create a new subdirectory at the root of the repository called `pipeline-configuration`.

!!! note
    The actual names of the `libraries` and `pipeline-configuration` subdirectories don't matter and are configurable, the convention is to keep them named like this, however.

Within this `pipeline-configuration` directory create a file called `Jenkinsfile` and populate it with the same contents as the `Pipeline Template` text box in the Jenkins UI.

``` groovy title="./pipeline-configuration/Jenkinsfile"
build()
static_code_analysis()
```

!!! important
    The `Jenkinsfile` is the *default Pipeline Template* that will be used. It is possible to define more than one Pipeline Template and let application teams select which template applies to them. More on that later, or [read the doc on template selection](../../concepts/pipeline-governance/pipeline-template-selection.md).

## Move the Pipeline Configuration to a Repository

In the same `pipeline-configuration` directory, create a file called `pipeline_config.groovy`.

!!! important
    When the Pipeline Configuration is stored in a file in a source code repository, it will always be called `pipeline_config.groovy`.

Populate this file with the same contents as the `Pipeline Configuration` text box in the Jenkins UI for the `single-job` you created and updated earlier:

``` groovy title="./pipeline-configuration/pipeline_config.groovy"
libraries {
    gradle
    sonarqube
}
```

The file structure in your GitHub repository should now look like this:

``` text
.
├── libraries
│   ├── gradle
│   │   └── steps
│   │       └── build.groovy
│   ├── maven
│   │   └── steps
│   │       └── build.groovy
│   └── sonarqube
│       └── steps
│           └── static_code_analysis.groovy
└── pipeline-configuration
    ├── Jenkinsfile
    └── pipeline_config.groovy
```

### Create the Global Governance Tier

Now that we have our template and Pipeline Configuration externalized into a source code repository, we have to tell Jenkins where to find it.

From the Jenkins home page:

* In the left-hand navigation menu click `Manage Jenkins`.
* Click `Configure System`.
* Scroll down to the `Jenkins Templating Engine` configuration section.
* Under `Pipeline Configuration` select `From SCM`.
* Select `Git` for the `Source Location` drop down menu.
* Under `Repository URL` type the *https* URL of the GitHub repository containing the libraries, template, and configuration file.
* In the `Credentials` drop down menu, select the `github` credential created during the prerequisites.
* Under `Branches to build` you may have to specify `*/main`, `*/master` or something else.
* Type `pipeline-configuration` in the `Configuration Base Directory` text box.
* Click `Save`.

![Global Governance Tier](./images/global_governance_tier.gif)

!!! note
    You just configured your first *Governance Tier*!

    Governance Tiers are the combination of:

    * A Pipeline Configuration repository specifying where the Pipeline Configuration file and Pipeline Templates can be found.
    * A set of library sources.

    When done in `Manage Jenkins > Configure System` it's called the Global Governance Tier and applies to every job on the Jenkins instance.

    Governance Tiers can also be configured for every Folder in Jenkins. When configured, they apply to every Job within that Folder. For more on Jenkins Folders: https://plugins.jenkins.io/cloudbees-folder/.

    Through Governance Tiers, you can create a governance hierarchy that matches your organizational hierarchy just by how you organize jobs within Jenkins.

## Create an Application Repository

We're going to apply the Pipeline Template and configuration file to every branch in a GitHub repository.

* Create a GitHub Repository that will serve as our mock application repository named `jte-the-basics-app-gradle`.
* Initialize the Repository with a README file.
* Modify the README in order to create a branch called *test*. Push the new branch. Consult Git documentation on how to create and push a branch if you don't know how, or follow the older guide GIF below:

![Creating a Gradle repo](./images/create_gradle_repo.gif)

## Create a Multibranch Project

Now that we have a GitHub repository representing our application, we can create a *Multibranch Project* in Jenkins.

!!! important
    Multibranch Projects are Folders in Jenkins that automatically create pipeline jobs for every branch and Pull Request in the source code repository they represent.

    Through JTE, we can configure each branch and Pull Request to use the *same* Pipeline Template. This _removes_ the need for a per-repository Jenkinsfile.

* From the Jenkins home page, select `New Item` in the left-hand navigation menu.
* In the `Enter an item name` text box, type `gradle-app`.
* Select `Multibranch Pipeline` as the job type.
* Click `OK` to create the job.
* Under `Branch Sources > Add Source` select `GitHub`.
* Select your `github` credential under the `Credentials` drop down menu.
* Enter the *https* repository URL of `jte-the-basics-app-gradle` under `Repository HTTPS URL`.
* Under `Build Configuration` select `Jenkins Templating Engine` from the `Mode` drop-down menu.
* Click `Save`.

When the job is created, you will be redirected to a page showing the logs for scanning the repository. In the breadcrumbs at the top of the page, you can select `gradle-app` to see the branch overview.

In this overview, you'll see two jobs in progress once the repository scan has repeated: a job for the `main` branch and a job for the `test` branch.

When these jobs complete, clicking them will show that each branch executed the Pipeline Template with the same configuration.

![Multibranch Configuration](./images/multibranch.gif)
