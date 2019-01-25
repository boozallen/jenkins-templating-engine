.. _Governance Model: 

----------------
Governance Model
----------------

One of the challenges associated with having a Jenkinsfile in every application 
source code repository is having governance within an organization over how software
gets developed. 

Each team may be doing something slightly differently and developers have access to 
bypass any required security gates by manipulating the Jenkinsfile.  

With the Jenkins Templating Engine, you're able to have a much clearer picture of 
the software delivery processes within an organization by consolidating your pipeline 
into a set of reusable templates managed within one or multiple pipeline configuration 
repositories. 

With JTE, you can configure a governance structure matching your organization's 
hierarchy through Governance Tiers. 

***************
Governance Tier 
***************

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

^^^^^^^^^^^^^^^^^^^^^^^
Hierarchical Governance
^^^^^^^^^^^^^^^^^^^^^^^
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

******************************
Configuration File Inheritance
******************************

When creating Governance Tiers to define your governance hierarchy, you can create configuration files to 
consolidate common configurations while tuning which aspects can be modified or overwritten by suborganizations
in the hierarchy. 

JTE provides some `common configurations <https://raw.githubusercontent.com/boozallen/jenkins-templating-engine/master/src/main/resources/org/boozallen/plugins/jte/config/pipeline_config.groovy>`_ 
by default.  The first configuration file found in the hierarchy will automatically override keys in common between 
the default configuration and the configuration file being loaded. 

After the first configuration file, each subsequent file will only be able to **add new root configuration keys** and 
any configuration that overlaps with the existing configuration will be ignored **unless** the parent configuration 
has explicitly allowed a configuration block to be modified via the ``merge`` and ``override`` keys.

Setting ``merge = true`` in a configuration block will allow the next configuration file in the hierarchy to append 
configuration keys to the block. 

Setting ``override = true`` in a configuration block will allow the next configuration file to replace the existing
configuration block. 

You can see some examples of these keywords in action :ref:`here <Conditional Inheritance Examples>`. 

Each configuration file in the Governance Tier hierarchy will be combined according to these rules until there 
is a resultant aggregated pipeline configuration. It is this aggregated pipeline configuration that gets used 
to populate the selected pipeline template for execution. 

***************************
Pipeline Template Selection
***************************

Pipeline Templates can either come from the application's source code repository, from the default 
template in a governance tier, or from a named template in a governance tier.  

^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
1. Application Source Code Repository
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

By default, if there is a Jenkinsfile within the application's source code repository it will be used 
as the pipeline template. 

Governance Tier's can choose to disable this flexibility to enforce the use of consolidated templates by 
setting ``allow_scm_jenkinsfile = false`` at the root of their configuration file. 

^^^^^^^^^^^^^^^^^^^^^^^^^^
2. Named Pipeline Template 
^^^^^^^^^^^^^^^^^^^^^^^^^^
Within a Governance Tier, you can create a ``pipeline_templates`` directory alongside the ``pipeline_config.groovy``
configuration file. Within this directory you can have as many different templates as you'd like and they will be
referenceable via their filename from the configuration file. 

To select a named pipeline template, you can specify ``pipeline_template = <template name>`` at the root 
of your configuration file. 

If a named pipeline template is specified, JTE will recursively search the ``pipeline_templates`` directories 
of each Governance Tier going up the hierarchy from most specific all the way to the Global Governance Tier 
defined in ``Manage Jenkins > Configure System``. 

.. important::

    If a named pipeline template is specified and the template is not found in any Governance Tier, JTE 
    will throw a Template Not Found Exception and fail the build. 

^^^^^^^^^^^^^^^^^^^^^^^^^^^^
3. Default Pipeline Template 
^^^^^^^^^^^^^^^^^^^^^^^^^^^^
If there is not an application repository ``Jenkinsfile`` or one is not permitted, and a named pipeline template 
has not been specified, JTE will look for the first Governance Tier Default Pipeline Template in ascending order. 

To create a Default Pipeline Template, simply create a ``Jenkinsfile`` alongside the ``pipeline_config.groovy`` 
configuration file in the Governance Tier. 

.. important:: 

    If no Default Pipeline Template can be found, then no pipeline template has been defined and JTE 
    will fail the build. 

*****************
Library Selection 
*****************
Each Governance Tier can have a list of Library Sources defined, with each Library Source containing 1 or 
more JTE pipeline libraries. 

When a library is specified in a configuration file, JTE will start with the most specific Governance Tier 
and search each Library Source from first to last looking for the library.  If none of the Library Sources 
in the first Governance Tier has the library, JTE will then look in the parent Governance Tier.  This pattern 
is followed until all Library Sources have been inspected for the library. 

.. important:: 

    If a library is specified and not present in any of the Governance Tier's Library Sources, JTE will throw 
    a Library Not Found Exception and fail the build. 



.. toctree::
   :maxdepth: 4
   :glob:
   :titlesonly:

   *