.. _Conditional Inheritance Examples: 

--------------------------------
Conditional Inheritance Examples
--------------------------------

The JTE configuration file DSL allows you to build arbitrary nested key-value pair 
structures. 

The merging of two configuration files assumes that by default, if a key has already
been set it may not be overridden unless explictly allowed via the ``merge`` and 
``override`` flags. 

We will walk through an example of what happens when neither of these flags have been 
set for a configuration block, as well as the result of adding one of the flags to a block.

**************************************
1. Strict Governance: No Customization
**************************************

Let's say that a Governance Tier has defined the application environments to be used 
by all applications within it's purview: 

**Governance Tier Configuration File**

.. code:: 

    application_environments{
        dev{
            long_name = "Development" 
        }
        prod{
            long_name = "Production" 
        }
    }

Now let's assume an application's configuration file attempts to add their own application 
environment to the configuration: 

.. code:: 

    application_environments{
        impl{
            long_name = "Implementation"
        }
    }

In this example, the application's configuration will be ignored because it's parent configuration 
has not allowed modifications to the ``application_environments`` key 

***************************
2. Mergeable Configurations 
***************************

Now let's say the Governance Tier wanted to allow this sort of customization. 

By setting ``merge = true`` within the ``application_environments`` block, the Governance Tier is 
saying that new keys may be added to the block.  In this case - this means allowing subconfigurations 
to append their own application environments. 

**Governance Tier Configuration**:

.. code:: 

    application_environments{
        merge = true 
        dev{
            long_name = "Development" 
        }
        prod{
            long_name = "Production" 
        }
    }

**Application Configuration File**:

.. code:: 

    application_environments{
        dev{
            long_name = "Dev"
            random_new_field = 3  
        }
        impl{
            long_name = "Implementation"
        }
    }

The application configuration has tried to add a new configuration to the ``dev``
application environment as well as add their own ``impl`` environment. 

**Resultant Configuration**: 

.. code:: 

    application_environments{
        dev{
            long_name = "Development" 
        }
        impl{
            long_name = "Implementation"
        }
        prod{
            long_name = "Production" 
        }
    }

.. important:: 

    By setting ``merge = true`` the Governance Tier has allowed new keys to be added to 
    the ``application_environments`` block.  This meant that the ``impl`` application 
    environment was added as a new key but the modification to the ``dev`` environment 
    was ignored.  To allow this as well, the Governance Tier would specify ``merge = true``
    within the ``dev`` block. 

*****************************
3. Overridable Configurations
*****************************

If the Governance Tier in this example was setting application environments as 
reasonable defaults but was not interested in enforcing the use of these environments, 
they could allow application configurations to take precedence by setting ``override = true``
in the ``application_environments`` block. 

**Governance Tier Configuration**:

.. code:: 

    application_environments{
        override = true 
        dev{
            long_name = "Development" 
        }
        prod{
            long_name = "Production" 
        }
    }

**Application Configuration File**:

.. code:: 

    application_environments{
        dev{
            long_name = "Dev"
            random_new_field = 3  
        }
        impl{
            long_name = "Implementation"
        }
    }


**Resultant Configuration**:

.. code:: 

    application_environments{
        dev{
            long_name = "Dev"
            random_new_field = 3  
        }
        impl{
            long_name = "Implementation"
        }
    }


You'll notice that the resultant configuration equals that of the application's configuration. 
By setting ``override = true`` the ``application_environments`` value from the application replaced
that of the Governance Tier configuration. 
