.. _Leverage Lifecycle Hooks:

------------------------
Leverage Lifecycle Hooks
------------------------

Sometimes it is necessary to trigger specific pipeline actions at certain times
during pipeline execution. For example, if you wanted to send multiple notification
types after a particular pipeline step or at the conclusion of a pipeline if the
build was failure.

JTE supports this type of Aspect Oriented Programming style event handling through
annotation markers that can be placed on library steps.

The following lifecycle hook annotations are available:

.. csv-table:: JTE LifeCycle Hook annotations
   :align: center
   :header: "Annotation", "Description"

    "``@Validate``", "Will get executed at the beginning of a pipeline run, should throw exception if this step does not have its prerequesites"
    "``@Init``", "Will get executed at the beginning of a pipeline run, after Validate"
    "``@BeforeStep``", "Will get executed before every pipeline step invocation"
    "``@AfterStep``", "Will get executed after every pipeline step invocation"
    "``@CleanUp``", "Will get executed at the end of a pipeline run"
    "``@Notify``", "Will get executed after every pipeline step invocation as well as at the end of the pipeline run"

==============
Implementation
==============

In order to write a pipeline step leveraging one of these lifecycle hooks,
you must annotate the step with the annotation and accept a context input
parameter to the method.

This context parameter provides context so that the step can vary its
behavior based on what just happened in the pipeline.

.. csv-table:: JTE LifeCycle Hook Context Parameter
   :align: center
   :header: "Variable", "Description"

   "``context.library``", "The name of the Library that contributed the step associated with the hook"
   "``context.step``", "The name of the Step associated with the hook"
   "``context.status``", "The current pipeline build status"

.. important::

    ``context.library`` and ``context.step`` will be ``null`` before and after pipeline execution
    and are only to be leveraged by ``@BeforeStep``, ``@AfterStep``, and ``@Notify``

.. note::

    The reference to the variable name ``context`` makes the assumption that you've named the
    input parameter to the step with the hook annotation ``context``.  In practice, this variable
    name can be whatever you please

=====================
Conditional Execution
=====================

Sometimes you'll only want to invoke the Hook when certain conditions are met, such as a build failure
or in relation to another step (like before static code analysis).

Each annotation accepts a ``Closure`` parameter.  If the return object of this closure is
`true <http://www.groovy-lang.org/semantics.html#Groovy-Truth>` then the hook will be executed.

While executing, the code within the ``Closure`` will be able to resolve the ``context`` variable
described above as well as the library configuration variable ``config``.

Example Syntax:

.. code::

    @BeforeStep({ context.step.equals("build") })
    void call(context){
        /*
            execute something right before the library step called
            build is executed.
        */
    }

.. important::

    The closure parameter is optional. If omitted, the hook will always be executed.

======================
Example Implementation
======================

Let's say we wanted to print to the console log a specific message when the
``build`` step fails to execute successfully.

************************
Step 1: Create a Library
************************

Within one of your configured :ref:`library sources <Pipeline Libraries>`, create a new
directory.  For this example, let's call the library ``slack``

**Repository Structure**:

.. code::

    README.rst
    slack/


***********************
Step 2: Create the Step
***********************

When creating steps whose only purpose is to be invoked at a particular
stage of the pipeline, the name of the step does not matter because it
is not being called via a pipeline template.  These steps are discovered
by JTE and invoked dynamically.

This means that if there are multiple steps with the same annotation
they will all be executed. This is useful for situations such as
creating pipeline templates to be used by multiple teams all using
different tools to receive pipeline notifications - or using multiple
notification tools (email, slack, sms, etc).

For this example, we'll call the step ``sendSlackNotification``.

.. code::

    README.rst
    slack/
        sendSlackNotification.groovy

We must remember to annotate the method and pass the step a context
input parameter.  Remember, our goal is to create a step that sends
a message when the ``build`` step fails.

**sendSlackNotification.groovy**:

.. code::

    @AfterStep({
        context.step.equals("build") && context.status.equals("FAILURE")
    })
    void call(context){
        slackSend color: '#ff0000', message: "Build Failure: ${env.JOB_URL}"
    }

.. note::

    This example assumes you've installed and appropriately configured the
    `Slack Notification Plugin <https://jenkins.io/doc/pipeline/steps/slack/>`_

************************
Step 3: Load the Library
************************

With the library created and slack notification step written all you have
to do to send a slack notification after the ``build`` step fails is
load the ``slack`` library in your pipeline configuration file.

.. code::

    libraries{
        slack
    }

JTE will automatically pick up that a library has been loaded that
contributes steps with lifecycle hook annotations.
