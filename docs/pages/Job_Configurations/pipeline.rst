.. _Pipeline Jobs: 

------------------------
Individual Pipeline Jobs
------------------------

The most straight forward way test out a pipeline template is to create a Pipeline job in Jenkins. 

===============================================================
Select the Jenkins Templating Engine Pipeline Definition Option
===============================================================

A ``Jenkins Templating Engine`` pipeline definition is available when the plugin is installed: 

.. figure:: ../../images/Job_Configurations/pipeline/flow_definition.png
   :align: center 
   

======================
Configure the Pipeline
======================

After selecting the ``Jenkins Templating Engine`` definition, you'll be able to configure your
**Pipeline Template** and **Pipeline Configuration**

.. figure:: ../../images/Job_Configurations/pipeline/pipeline_configuration.png
   :align: center 

===========
The Example
===========

The example configuration above assumes you have configured a **Library Source** that has a 
``maven`` and ``sonarqube`` library contributing the ``unit_test``, ``build``, and ``static_code_analysis``
steps. 

The pipeline configuration defines a **Stage** called ``continuous_integration`` which invokes the 
steps defined, in order. 

Then, in the **Pipeline Template**, you will be able to invoke the ``continuous_integration`` stage that 
was defined. 