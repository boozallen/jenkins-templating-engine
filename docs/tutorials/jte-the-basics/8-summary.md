# Summary

We learned a lot in this lab! Let's recap some of what we learned:

## GitHub Credentials in Jenkins

We learned how to create a Personal Access Token and store it in the Jenkins credential store.

While credentials aren't strictly needed for public repositories, GitHub will rate limit your Jenkins instance's API requests, which can dramatically slow down the pipeline and cause it to fail.

## Different Types of Jenkins Jobs

We learned about the three kinds of Jenkins Jobs most commonly used when working with the Jenkins Templating Engine:

| Job Type | Description |
| -------- | ------------|
| Pipeline Job | Best suited for one-off tasks or debugging pipelines developed with JTE. |
| Multibranch Projects | Represent an entire GitHub repository and create a job for every branch and Pull Request. |
| GitHub Organization Folder | Represent an entire GitHub Organization. Can be filtered to restrict which repositories are automatically represented in Jenkins. |

## What makes up a pipeline in JTE?

We learned that:

* **Pipeline Templates** can call **steps** contributed by **libraries**.
* Pipeline Templates are responsible for the **business logic** of your pipeline.
* Libraries are responsible for the **technical implementation** of your pipeline.
* Pipeline Configuration Files **implement** a template by specifying (among other things) what libraries to load.
* When stored in a repo, Pipeline Configuration files are named ``pipeline_config.groovy`` and are located at the root of the repository.

## What is a Governance Tier?

We learned that:

* **Governance Tiers** are a way to **externalize configuration** into source code repositories.
* A Governance Tier is made up of a Pipeline Configuration repository and a set of Library Sources.
* Pipeline Configuration repositories optionally contain a pipeline template and a Pipeline Configuration file.
* The Jenkinsfile is the **default pipeline template** in a Governance Tier.

## How can we reuse Pipeline Templates?

We learned that:

* Pipeline Templates can be applied to multiple repositories simultaneously through the GitHub Organization Job Type.
* Pipeline Configuration files can be aggregated between Governance Tiers and an application repository itself.
* There are rules around **conditional inheritance** when it comes to Pipeline Configuration aggregation.
