.. _Pipeline Libraries:

---------------
Library Sources
---------------

The Jenkins Templating Engine provides a mechanism for using the same
pipeline across multiple applications, regardless of what tools they are using.

To do this, there has to be a way to organize different implementations of the
same step.  For example, if two teams are using gradle and maven to build their
respective applications via a ``build`` step invoked from a template - then there
must be a way of storing and labeling these different implementations.

That's where libraries come in.  In this example, we would create a ``maven``
and ``gradle`` library, each contributing a ``build`` step via a file called
``build.groovy``.

.. note::

    As this example illustrates, you can think of the pipeline templates you create
    as an api that libraries and :ref:`primitives <Primitives>` implement.  Therefore,
    libraries that are meant to be interchangeable should contribute steps with the same name.
    This way, multiple teams can use the same pipeline template by specifying different libraries
    to load in their pipeline configuration file.

A **Library Source** is a reference to a place from which libraries can be found for loading.

Following the governance model JTE lays out, Library Sources can be defined globally under
``Manage Jenkins >> Configure System >> Jenkins Templating Engine`` and become available
to every pipeline on the Jenkins instance.

Alternatively, access to libraries can be scoped to jobs within a Folder in Jenkins by defining
the Library Source on the Folder.

========================
Library Source Structure
========================

In general, individual libraries are just directories within the configured library source.

The library is referenceable in your ``pipeline_config.groovy`` file via the directory name.

Each groovy file within a library directory (besides the library configuration file) will be loaded
as a step invokable from a pipeline template where the step name is equal to the base file name.

=================
Library Providers
=================

Libraries can come from different sources.  The current two storage mechanisms for libraries are Source Code
Repositories and packaging libraries into a separate Jenkins plugin. Both of these options have benefits and
draw backs that are outlined on their respective pages.

.. csv-table:: Library Providers
   :header: "type", "Description"

   ":ref:`SCM Library Provider<scm library provider>`", "Find libraries in a Source Code Repository"
   ":ref:`Library Providing Plugin<plugin library provider>`", "Package libraries into a Jenkins Plugin"


.. toctree::
   :hidden:
   :titlesonly:

   scm_library_provider
   plugin_library_provider
   example_library_walkthrough
