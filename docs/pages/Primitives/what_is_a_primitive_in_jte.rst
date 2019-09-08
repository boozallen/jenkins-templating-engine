.. _Primitives: 

---------------------------
What is a primitive in JTE?
---------------------------

In JTE, primitives are constructs leveraged largely for syntactic sugar to make pipeline 
templates more reader friendly. 

JTE provides several primitives by default and it is possible to extend the Jenkins Templating
Engine plugin with your own plugin providing additional primitives. 


.. csv-table:: Jenkins Templating Engine Primitives
   :header: "Name", "Description"

   ":ref:`Application Environments<Application Environments>`", "Injects an application environment object for reference in your pipeline template to hold application environment specific information." 
   ":ref:`Stages<Stages>`", "Creates a new step for reference inside your template that can invoke a group of steps in order." 
   ":ref:`Keywords<Keywords>`", "Define variables to be used in your pipeline template from the configuration file." 
   ":ref:`Default Step Implementation<default step implementation>`", "Create a step implementation dynamically through the configuration file"
