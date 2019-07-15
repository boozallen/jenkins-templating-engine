.. _Validate Library Parameters:

---------------------------
Validate Library Parameters
---------------------------

.. note::

    If you haven't read how to
    :ref:`externalize library configurations <Externalizing Library Configuration>`,
    it's strongly recommended you read that to have context on this page.

This page explains how to confirm that library parameters, which are field-value
pairs passed to a library though its external configuration, are valid. This
means confirming any parameters required by your library are supplied, and that
all parameters supplied have an acceptable value and/or data type.

For example, say your library requires a name for a Jenkins credential, and
expects it to be passed through a field in your library's config called
"credential." You would want to ensure when your library is used that:

1. In the aggregated *pipeline_config.groovy*, there is a "credential" field in your library's configuration
2. The value for that "credential" field is a String (since we usually reference Jenkins credentials by a name)


=========================================
The Library Configuration File - Overview
=========================================

.. important::

   Including a library config (*library_config.groovy*) in your library is
   entirely optional. There are other ways to validate parameters, which are
   covered below


You can validate the parameters passed via *pipeline_config.groovy* by a library
configuration file, or library config. This file, named *library_config.groovy*,
goes at the root of your library and includes a list of required and optional
fields, as well as tests you want to apply to those fields. When your library is
loaded by a JTE pipeline, each of your library's parameters from the aggregated
pipeline config file are tested according to the contents of the library config.
If those tests fail, or there's an extra field you didn't specify, the pipeline
fails.

Here's an example of a *library_config.groovy* file:

.. code-block:: groovy
   :caption: library_config.groovy

   fields{
     required{
       credential = String
       mode = [ "mode1", "mode2" ]
     }
     optional{
       library_option = /[oO]ption\s*\d+/
       do_optional_thing = Boolean
     }
   }

In this example, there are two required fields ("credential" and "mode"), and
two optional fields ("library option" and "do_optional_thing"). The pipeline
config is required to provide two parameters: a "credential" field with a String
value and a "mode" field with a value of either "mode1" or "mode2." There are
also two optional fields: "library_option", which must have a value that matches
the regex expression ``/[oO]ption\s*\d+/``, and "do_optional_thing",
which must have a Boolean value. Given this example library config, this
would be a valid snippet from your aggregated pipeline config:

.. code-block:: groovy

   libraries{
     your_library{
       credential = "credentialA"
       mode = "mode1"
       library_option = "Option123"
     }
   }

As would this:

.. code-block:: groovy

   libraries{
     your_library{
       credential = "secret-password"
       mode = "mode2"
       library_option = "option 3"
       do_optional_thing = true
     }
   }

Both these snippets have the two required fields, "credential" and "mode," and
the only other fields are listed as optional in the library config. In addition,
the values for each of those fields pass the tests set in the library config.

==============
Listing Fields
==============

In your library config you specify a list of fields that you expect to
be used in your library's external configuration (i.e. in the pipeline config
file). In this list, you must also distinguish between *required* and
*optional* fields. This list establishes two rules:

1. If a *required* field is not present in your library's
section of the pipeline configuration, then the pipeline fails

2. If there is any field present in your library's section of
the pipeline configuration that is **not** listed as either a *required* or
*optional* field, then the pipeline fails

=====================
Validating Parameters
=====================

For every field in your library configuration, you must specify a rule that
field's value must conform to. There are currently three types of rules:

* the value must be a particular data type (Type Match)
* the value must be one of a set of listed values (Enum Match)
* the value must match a given regex expression. (Regex Match)

Type Match
-----------

If you want to test a parameter is of a particular data type (e.g. boolean,
string), you put the name of that data type as the field's value in the
library config. For example, if I want to make sure "credential" is of type
"String", my *library_config.groovy* would contain:

.. code-block:: groovy

   fields{
     required{
       credential = String
     }
   }

The current options for data types to test for are:

* boolean / Boolean
* String
* Integer / int
* Double
* BigDecimal
* Float
* Number

Enum Match
----------

If you want to test that a parameter is one a particular set of values, you can
put an array of acceptable options as the field's value in the library config.
For example, if you want to make sure the field "intensity" is either "high,"
"medium," or "low," your *library_config.groovy* would contain:

.. code-block:: groovy

   fields{
     required{
       intensity = ["high", "medium", "low"]
     }
   }

Given this library config, if the "intensity" value for your library (set by
the pipeline config) isn't one of those three values, then the pipeline will
throw an error. For example, this pipeline config snippet would be valid:

.. code-block:: groovy

   libraries{
     your_library{
       intensity = "medium"
     }
   }

But this snippet would cause an error:

.. code-block:: groovy

   libraries{
     your_library{
       intensity = "intense"
       // throws an error because "intense" is neither "high," "medium", nor "low"
     }
   }

.. note::

	 You can put more than strings into these enum arrays; any type of object will work, and multiple types of objects can be in the same array.

Regex Match
-----------

If you want to test that a String parameter conforms to a particular pattern,
you can put a `regex <https://en.wikipedia.org/wiki/Regular_expression>`_
expression representing that pattern for the field's value in the library config.
For example, if you want to make sure "library_option" is one word with only
alphanumeric characters, your *library_config.groovy* would contain:

.. code-block:: groovy

   fields{
     required{
       library_option = /^[a-zA-Z0-9]+$/
     }
   }

Given this library config, if the "credential" value for your library, set by
the pipeline config, doesn't match the regex expression ``^[a-zA-Z0-9]+$``, then
the pipeline will throw an error. For example, this snippet would be valid:

.. code-block:: groovy

   libraries{
     your_library{
       credential = "secretPassword"
     }
   }

But this snippet would cause an error:

.. code-block:: groovy

   libraries{
     your_library{
       credential = "secret-password"
       // throws an error because of the "-"
     }
   }

.. note::

   Resources for writing and understanding regular expressions include this
   `Java summary of regular-expression constructs <https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html>`_,
   as well as this `Regex Cheat Sheet <https://www.rexegg.com/regex-quickstart.html>`_


=============
Nested Fields
=============

The parameters for your library in the pipeline config aren't always in a flat
list; it's common to logically group parameters in submaps. For example, your
pipeline config may have a snippet like this:

.. code-block:: groovy

   libraries{
     your_library{
       image{
         name = "your-image"
         credential = "repo-cred"
       }
       web_service{
         url = "https://example.com"
         credential = "service-cred"
         options{
           optionA = "foobar"
           optionB = 7
           optionC = true
         }
       }
     }
   }

The library config is only concerned with the lines that have key-value pairs
where the value isn't another map. For example, you wouldn't be able to set ``web_service``
or ``web_serivice.options`` as a required or optional field, but you can set the
fields under them, like ``web_service.credential`` or ``web_service.options.optionB``
as required/optional.

Also, if two parameters are grouped under the same field they don't both have
to be required/optional. Continuing with the *pipeline_config.groovy* example
snippet above, you could require ``optionA`` and ``optionB``, but not ``optionC``.
The resulting library config might look like this:

.. code-block:: groovy

   fields{
     required{
       image{
         name = String
         credential = String
       }
       web_service{
         url = /^(http|https):\/\/.+$/
         credential = String
         options{
           optionA = String
           optionB = Integer
         }
       }
     }
     optional{
       web_service{
         options{
           optionC = Boolean
         }
       }
     }
   }

You would need to include the whole structure, but you can pick and choose
which fields within those submaps are required and which are optional.

=============================
Additional Validation Methods
=============================

While using a library configuration covers most libraries' validation
requirements, you may wish to do more complex validation, or you may have
a particular need that's not met. For those cases, you can either create a
separate pipeline step with the ``@Validate`` annotation, or you can validate
the parameters within the step itself.

Validating Within the Step
--------------------------

In our primary example in how to
:ref:`externalize library configurations <Externalizing Library Configuration>`,
we had a step that took an Integer ``number`` parameter, as well as a String
``message`` parameter. We assume that:

* those input parameters are both configured
* those input parameters are of the correct type

To actually validate these assumptions, the following code could be used:

.. code-block:: groovy

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

Validating in a Separate Step
-----------------------------

Since your library's parameters are accessible w/in any step of your library,
you can create a separate step that performs the validation, then annotate that
step with ``@validate`` to run the step when the pipeline starts. Using the same
example as above, the code below could be used.

.. code-block:: groovy

    @validate
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

    }
