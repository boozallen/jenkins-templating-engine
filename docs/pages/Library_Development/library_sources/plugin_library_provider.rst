.. _plugin library provider:

-----------------------
Plugin Library Provider
-----------------------

JTE makes it possible to package a set of libraries into a Jenkins Plugin.

When a library-providing plugin has been installed, a new option becomes available when selecting a library source:

.. image:: ../../../images/Library_Development/library_sources/pluginsource.png

After selecting ``From a Library Providing Plugin``, just select the library providing plugin from the autopopulated dropdown list.

===============================
Why package libraries this way?
===============================

It's a lot less work to configure a source code repository and configure it as a library source.
So why would someone want to spend the time packaging their libraries as a Jenkins Plugin?

**1. Plugin Dependency Management**

    | Steps contributed by libraries often rely on Jenkins plugins in their implementation.  Packing libraries as a
    | Jenkins Plugin ensures that all other plugin dependencies (and their versions) are satisfied when the library
    | providing plugin is installed.

**2. Initialization Performance**

    | It slows down library loading to reach out to a remote source code repository to retrieve libraries.
    | When libraries are packaged in a Jenkins Plugin, library loading can be sped up significantly.

**3. Ensuring a specific version of JTE**

    | It can be helpful to ensure that specific version of the Templating Engine Plugin is installed if the
    | interface between JTE and libraries is changed.

**4. Library Source Versioning**

    | One specific use-case for the Templating Engine Plugin is for DevOps engineers supporting multiple pipelines
    | simultaneously.  In these situations, the libraries provided are likely shared across the teams as well.
    | Over time, libraries can be changed in a way that breaks backwards compatibility.  Packaging libraries as
    | a plugin allows you to version your set of libraries as an artifact where teams can upgrade when it makes
    | the most operational sense.

===================
Building the Plugin
===================

Building the plugin leverages

*****************
Sample Repository
*****************

We have created a sample repository you can use as a launching pad for packaging your libraries as a plugin.

Checkout the `jte-libs-as-a-plugin <https://github.com/steven-terrana/jte-libs-as-plugin.git>`_ repository
to get started.

====================
Development Workflow
====================

If packaging your libraries via a Jenkins Plugin, we recommend configuring your library source as a
:ref:`SCM Library <>` during development.

If you're following the repository template based off of the sample repository mentioned above,
you would configure the SCM Library repository URL and set the base directory to ``src/main/resources/libraries``.

This way, you can continuously push updates to the remote repository for testing, and only once you're satisfied
with the changes, increment the version and package the libraries as a plugin.
