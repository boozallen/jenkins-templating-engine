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

import spock.lang.Specification
import org.junit.Rule
import jenkins.plugins.git.GitSampleRepoRule
import jenkins.plugins.git.GitSCMSource
import hudson.plugins.git.GitSCM
import static hudson.model.Result.FAILURE
import static hudson.model.Result.SUCCESS

import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.WithoutJenkins

class GovernanceTierSpec extends Specification{

    @Rule JenkinsRule jenkinsRule = new JenkinsRule()
    @Rule GitSampleRepoRule sampleRepo = new GitSampleRepoRule()
    GovernanceTier tier 

    def setup(){
        // initialize repository 
        sampleRepo.init()
        sampleRepo.write("pipeline_config.groovy", """
        libraries{
            openshift{
                url = "whatever" 
            }
        }
        """)
        sampleRepo.git("add", "*")
        sampleRepo.git("commit", "--message=init")
        // create Governance Tier 
        GitSCM scm = new GitSCM(sampleRepo.fileUrl())
        String baseDir = "" 
        List<TemplateLibrarySource> librarySources = []
        tier = new GovernanceTier(scm, baseDir, librarySources)
    }

    @WithoutJenkins
    def "Validate getConfig"(){
        TemplateConfigObject config 
        
        when: 
        config = tier.getConfig() 
        then: 
        assert true
    }

}
