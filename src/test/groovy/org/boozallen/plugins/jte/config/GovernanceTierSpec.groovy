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

package org.boozallen.plugins.jte.config

import org.boozallen.plugins.jte.Utils 
import spock.lang.* 
import org.junit.Rule
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import jenkins.plugins.git.GitSampleRepoRule
import jenkins.plugins.git.GitSCMSource
import hudson.plugins.git.GitSCM
import hudson.plugins.git.BranchSpec
import hudson.plugins.git.extensions.GitSCMExtension
import hudson.plugins.git.SubmoduleConfig
import static hudson.model.Result.FAILURE
import static hudson.model.Result.SUCCESS

import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.WithoutJenkins

class GovernanceTierSpec extends Specification{

    @Rule JenkinsRule jenkinsRule = new JenkinsRule()
    @Rule GitSampleRepoRule sampleRepo = new GitSampleRepoRule()
    GovernanceTier tier1
    GovernanceTier tier2 

    def setup(){
        // initialize repository 
        sampleRepo.init()
        
        // write tier 1 files
        sampleRepo.write("pipeline_config.groovy", """
        tier1 = true
        """)
        sampleRepo.write("Jenkinsfile", """
        Jenkinsfile 1 
        """)
        sampleRepo.write("pipeline_templates/test", """
        test template 1
        """)
        

        // write tier 2 files 
        String baseDir2 = "suborg"
        sampleRepo.write("${baseDir2}/pipeline_config.groovy", """
        tier2 = true
        """)
        sampleRepo.write("${baseDir2}/Jenkinsfile", """
        Jenkinsfile 2 
        """)
        sampleRepo.write("${baseDir2}/pipeline_templates/test", """
        test template 2
        """)

        // commit files 
        sampleRepo.git("add", "*")
        sampleRepo.git("commit", "--message=init")
        
        // create common SCM  
        GitSCM scm = new GitSCM(
            GitSCM.createRepoList(sampleRepo.toString(), null), 
            Collections.singletonList(new BranchSpec("*/master")), 
            false, 
            Collections.<SubmoduleConfig>emptyList(), 
            null, 
            null, 
            Collections.<GitSCMExtension>emptyList()
        )

        // create tier 1 
        List<TemplateLibrarySource> librarySources1 = []
        tier1 = new GovernanceTier(scm, "", librarySources1)

        // create tier 2 
        List<TemplateLibrarySource> librarySources2 = [] 
        tier2 = new GovernanceTier(scm, baseDir2, librarySources2)

        // mock Utils getCurrentJob
        WorkflowJob currentJob = jenkinsRule.jenkins.createProject(WorkflowJob.class, "job"); 
        def utilsMock = GroovySpy(Utils, global: true)
        _ * Utils.getCurrentJob() >> currentJob 
    }

    // test baseDir is root of repository 
    def "Get root level pipeline_config.groovy as TemplateConfigObject"(){
        setup: 
            def config  

        when:
            config = tier1.getConfig()
        
        then: 
            assert config instanceof TemplateConfigObject
            assert config.getConfig().tier1 
    }

    def "Get root level Jenkinsfile"(){
        setup: 
            def jenkinsfile  

        when:
            jenkinsfile = tier1.getJenkinsfile()
        
        then: 
            assert jenkinsfile instanceof String
            assert jenkinsfile.contains("Jenkinsfile 1")
    }

    def "Get pipeline template 'test'"(){
        setup: 
            def jenkinsfile  

        when:
            jenkinsfile = tier1.getTemplate("test")
        
        then: 
            assert jenkinsfile instanceof String
            assert jenkinsfile.contains("test template 1")
    }

    // test basedir is nested 
    def "Get nested level pipeline_config.groovy as TemplateConfigObject"(){
        setup: 
            def config  

        when:
            config = tier2.getConfig()
        
        then: 
            assert config instanceof TemplateConfigObject
            assert config.getConfig().tier2
    }

    def "Get nested level Jenkinsfile"(){        
        setup: 
            def jenkinsfile  

        when:
            jenkinsfile = tier2.getJenkinsfile()
        
        then: 
            assert jenkinsfile instanceof String
            assert jenkinsfile.contains("Jenkinsfile 2")
    }

    def "Get nested pipeline template 'test'"(){
        setup: 
            def jenkinsfile  

        when:
            jenkinsfile = tier2.getTemplate("test")
        
        then: 
            assert jenkinsfile instanceof String
            assert jenkinsfile.contains("test template 2")
    }

}