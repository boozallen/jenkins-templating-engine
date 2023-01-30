# JTE: The Basics

The purpose of this lab is to introduce you to the Jenkins Templating Engine as a *framework* for building Jenkins pipelines in a way that allows you to share pipeline templates between teams.

By being able to build reusable, tool-agnostic pipeline templates and apply them to multiple applications simultaneously, you can then *remove* individual Jenkinsfiles from each source code repository.

!!! important
    In JTE, we talk a lot about the concept of governance. When we say governance, we mean that JTE allows you to *enforce* a common software delivery process by applying the *same* pipeline template to *multiple* repositories at the same time.

    The modularity that JTE promotes allows you to do this across teams _regardless_ of the specific tools that each application may be using.

## What You'll Learn

* Configure your first tool-agnostic pipeline template
* Create your first set of Pipeline Libraries to provide tool-specific implementations of steps
* Create your first Pipeline Configuration file that implements the template
* Learn the different types of jobs available in Jenkins and when to use them
* Learn how to take the Jenkinsfile (pipeline template) *out* of the repository
* Learn how to consolidate Pipeline Configurations and apply the same template across multiple repositories
