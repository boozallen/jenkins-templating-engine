Job DSL Examples for Utilizing the Jenkins Templating Engine
============================================================

What are Job DSL Scripts?
*************************
Job DSL scripts can be used to create and configure Jenkins jobs programatically. These scripts are used through the `Job DSL Plugin
<https://github.com/jenkinsci/job-dsl-plugin>`_


Github Organization Job DSL Script (github_organization.groovy)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
This script will create and configure a Github Organization job in Jenkins by doing the following:

- Under "Projects"

  - Specifying the Github Organization
  - Which Github Credentials to use
  - Behaviors
  - Specifying the Jenkins Templating Engine as a project recognizer

- Under "Solutions Delivery Platform"

  - Pointing the Pipeline Configuration Source to the correct Github Repository
  - Configuring the base directory of the configuration
  - Pointing the Library Source to the correct Github Repository

In order for the script to be configured properly for your own use, the following environment variables need to be available for
the script to use:

- TENANT_GITHUB_ORG
- TENANT_GITHUB_CREDS_ID
- TENANT_GITHUB_API_URL
- TENANT_GITHUB_CONFIG_URL
- TENANT_GITHUB_LIBRARY_URL
- BASE_DIRECTORY

It is up to you to determine how these environment variables are set.
