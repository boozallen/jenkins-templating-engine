.. _Externalizing Library Configuration:

-----------------------------------
Externalizing Library Configuration
-----------------------------------

One of the major benefits of organizing your pipeline code into libraries is
the ability to reuse these libraries across different teams.

In order to achieve this level of reusability, it's best to externalize hard
coded values as parameters that can be set from the pipeline configuration repository.

========================
Creating an Example Step
========================

To demonstrate how to do this, let's create a step that simply prints a message a
specified number of times, with both the message and number of times printed being
values passed via the configuration file.

********************
Repository Structure
********************

.. code::

    README.rst
    example/
        printAThing.groovy

With this source code repository configured as a Library Source, we would be
able to load the ``example`` library from our pipeline configuration repository
and invoke the ``printAThing()`` step from a pipeline template.

*********************
Library Configuration
*********************

When writing a library step, a ``config`` variable comes autowired into your scope
that can be referenced to access configurations defined in your library block from
the configuration file.

Let's create a configuration parameter ``number`` to represent the number of times
to print the message and a configuration parameter ``message`` to represent the
message to be printed.

**pipeline_config.groovy**:

.. code::

    libraries{
        example{
            number = 3
            message = "my message to be printed!"
        }
    }


****************
Step Declaration
****************

We can now write our library step to access these parameters directly on the
``config`` variable automatically provided to the step.

.. important::

    To access the number of times to print the message, read the ``config.number``
    value.  To access the message to be printed, read the ``config.message`` value.

**printAThing.groovy**:

.. code::

    void call(){
        for(def i = 0, i < config.number, i++){
            println config.message
        }
    }

**********
Conclusion
**********

We learned how to access external library configurations via the pipeline configuration
repository.  These parameters can be any serializable data type and are accessible
from within your step via the autowired ``config`` variable.

.. important::

    The pipeline step we built today has some flaws.  It does not guarantee that
    the number of times to print the message is an Integer or that the message
    to be printed is a String. For next steps, learn how to :ref:`validate library
    parameters <Validate Library Parameters>`

