Jenkins Templating Engine
-------------------------

The Jenkins Templating Engine is a plugin developed by Booz Allen Hamilton focused
on pipeline templating and governance. In practice, this allows you to consolidate 
your team's pipelines into shareable workflows that define the business logic of 
your software delivery processes while allowing for optimal pipeline code reuse by
pulling out tool specific implementations into library modules. 


Build
=====

This plugin uses the `Gradle JPI Plugin`_ to build. 

to build run: ``gradle clean jpi``

.. _`Gradle JPI Plugin`: https://github.com/jenkinsci/gradle-jpi-plugin

Test
====

TODO: 
  * add more coverage

Currently using Spock to test some aspects. 

To execute tests run: ``gradle test``
