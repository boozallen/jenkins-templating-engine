.. _GitHub Organization Job:

-----------------------
GitHub Organization Job
-----------------------

JTE can be used to apply a pipeline template to an entire GitHub Organization by creating a
**GitHub Organization** job in Jenkins.

To configure this, under the **Projects** section you will set the **Project Recognizer** as
``Jenkins Templating Engine`` (as shown below)

.. figure::../../images/Job_Configurations/github_org/project_recognizer.png
   :align: center

========================
Repository Configuration
========================

Note, the **GitHub Organization** project can configure a **Governance Tier** for specifying pipeline templates,
library sources, and applying a pipeline configuration to every branch of every repository in a GitHub Organization.

You can find these settings under the ``Jenkins Templating Engine`` section of the job configuration, as shown below:

.. figure:: ../../images/Job_Configurations/github_org/governance_tier.png
   :align: center

