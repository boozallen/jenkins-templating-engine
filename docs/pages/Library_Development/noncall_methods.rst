.. _noncall_methods: 

------------------------------------------
Defining Non-Call Methods in Library Steps
------------------------------------------

It is sometimes advantageous to define more steps than just the call method.  

Within the context of creating templates, this pattern comes up when defining internal methods 
used by library steps as opposed to being invoked directly by a template. 

For example: 

.. code-block:: groovy
   :caption: test_step.groovy

    def printMessage(String message){
        println "the message is ${message}" 
    }

creates a step ``test_step`` that can be invoked via: 

.. code-block:: groovy

    test_step.printMessage("example!")

    