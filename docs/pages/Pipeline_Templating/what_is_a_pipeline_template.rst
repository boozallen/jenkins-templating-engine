.. _Pipeline Templating: 

----------------------------
What is a pipeline template?
----------------------------

Templates
*********

When developing a pipeline used by multiple teams, you'll often find that 
the workflow is the same while the specifics of what each step in the process does
may differ between applications.  

Whether the application is a Java app packaged as a war and deployed to an AWS
EC2 instance or a React app being statically bundled and deployed to an S3 bucket 
or Nginx instance, the steps in your pipeline are the same.

In this example, both applications could have a pipeline workflow that performs unit testing, 
static code analysis, packaging an artifact, and deploying that artifact to an application environment. 

.. figure:: ../../images/home/sample_template.png
   :scale: 50%
   :align: center

   Sample Pipeline Workflow

You would represent this workflow in a template! Templates are built by calling steps and referencing 
primitives defined in your pipeline configuration file. 

*****
Steps 
*****

Pipeline steps are methods supplied by pipeline libraries that implement a specific action in 
your pipeline template. In your pipeline configuration file you will specify what libraries should
be loaded. 

Each of these libraries will contribute steps that can be called from your pipeline template. 

In our simple example, a pipeline template could be defined as follows: 

.. code:: 

    unit_test()
    static_code_analysis()
    build()
    deploy_to dev 

This template could be reused by multiple teams using different tools by creating :ref:`libraries<Getting Started>` which
implement the pipeline steps ``unit_test``, ``static_code_analysis``, ``build``, and ``deploy_to``.  Once these libraries
are created, a configuration file is created to specify which libraries to load. 

For the deployment step, you'll notice that a ``dev`` application environment was referenced to specify where the 
deployment should take place. This ``dev`` application environment is an example of a primitive that can be defined
alongside your template in the configuration file. 

**********
Primitives 
**********

Primitives are template constructs whose purpose is to make the pipeline template as simple and easy to read as possible. 

The default primitives currently supported are :ref:`Application Environments<Application Environments>`, 
:ref:`Keywords<Keywords>`, and :ref:`Stages<Stages>`. 

It is possible to write a Jenkins Plugin that extends the Jenkins Templating Engine to add a
:ref:`custom primitive<Adding a Custom Primitive>`.

*******************
Configuration Files
*******************

To complete this example, we can build out sample pipeline configuration files for each application. 

Configuration files provide the information necessary to populate a pipeline template with what's needed
to execute. 

For our example, we can define a common pipeline configuration to be inherited by both applications that defines
the development application environment and a common SonarQube pipeline library to perform the static code analysis:

.. code:: 

   // define common application environment 
   application_environments{
       dev{
           long_name = "Development" 
           ec2{
               ips = [ "1.2.3.4", "1.2.3.5" ]
               credential_id = "ec2_ssh" 
           } 
           s3{
               url = "https://s3.example.com"
               path = "content/" 
               credential_id = "s3_bucket" 
           }  
       }
   }

   // define pipeline libraries common between applications
   libraries{
       merge = true 
       sonarqube // supplies the static_code_analysis step 
   }


For the Java application: 

.. code:: 

   libraries{
       java  // supplies the build step 
       junit // supplies the unit_test step 
       ec2   // supplies the deploy_to step 
   }

For the React application: 

.. code:: 

   libraries{
       React // supplies the build step 
       jest  // supplies the unit_test step 
       s3    // supplies the deploy_to step  
   }

You would then create a :ref:`Governance Tier<Governance Model>` to point JTE to your common 
configuration and place the application specific configurations at the root of their source 
code repository in a file called ``pipeline_config.groovy``.  

:ref:`Go here to learn more about JTE Configuration Files<Configuration Files>`

