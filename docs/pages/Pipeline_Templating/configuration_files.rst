.. _Configuration Files:

-------------------
Configuration Files
-------------------

With the Jenkins Templating Engine, configuration files are used to populate templates with
the information necessary to execute.

The configuration file is a groovy file named ``pipeline_config.groovy`` that gets placed at
the root of the source code repository or within a pipeline configuration repository.

******
Syntax
******

The configuration file is a custom groovy domain specific language intended to avoid the
burdensome syntax of JSON without the whitespace pitfalls of YAML.

While the flexibility of what can be done within a configuration file is limited by the
:ref:`sandbox<sandboxing>`, the file is executed as a groovy script and builds a configuration object.

The configuration file acts as a nested builder language will accept any arbitrary configuration. Beyond
several JTE configuration fields, it is up to specific primitives and libraries to provide meaning to the
keys and values provided within a configuration.

An example configuration file would be:

.. code::

    // restrict individual repository Jenkinsfiles
    allow_scm_jenkinsfile = false
    // skip the default JTE checkout and do it explicitly
    skip_default_checkout = true
    // define application environment objects
    application_environments{
        dev{
            long_name = "Development"
        }
        prod{
            long_name = "Production"
        }
    }

    /*
      define libraries to load.
      available libraries are based upon the
      library sources configured.
    */
    libraries{
        github
        sonarqube
        docker
        // library configurations are determined by
        // the specific library implementation
        openshift{
            url = "https://example.openshift.com"
            cred_id = "openshift"
        }
    }


**************
Configurations
**************

^^^^^^^^^^^^^^^^^^^^^^^^
Application Jenkinsfiles
^^^^^^^^^^^^^^^^^^^^^^^^
By default, if there is a Jenkinsfile in an application source code repository
it will be executed as the pipeline template.

When using JTE to share a common pipeline template across multiple applications
an organization may want to prevent individual repositories from providing their
own pipeline templates for governance.

To disable this default behavior, specify ``allow_scm_jenkinsfile = false`` within
a governance tier's configuration file.

For a full overview of how governance works with JTE, check out the
:ref:`Governance Model<Governance Model>`.


^^^^^^^^^^^^^^^^
Template Methods
^^^^^^^^^^^^^^^^

The JTE model is similar in design to the `Template Method Design Pattern <https://dzone.com/articles/design-patterns-template-method>`_.
A template is defined which invokes abstract methods (steps) yet to be defined.

These methods are then implemented by either steps from the libraries loaded or the :ref:`default step implementation<default step implementation>`.

As a result, situations may arise where a step is called from a template that has not been implemented via the available options. In some cases,
this is acceptable.

The ``template_methods`` block of your configuration file is used to declare the steps that will be called from your template. In the event
that a step is called that has not been defined, it will be routed to the default step implementation if it's name appears in the ``template_methods`` block.

By default, JTE provides the following default values for ``template_methods``.

.. code::

    template_methods{
        unit_test
        static_code_analysis
        build
        scan_container_image
        penetration_test
        accessibility_compliance_test
        performance_test
        functional_test
    }

