/*
    whether or not to allow the use of the 
    SCM Jenkinsfile if present. 

    Governance tiers will likely disable this to enable
    consolidated pipeline template definitions
*/
allow_scm_jenkinsfile = true 

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
