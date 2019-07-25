.. _configuration-file-aggregation:

------------------------------
Configuration File Aggregation
------------------------------

When creating Governance Tiers to define your governance hierarchy, you can create configuration files to 
consolidate common configurations while tuning which aspects can be modified or overwritten by suborganizations
in the hierarchy. 

JTE provides some `common configurations <https://raw.githubusercontent.com/jenkinsci/templating-engine-plugin/master/src/main/resources/org/boozallen/plugins/jte/config/pipeline_config.groovy>`_ 
by default.  The first configuration file found in the hierarchy will automatically override keys in common between 
the default configuration and the configuration file being loaded. 

After the first configuration file, each subsequent file will only be able to **add new root configuration keys** and 
any configuration that overlaps with the existing configuration will be ignored **unless** the parent configuration 
has explicitly allowed a configuration block to be modified via the ``merge`` and ``override`` keys.

Setting ``merge = true`` in a configuration block will allow the next configuration file in the hierarchy to append 
configuration keys to the block. 

Setting ``override = true`` in a configuration block will allow the next configuration file to replace the existing
configuration block. 

You can see some examples of these keywords in action :ref:`here <Conditional Inheritance Examples>`. 

Each configuration file in the Governance Tier hierarchy will be combined according to these rules until there 
is a resultant aggregated pipeline configuration. It is this aggregated pipeline configuration that gets used 
to populate the selected pipeline template for execution. 