.. _Template Step: 

-----------------
The Template Step
-----------------

The Jenkins Templating Engine adds a pipeline step called ``template`` that can be called from a pipeline. 

This step is typically called indirectly when using a :ref:`Multibranch Job <Multibranch Job>` or when
creating a :ref:`GitHub Organization Job <GitHub Organization Job>`. 

When this step is executed, the pipeline will have access to the primitives that have been defined via 
the aggregated result of the specified pipeline configuration files as well as any steps that have been 
contributed by loaded libraries. 

===================================================
Set the Template implicitely from a Governance Tier 
===================================================

The real power in the Jenkins Templating Engine is that it allows you to 
externalize the Jenkinsfile from an individual source code repository, and 
generalize it to be used by multiple teams simultaneously. 

This means that the default way to define a Template is externally in a source 
code repository via a Governance Tier. 

This is exactly what happens when you invoke the ``template`` step directly 
with no parameters via ``template()``. 

When no parameters are passed, the pipeline configuration files defined on 
each Governance Tier are aggregated.  This aggregated configuration is then used 
to initialize the Template's environment with the various Primitives that were defined
as well as load any libraries that have been configured. 

==============================
Set the Template via a Closure 
==============================

The ``template`` step itself takes an optional ``Closure`` parameter.  This ``Closure`` parameter 
is most often used for testing, though if governance is not a factor it works just as well as 
defining your pipeline template externally. 

An Example
^^^^^^^^^^

Let's assume that you are running a typical Jenkinsfile and want to invoke the ``template`` step directly.

You have configured a Governance Tier that will load a library contributing a ``build()`` step. 

Executing this ``build`` step then, can be done by trying to invoke it from within the context of the 
``template`` step as follows: 

.. code:: groovy

    /*
        here you do not have access to the 
        build step because it has not been loaded yet
    */
    template{
        /*
            now that you are within the context of the 
            template step, the library will have been loaded
            and you will be able to execute the build step
        */
        build() 
    }

.. note:: 

    When a ``Closure`` is passed to the ``template`` step - we do not look for a 
    template, regardless if a Governance Tier specifies what template should be used. 

    This is because if you want to restrict what template should be used, it's best 
    to use a Multibranch or GitHub Organization job to apply these standards across 
    entire repositories or organizations.  

