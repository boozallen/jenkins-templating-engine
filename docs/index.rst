.. _`Jenkins Templating Engine`: 

=========================
Jenkins Templating Engine
=========================

The `Jenkins Templating Engine (JTE) <https://plugins.jenkins.io/templating-engine>`_ is a plugin developed by Booz Allen Hamilton enabling
pipeline templating and governance. JTE  allows you to consolidate pipelines into shareable workflows that define the business logic of 
your software delivery processes while allowing for optimal pipeline code reuse by pulling out tool specific implementations into library modules. 

How Does It Work? 
*****************

The Jenkins Templating Engine works off the idea of pipeline templates that define generically 
what should happen (and when) and accompanying configuration files that populate the template 
with implementation details. 

Why Templating?
***************

Pipelines are typically defined on a per application basis via a ``Jenkinsfile`` in the 
source code repository.  Often, common code can be pulled out into Shared Libraries to reduce 
code duplication but a few problems remain.  

Organization's application portfolios often have a diverse technology stack, each requiring their
own pipeline-as-code integrations.  It becomes increasingly complex to manage these different 
implementations across an organization while standardizing on the required software delivery processes.
For organizations with strict governance requirements having a Jenkinsfile within the source code repository 
can allow developers to bypass these mandatory requirements. 

.. figure:: images/home/value.png
   :scale: 50%
   :align: center

   Why Templating? 

Organizational Governance
^^^^^^^^^^^^^^^^^^^^^^^^^

The real power of JTE comes from creating shareable workflows and using them across teams.  
JTE allows you to create a centralized hierarchical governance for your pipeline configurations  
that aligns to your organizational structure.
With governance tiers, organizations can accelerate enterprise adoption by creating inheritable pipelines that
share common configurations.  

Optimize Code Reuse
^^^^^^^^^^^^^^^^^^^

JTE allows you to share pipeline code effectively across multiple teams through the use of 
common pipeline libraries. 


Simplify Pipeline Maintainability
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

When supporting pipelines for multiple applications it becomes more challenging to maintain 
the associated complexity.  Multiple tech stacks and tool integrations can lead to largely 
similar Jenkinsfiles with distinct differences around how specific parts of the pipeline function.

JTE embraces the traditional software development practices of modularity and separation of concerns to 
create a more organized code base for your pipelines. 

.. important::
    At Booz Allen, we created the Jenkins Templating Engine to support our Solutions Delivery Platform; a 
    DevSecOps platform accelerating adoption of modern software development best practices. 

.. toctree::
   :hidden:
   :maxdepth: 2
   :titlesonly:

   pages/Templating/index
   pages/Governance/index
   pages/Library_Development/index
   