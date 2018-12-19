Jenkins Templating Engine
-------------------------

The Jenkins Templating Engine is a plugin developed by `Booz Allen Hamilton`_ focused
on pipeline templating and governance. In practice, this allows you to consolidate 
your team's pipelines into shareable workflows that define the business logic of 
your software delivery processes while allowing for optimal pipeline code reuse by
pulling out tool specific implementations into library modules. 

.. _`Booz Allen Hamilton`: https://www.boozallen.com/

Build Plugin 
============

This plugin uses the `Gradle JPI Plugin`_ to build. 

to build run: ``make jpi``

.. _`Gradle JPI Plugin`: https://github.com/jenkinsci/gradle-jpi-plugin

Test
====

Currently using Spock to test some aspects. Significant coverage is still required. 

To execute tests run: ``make test``

Build Documentation 
===================

Leveraging Sphinx to build documentation. 

To build docs locally run: ``make docs`` 

The docs will be viewable by opening ``docs/_build/html/index.html`` 

To get hot loading of docs during development: ``make livedocs`` 