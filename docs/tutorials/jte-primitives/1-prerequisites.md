# Prerequisites

## Jenkins Instance

A Jenkins instance will be required for this lab. If you don't have one available to you, we recommend going through the [Local Development Learning Lab](../local-development/index.md) to deploy a local Jenkins instance through Docker.

## JTE: The Basics

This lab continues to build upon our knowledge of the Jenkins Templating Engine so first completing the [Basics Learning Lab](../jte-the-basics/index.md) would be very helpful.

In this lab we will assume you're using the same Pipeline Configuration repository used during The Basics lab and that it is already configured as a Library Source in the Global Governance Tier.

### Remove the Global Governance Tier's Pipeline Configuration

For the purposes of this lab, we will only be using the Pipeline Job type. In JTE: The Basics, we created a Pipeline Configuration to the Global Governance Tier that applies to every job on the Jenkins instance.

If this is still configured, remove it.

* From the Jenkins homepage, Click `Manage Jenkins` in the left-hand navigation menu.
* Under `System Configuration` click `Configure System`.
* Scroll down to the `Jenkins Templating Engine` configuration section.
* Remove any text in the `Configuration Base Directory` text box.
* Under `Pipeline Configuration` drop-down menu, select `None`.
* Click `Save`.

!!! note
    There should still be a global Library Source configured. Leave it as-is.
