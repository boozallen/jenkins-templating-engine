.. _library-selection:

-----------------
Library Selection 
-----------------
Each Governance Tier can have a list of Library Sources defined, with each Library Source containing 1 or 
more JTE pipeline libraries. 

When a library is specified in a configuration file, JTE will start with the most specific Governance Tier 
and search each Library Source from first to last looking for the library.  If none of the Library Sources 
in the first Governance Tier has the library, JTE will then look in the parent Governance Tier.  This pattern 
is followed until all Library Sources have been inspected for the library. 

.. important:: 

    If a library is specified and not present in any of the Governance Tier's Library Sources, JTE will throw 
    a Library Not Found Exception and fail the build. 