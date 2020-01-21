.. _hierarchical-goverance:

-----------------------
Hierarchical Governance
-----------------------
In large organizations, there may be company wide software delivery configurations with departmental profiles
for tools being used or the specific standards required and so on all the way down to specific application configurations
such as a URL to test against, etc, until your governance hierarchy starts to look something like:

.. image:: ../../images/governance/hierarchy.png
   :scale: 50%
   :align: center

A Jenkins wide Governance Tier can be configured in ``Manage Jenkins > Configure System`` under the ``Jenkins Templating Engine``
section header.  This Governance Tier will apply to all jobs in Jenkins.

.. image:: ../../images/installation/configure_system_validation.png

A Governance Tier can then be configured on any folder within Jenkins, including multibranch jobs or GitHub Organization Jobs.

.. important::

    You can create a governance hierarchy in JTE matching your organizational hierarchy by organizing your jobs
    accordingly within Jenkins and configuring a Governance Tier globally and on the corresponding Folders within
    Jenkins.
