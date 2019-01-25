.. _Keywords:

--------
Keywords
--------

Keywords provide some syntatic sugar for defining variables outside of the pipeline template. 

Within the ``keywords`` block, any configuration defined will be made available within the pipeline 
template. 

****************
Default Keywords
****************

These default Keywords will be overridden if defined in a user configuration file. 

.. code:: 

    keywords{
        master  =  /^[Mm]aster$/
        develop =  /^[Dd]evelop(ment|er|)$/ 
        hotfix  =  /^[Hh]ot[Ff]ix-/ 
        release =  /^[Rr]elease-(\d+.)*\d$/
    }

The default keywords correspond to regular expressions of commonly used branch names. 
