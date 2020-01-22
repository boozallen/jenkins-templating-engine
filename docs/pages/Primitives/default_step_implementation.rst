.. _default step implementation:

---------------------------
Default Step Implementation
---------------------------

The JTE default step implementation allows you to generically define pipeline steps
in a ``steps`` block in your configuration file.

This can be used for simple step implementations that can run from a container image
by executing a shell command or script. Optionally, artifacts generated during the
step execution can be stashed for later retrieval.

.. note::

    If the default step implementation is called for a step but that step has not
    been defined in the ``steps`` block then nothing will happen.

*************
Configuration
*************

.. csv-table:: Configuration Options
   :header:  "Field", "Description", "Default Value", "Required?"

   "stage", "Display name for this step.", "step_name", "false"
   "image", "Container image to run the step within.", ,"true"
   "command", "The shell command to run inside the step container image", ,"if script is not set"
   "script", "The path to a shell script to execute", ,"if command is not set"
   "stash.name", "The ID of the resultant stash of files from the step", ,"required if a stash is to be used"
   "stash.includes", "The files to preserve.", "``**``", "false"
   "stash.excludes", "The files to ignore.", , "false"
   "stash.useDefaultExcludes", "Whether to use the default exludes of the Jenkins stash step.", "true", "false"
   "stash.allowEmpty", "Whether or not the stash may contain no files", "false", "false"

**********************
Example Implementation
**********************

Below is an example of configuring the default step implementation to act as a ``unit_test``
step to run a test suite through maven.

.. code:: groovy

    steps{
        unit_test{
            stage = "Unit Test"
            image = "maven"
            command = "mvn clean verify"
            stash{
                name = "test-results"
                includes = "./target"
                excludes = "./src"
                useDefaultExcludes = false
                allowEmpty = true
            }
        }
    }

With this configuration in place, ``unit_test()`` will be callable from a pipeline template and will
run ``mvn clean verify`` inside the ``maven`` container image.  The ``./target`` directory will be
stored for later use by stashing the contents in a stash named ``test-results``.

In addition to calling commands, your default step implementations can run scripts. Instead of the
`command` field, use the `script` field and pass the path for the script in your application
repository. Below is an example

.. code:: groovy

    steps{
        unit_test{
            stage = "Unit Test"
            image = "maven"
            script = "./tests/unit_test.sh"
            stash{
                name = "test-results"
                includes = "./target"
                excludes = "./src"
                useDefaultExcludes = false
                allowEmpty = true
            }
        }
    }

You can name the keys within ``steps`` whatever you please, and they will be callable as methods.

