.. _`Jenkins Templating Engine`: 

Jenkins Templating Engine
=========================

The Jenkins Templating Engine (JTE) is a plugin developed by Booz Allen Hamilton focused
on pipeline templating and governance. In practice, this allows you to consolidate 
your team's pipelines into shareable workflows that define the business logic of 
your software delivery processes while allowing for optimal pipeline code reuse by
pulling out tool specific implementations into library modules. 

How Does It Work? 
*****************

In JTE there are templates and configuration files which provide the necessary information to implement
the functionality defined in the template. 

JTE allows you to separate the business logic of your pipeline from the technical implementation by 
creating pipeline templates and abstracting the implementation of pipeline actions out into pipeline
libraries. The general idea is that the development teams for which a pipeline is being 
built will follow the same workflow regardless of technical stack. 

An example workflow might be: 

.. image:: docs/images/sample_template.png
   :scale: 50%
   :align: center

This workflow could be implemented by creating a template, as follows: 

.. code:: 

    unit_test()
    static_code_analysis()
    build()
    deploy_to dev 

This template could be reused by with multiple tech stacks by creating :ref:`libraries<Library Development>` which
implement the pipeline **steps** ``unit_test``, ``static_code_analysis``, ``build``, and ``deploy_to``.  Once these libraries
are created, a configuration file is created to specify which libraries to load. 

.. important::
   The take away here:  Pipeline templates define generic steps that should be executed.  These implementation of
   these steps are abstracted out into libraries.  A configuration file is used to determine which libraries a particular
   application should use.  A common pipeline template, defined within **or** externally, to an application source code repository
   is can be used across applications by loading different libraries which implement the steps defined
   in the pipeline template. 


Why Templating?
***************

Typically, pipelines are defined on a per application basis via a ``Jenkinsfile`` in the 
source code repository.  Often, common code can be pulled out into Shared Libraries to reduce 
code duplication but a few problems remain.  Organization's application portfolios often have
a diverse technical stack, each requiring their own pipeline-as-code integrations.  It becomes
increasingly complex to manage these different implementations across an organization while 
standardizing on the software delivery processes required. Additionally, for organizations with
strict governance requirements around types of security testing and approvals, having a Jenkinsfile
within the source code repository can be problematic as it potentially allows developers to bypass
these mandatory steps. 

.. image:: docs/images/value.png
   :scale: 50%
   :align: center

Organizational Governance
^^^^^^^^^^^^^^^^^^^^^^^^^


Optimize Code Reuse
^^^^^^^^^^^^^^^^^^^


Code Reuse
^^^^^^^^^^



.. toctree::
   :hidden:
   :maxdepth: 2
   :titlesonly:

   pages/installation
   pages/Templating/index
   pages/Governance/index
   pages/Library_Development/index
   pages/Plugin_Development/index
   pages/Custom_Primitives/index
