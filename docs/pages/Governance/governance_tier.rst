.. _governance-tier:

---------------
Governance Tier
---------------

Templates define the business logic of the pipeline and a configuration file is used
to implement the functionality of the template.

If you would like to leverage the templating capabilities of JTE without sharing workflows
between applications, you can place a ``Jenkinsfile`` template at the root of your application's
source code repository alongside the ``pipeline_config.groovy`` configuration file.

If you would like to consolidate templates or common configurations, you can put organizational
templates and common configurations into a **Governance Tier**.

.. image:: ../../images/governance/jte_governance_tier.png
   :scale: 50%
   :align: center

Governance Tiers consist of :ref:`Configuration Files <Configuration Files>`, :ref:`Library Sources <Pipeline Libraries>`,
and :ref:`Pipeline Templates <Pipeline Templating>`.
