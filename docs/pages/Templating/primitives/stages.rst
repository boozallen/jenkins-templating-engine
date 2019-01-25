.. _Stages:

------
Stages
------

Stages are a primitive that allow you to group together steps to be called at once
in order to avoid repetition in the pipeline template. 

Stages are defined by through the ``stages`` key, with subkeys becoming available 
as steps within your pipeline template.

A common example would be creating a continuous integration stage: 

.. code::

    stages{
        continuous_integration{
            unit_test
            static_code_analysis
            build
            scan_artifact
        }
    }

and then in your template: 

.. code::

    continuous_integration()
    deploy_to dev 