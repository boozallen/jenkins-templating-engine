Primitives
----------

In JTE, primitives are constructs leveraged largely for syntactic sugar to make pipeline 
templates more reader friendly. 

JTE provides several primitives by default and it is possible to extend the Jenkins Templating
Engine plugin with your own plugin providing additional primitives. 


.. csv-table:: Jenkins Templating Engine Primitives
   :header: "Name", "Description"

   "Application Environments", "Injects an application environment object for reference in your pipeline template to hold application environment specific information." 
   "Stages", "Creates a new step for reference inside your template that can invoke a group of steps in order." 
   "Keywords", "Define variables to be used in your pipeline template from the configuration file." 

.. toctree::
   :hidden:
   :maxdepth: 1
   :glob:
   :titlesonly:

   **