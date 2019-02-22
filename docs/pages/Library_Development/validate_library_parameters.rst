.. _Validate Library Parameters: 

---------------------------
Validate Library Parameters
---------------------------

.. note:: 

    If you haven't read how to :ref:`externalize library configurations <Externalizing Library Configuration>`, it's 
    strongly recommended you read that to have context on this page. 

This page is just an example of a pattern used by Booz Allen's Solutions Delivery Platform to validate 
library configurations within our `SDP Libraries`_. 

.. _SDP Libraries: https://github.com/boozallen/sdp-libraries.git 


====================
Parameter Validation
====================

A good practice is to start off a step declaration by parsing any input parameters or 
library configuration options.

In our primary example in how to :ref:`externalize library configurations <Externalizing Library Configuration>`,
we had a step that took an Integer ``number`` parameter, as well as a String ``message`` parameter. 

The example assumes that:

* those input parameters are both configured
* those input parameters are of the correct type

To actually validate these assumptions, the following code could be used: 

.. code:: 

    void call(){

        // define library configuration parameters
        String error_msg = """
        This step has the following library parameters: 

          number:  [Integer] // required 
          message: [String]  // required

        """

        // validate number 
        if (config.number){
            if (!(config.number instanceof Integer)){
                error """
                number parameter must be an Integer, received [${config.number}]
                --
                ${error_msg}
                """
            }
        }else{
            error """
            must provide number parameter
            --
            ${error_msg}
            """
        }

        // validate message 
        if (config.message){
            if (!(config.message instanceof Integer)){
                error """
                message parameter must be a String, received [${config.message}]
                --
                ${error_msg}
                """
            }
        }else{
            error """
            must provide message parameter
            --
            ${error_msg}
            """
        }

        // execute step functionality
        for(def i = 0, i < config.number, i++){
            println config.message 
        }

    }