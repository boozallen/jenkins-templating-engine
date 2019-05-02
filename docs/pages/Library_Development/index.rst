.. _Library Development: 

-------------------
Library Development
-------------------

Within the Jenkins Templating Engine framework, the technical implementation of different actions is 
done via libraries composed of one or more steps.  

Libraries are configured as :ref:`Library Sources <Pipeline Libraries>` either globally 
as part of the Global Jenkins Templating Engine configuration or within a particular
:ref:`Governance Tier <Governance Model>`. 

Once a SCM repository has been set as a Library Source, you can begin creating libraries. 

==================
Creating a Library
==================

Creating a Library is as easy as creating a root level directory within your configured Library Source's 
source code repository. 

An example repository structure could be: 

.. code:: 

    README.rst
    example_library/ 

The library is named and is referenceable by the directory name. 

===============
Creating a Step
===============

To create a step, create a ``.groovy`` file within the library directory. The step name 
will be that of the basename of the file.  

For example, to create a ``build`` step, add a ``build.groovy`` file: 

.. code:: 

    README.rst
    example_library/ 
        build.groovy 

In most cases, writing a step is done by defining a ``call`` method within the groovy step file created. This 
``call`` method can take input arguments and return objects.  Within the step you can execute any 
old regular Jenkins pipeline code, including invoking other steps defined by loaded libraries. 

Let's say you wanted this build step to run ``mvn clean verify``, your ``build.groovy`` file 
could be: 

.. code:: 

    void call(){
        node{
            sh "mvn clean verify" 
        }
    }

.. note:: 

    Steps, like regular global variables in Jenkins Shared Libraries, can define methods other than call. 
    See an example :ref:`here <noncall_methods>` 


================
Load the Library 
================

With a directory created called ``example_library`` within your Library Source, you can 
now load this library from your pipeline configuration to make the steps contained therein 
accessible from your pipeline template. 

**pipeline_config.groovy**: 

.. code:: 

    libraries{
        example_library
    }


=================================
Invoking the Step From a Template
=================================

With the ``example_library`` library loaded via your pipeline configuration file, you can then invoke 
the ``build`` step from a pipeline template via: 

.. code::

    build()

which will execute the shell command defined in the step. 

=======================
Advanced Considerations
=======================

Now that you know the basics of how to create a library, populate the library with steps, 
reference them from your pipeline configuration file, and invoke the step from a pipeline 
template you can now move on to some of the more advanced library development concepts: 

.. csv-table:: Advanced JTE Library Development 
   :header: "**Concept**", "**Description**"

   ":ref:`Externalizing Library Configuration <Externalizing Library Configuration>`", "Learn how to build reusable libraries by externalizing configurations to the pipeline configuration file"
   ":ref:`Validate Library Parameters <Validate Library Parameters>`", "Learn how to validate library input parameters as part of your steps." 
   ":ref:`Leverage Lifecycle Hooks <Leverage Lifecycle Hooks>`", "Learn how to register steps for automatic invocation through annotation based registration"



.. toctree::
   :hidden:
   :maxdepth: 1
   :glob:
   :titlesonly:

   *