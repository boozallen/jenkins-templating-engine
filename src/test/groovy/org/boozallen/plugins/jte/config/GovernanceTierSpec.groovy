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

import spock.lang.* 
import org.junit.Rule
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import com.cloudbees.hudson.plugins.folder.Folder
import jenkins.plugins.git.GitSampleRepoRule
import hudson.plugins.git.GitSCM
import hudson.plugins.git.BranchSpec
import hudson.plugins.git.extensions.GitSCMExtension
import hudson.plugins.git.SubmoduleConfig

class GovernanceTierSpec extends Specification{

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()
    @Rule GitSampleRepoRule sampleRepo = new GitSampleRepoRule()
    GovernanceTier tier1
    GovernanceTier tier2 

    def setup(){
        // initialize repository 
        sampleRepo.init()
        
        // write tier 1 files
        sampleRepo.write("pipeline_config.groovy", "tier1 = true")
        sampleRepo.write("Jenkinsfile", "Jenkinsfile 1")
        sampleRepo.write("pipeline_templates/test", "test template 1")
        

        // write tier 2 files 
        String baseDir2 = "suborg"
        sampleRepo.write("${baseDir2}/pipeline_config.groovy", "tier2 = true")
        sampleRepo.write("${baseDir2}/Jenkinsfile", "Jenkinsfile 2")
        sampleRepo.write("${baseDir2}/pipeline_templates/test", "test template 2")

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
    }

    // test baseDir is root of repository 
    def "Get Config: root base directory"(){
        given: 
            WorkflowJob currentJob = jenkins.createProject(WorkflowJob); 
            GroovySpy(Utils, global: true)
            _ * Utils.getCurrentJob() >> currentJob 
            def config  
        when:
            config = tier1.getConfig()
        then: 
            assert config instanceof TemplateConfigObject
            assert config.getConfig().tier1 
    }

    def "Get Jenkinsfile: root base directory"(){
        given: 
            WorkflowJob currentJob = jenkins.createProject(WorkflowJob); 
            GroovySpy(Utils, global: true)
            _ * Utils.getCurrentJob() >> currentJob 
            def jenkinsfile 
        when:
            jenkinsfile = tier1.getJenkinsfile()
        then: 
            assert jenkinsfile instanceof String
            assert jenkinsfile.contains("Jenkinsfile 1")
    }

    def "Get Pipeline Template: root base directory"(){
        given: 
            WorkflowJob currentJob = jenkins.createProject(WorkflowJob); 
            GroovySpy(Utils, global: true)
            _ * Utils.getCurrentJob() >> currentJob 
            def jenkinsfile  
        when:
            jenkinsfile = tier1.getTemplate("test")
        then: 
            assert jenkinsfile instanceof String
            assert jenkinsfile.contains("test template 1")
    }

    // test basedir is nested 
    def "Get Config: nested base directory"(){
        given: 
            WorkflowJob currentJob = jenkins.createProject(WorkflowJob); 
            GroovySpy(Utils, global: true)
            _ * Utils.getCurrentJob() >> currentJob 
            def config 
        when:
            config = tier2.getConfig()
        
        then: 
            assert config instanceof TemplateConfigObject
            assert config.getConfig().tier2
    }

    def "Get Jenkinsfile: nested base directory"(){        
        given: 
            WorkflowJob currentJob = jenkins.createProject(WorkflowJob); 
            GroovySpy(Utils, global: true)
            _ * Utils.getCurrentJob() >> currentJob 
            def jenkinsfile
        when:
            jenkinsfile = tier2.getJenkinsfile()
        then: 
            assert jenkinsfile instanceof String
            assert jenkinsfile.contains("Jenkinsfile 2")
    }

    def "Get Pipeline Template: nested base directory"(){
        given: 
            WorkflowJob currentJob = jenkins.createProject(WorkflowJob); 
            GroovySpy(Utils, global: true)
            _ * Utils.getCurrentJob() >> currentJob 
            def jenkinsfile  
        when:
            jenkinsfile = tier2.getTemplate("test")
        then: 
            assert jenkinsfile instanceof String
            assert jenkinsfile.contains("test template 2")
    }

    
    def "Get Governance Hierarchy: no hierarchy"(){
        given: 
            WorkflowJob currentJob = jenkins.createProject(WorkflowJob); 
            GroovySpy(Utils, global: true)
            _ * Utils.getCurrentJob() >> currentJob 
            def list 
        when: 
            list = GovernanceTier.getHierarchy()
        then: 
            assert list instanceof List<GovernanceTier> 
            assert list.isEmpty()
    }

    def "Get Governance Hierarchy: 1 deep folder structure, no global config"(){
        given:             
            // setup job hierarchy 
            Folder folder = jenkins.createProject(Folder, jenkins.createUniqueProjectName())
            TemplateConfigFolderProperty prop = new TemplateConfigFolderProperty(tier1) 
            folder.getProperties().add(prop) 

            WorkflowJob currentJob = folder.createProject(WorkflowJob, jenkins.createUniqueProjectName()); 
            GroovySpy(Utils, global: true)
            _ * Utils.getCurrentJob() >> currentJob 

            // define expected result 
            def list 
            List<GovernanceTier> expectedResult = [ tier1 ]
        when: 
            list = GovernanceTier.getHierarchy()
        then: 
            assert list instanceof List<GovernanceTier> 
            assert list == expectedResult 
    }

    def "Get Governance Hierarchy: 2 deep folder structure, no global config"(){
        given: "a job within a nested folder structure" 
            // setup job hierarchy 
            Folder folder1 = jenkins.createProject(Folder, jenkins.createUniqueProjectName())
            TemplateConfigFolderProperty prop1 = new TemplateConfigFolderProperty(tier1) 
            folder1.getProperties().add(prop1)

            Folder folder2 = folder1.createProject(Folder, jenkins.createUniqueProjectName())
            TemplateConfigFolderProperty prop2 = new TemplateConfigFolderProperty(tier2)
            folder2.getProperties().add(prop2)

            WorkflowJob currentJob = folder2.createProject(WorkflowJob, jenkins.createUniqueProjectName()); 
            GroovySpy(Utils, global: true)
            _ * Utils.getCurrentJob() >> currentJob 

            // define expected result
            def list  
            List<GovernanceTier> expectedResult = [ tier2, tier1 ]

        when: "I get the Governance Tier Hierarchy"
            list = GovernanceTier.getHierarchy()

        then: "The hierarchy listed matches the folder structure hierarchy"
            assert list instanceof List<GovernanceTier> 
            assert list == expectedResult 
    }

    def "Get Governance Hierarchy: folder with no tier"(){
        given: "a job within a nested folder structure" 
            // setup job hierarchy 
            Folder folder1 = jenkins.createProject(Folder, jenkins.createUniqueProjectName())
            Folder folder2 = folder1.createProject(Folder, jenkins.createUniqueProjectName())
            TemplateConfigFolderProperty prop2 = new TemplateConfigFolderProperty(tier1)
            folder2.getProperties().add(prop2)

            WorkflowJob currentJob = folder2.createProject(WorkflowJob, jenkins.createUniqueProjectName()); 
            GroovySpy(Utils, global: true)
            _ * Utils.getCurrentJob() >> currentJob 

            // define expected result
            def list  
            List<GovernanceTier> expectedResult = [ tier1 ]

        when: "I get the Governance Tier Hierarchy"
            list = GovernanceTier.getHierarchy()

        then: "The hierarchy listed matches the folder structure hierarchy"
            assert list instanceof List<GovernanceTier> 
            assert list == expectedResult 
    }


    def "Get Governance Hierarchy: global config"(){
        given: 
            TemplateGlobalConfig global = TemplateGlobalConfig.get() 
            global.setTier(tier1) 

            WorkflowJob currentJob = jenkins.createProject(WorkflowJob); 
            GroovySpy(Utils, global: true)
            _ * Utils.getCurrentJob() >> currentJob 
            
            // define expected result 
            def list  
            List<GovernanceTier> expectedResult = [ tier1 ]
        when: 
            list = GovernanceTier.getHierarchy()
        then: 
            assert list instanceof List<GovernanceTier> 
            assert list == expectedResult 
    }

    def "Get Governance Hierarchy: 1 deep folder structure, global config"(){
        given: 
            TemplateGlobalConfig global = TemplateGlobalConfig.get() 
            global.setTier(tier1) 

            // setup job hierarchy 
            Folder folder = jenkins.createProject(Folder, jenkins.createUniqueProjectName())
            TemplateConfigFolderProperty prop = new TemplateConfigFolderProperty(tier2) 
            folder.getProperties().add(prop) 

            WorkflowJob currentJob = folder.createProject(WorkflowJob, jenkins.createUniqueProjectName()); 
            GroovySpy(Utils, global: true)
            _ * Utils.getCurrentJob() >> currentJob 
            
            // define expected result 
            def list  
            List<GovernanceTier> expectedResult = [ tier2, tier1 ]
        when: 
            list = GovernanceTier.getHierarchy()
        then: 
            assert list instanceof List<GovernanceTier> 
            assert list == expectedResult 
    }

    

}