/*
   Copyright 2018 Booz Allen Hamilton

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/


/*
    whether or not to allow the use of the 
    SCM Jenkinsfile if present. 

    Governance tiers will likely disable this to enable
    consolidated pipeline template definitions
*/
allow_scm_jenkinsfile = true 

application_environments{
    dev
    test 
    staging 
    prod 
}

keywords{
    master  =  /^[Mm]aster$/
    develop =  /^[Dd]evelop(ment|er|)$/ 
    hotfix  =  /^[Hh]ot[Ff]ix-/ 
    release =  /^[Rr]elease-(\d+.)*\d$/
}

template_methods{
    unit_test
    static_code_analysis
    build    
    scan_container_image
    penetration_test
    accessibility_compliance_test
    performance_test
    functional_test
}

steps
