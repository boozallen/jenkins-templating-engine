.. _Governance Model: 

----------------
Governance Model
----------------

One of the challenges associated with having a Jenkinsfile in every application 
source code repository is having governance within an organization over how software
gets developed. 

Each team may be doing something slightly differently and developers have access to 
bypass any required security gates by manipulating the Jenkinsfile.  

With the Jenkins Templating Engine, you're able to have a much clearer picture of 
the software delivery processes within an organization by consolidating your pipeline 
into a set of reusable templates managed within one or multiple pipeline configuration 
repositories. 

With JTE, you can configure a governance structure matching your organization's 
hierarchy through Governance Tiers. 

