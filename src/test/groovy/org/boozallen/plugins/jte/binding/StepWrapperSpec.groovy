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

package org.boozallen.plugins.jte.binding


import org.boozallen.plugins.jte.Utils
import org.boozallen.plugins.jte.config.TemplateConfigObject
import org.boozallen.plugins.jte.config.TemplateGlobalConfig
import org.boozallen.plugins.jte.config.TemplateLibrarySource
import org.boozallen.plugins.jte.config.GovernanceTier
import spock.lang.* 
import org.junit.ClassRule
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.BuildWatcher
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import com.cloudbees.hudson.plugins.folder.Folder
import jenkins.plugins.git.GitSampleRepoRule
import hudson.plugins.git.GitSCM
import hudson.plugins.git.BranchSpec
import hudson.plugins.git.extensions.GitSCMExtension
import hudson.plugins.git.SubmoduleConfig
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import hudson.model.Result;

class StepWrapperSpec extends Specification{

    @Rule JenkinsRule jenkins = new JenkinsRule()
    @Shared @ClassRule BuildWatcher bw = new BuildWatcher()
    @Rule GitSampleRepoRule librarySource = new GitSampleRepoRule()
    GovernanceTier tier

    /*
        initial pipeline script that'll load the 
        test library and initialize the binding. 
    */
    String pipelineInit = """
        import org.boozallen.plugins.jte.binding.LibraryLoader
        import org.boozallen.plugins.jte.binding.TemplateBinding
        import org.boozallen.plugins.jte.config.TemplateConfigObject 

        setBinding(new TemplateBinding())
        pipelineConfig = new TemplateConfigObject(config: [
            libraries: [
                test: [
                    random: "random",
                    eleven: 11 
                ]
            ]
        ])

        LibraryLoader.doInject(pipelineConfig, this)
    """

    def setup(){
        /*
            create library "test" with step "test_step" 
        */
        librarySource.init()
      
        GitSCM scm = new GitSCM(
            GitSCM.createRepoList(librarySource.toString(), null), 
            Collections.singletonList(new BranchSpec("*/master")), 
            false, 
            Collections.<SubmoduleConfig>emptyList(), 
            null, 
            null, 
            Collections.<GitSCMExtension>emptyList()
        )

        /*
            create global governance tier registering library source 
        */
        List<TemplateLibrarySource> librarySources = [ new TemplateLibrarySource(scm: scm) ]
        tier = new GovernanceTier(null, "", librarySources)            
        TemplateGlobalConfig global = TemplateGlobalConfig.get() 
        global.setTier(tier) 

    }

    void createStep(String name, String impl){
        librarySource.write("test/${name}.groovy", impl)
        librarySource.git("add", "*")
        librarySource.git("commit", "--message=init")
    }

    def "steps invocable via call shorthand with no params"(){
        when: 
            createStep("test_step", """
            void call(){
                println "test" 
            }
            """)
            WorkflowJob job = jenkins.createProject(WorkflowJob, "job"); 
            job.setDefinition(new CpsFlowDefinition(""" ${pipelineInit}
                test_step()
            """, false))
        then: 
            jenkins.assertLogContains("test", jenkins.buildAndAssertSuccess(job)) 
    }

    def "steps invocable via call shorthand with one param"(){
        when: 
            createStep("test_step", """
            void call(String msg){
                println "msg -> \${msg}" 
            }
            """)
            WorkflowJob job = jenkins.createProject(WorkflowJob, "job"); 
            job.setDefinition(new CpsFlowDefinition(""" ${pipelineInit}
                test_step("message")
            """, false))
        then: 
            jenkins.assertLogContains("msg -> message", jenkins.buildAndAssertSuccess(job)) 
    }

    def "steps invocable via call shorthand with more than one param"(){
        when: 
            createStep("test_step", """
            void call(String msg, Integer i){
                println "msg -> \${msg} + \${i}" 
            }
            """)
            WorkflowJob job = jenkins.createProject(WorkflowJob, "job"); 
            job.setDefinition(new CpsFlowDefinition(""" ${pipelineInit}
                test_step("message", 3)
            """, false))
        then: 
            jenkins.assertLogContains("msg -> message + 3", jenkins.buildAndAssertSuccess(job)) 
    }

    def "steps can invoke non-call methods with no params"(){
        when: 
            createStep("test_step", """
            void other(){
                println "other" 
            }
            """)
            WorkflowJob job = jenkins.createProject(WorkflowJob, "job"); 
            job.setDefinition(new CpsFlowDefinition(""" ${pipelineInit}
                test_step.other()
            """, false))
        then: 
            jenkins.assertLogContains("other", jenkins.buildAndAssertSuccess(job)) 
    }

    def "steps can invoke non-call methods with 1 param"(){
        when: 
            createStep("test_step", """
            void other(String msg){
                println "message is \${msg}" 
            }
            """)
            WorkflowJob job = jenkins.createProject(WorkflowJob, "job"); 
            job.setDefinition(new CpsFlowDefinition(""" ${pipelineInit}
                test_step.other("example")
            """, false))
        then: 
            jenkins.assertLogContains("message is example", jenkins.buildAndAssertSuccess(job)) 
    }

    def "steps can invoke non-call methods with more than 1 param"(){
        when: 
            createStep("test_step", """
            void other(String msg, String msg2){
                println "message is \${msg} + \${msg2}" 
            }
            """)
            WorkflowJob job = jenkins.createProject(WorkflowJob, "job"); 
            job.setDefinition(new CpsFlowDefinition(""" ${pipelineInit}
                test_step.other("example1", "example2")
            """, false))
        then: 
            jenkins.assertLogContains("message is example1 + example2", jenkins.buildAndAssertSuccess(job)) 
    }

    def "steps can access configuration via config variable"(){
        when: 
            createStep("test_step", """
            void call(){
                assert ${StepWrapper.libraryConfigVariable} == [ 
                    random: "random", 
                    eleven: 11
                ] 
                println "${StepWrapper.libraryConfigVariable}.random -> '\${${StepWrapper.libraryConfigVariable}.random}'"
            }
            """)
            WorkflowJob job = jenkins.createProject(WorkflowJob, "job"); 
            job.setDefinition(new CpsFlowDefinition(""" ${pipelineInit}
                test_step()
            """, false))
        then: 
            jenkins.assertLogContains("config.random -> 'random'", jenkins.buildAndAssertSuccess(job))
    }

    def "steps can invoke pipeline steps directly"(){
        when: 
        createStep("test_step", """
        void call(){
            node{
                sh "echo hello"
            }
        }
        """)
        WorkflowJob job = jenkins.createProject(WorkflowJob, "job"); 
        job.setDefinition(new CpsFlowDefinition(""" ${pipelineInit}
            test_step()
        """, false))
        then: 
            jenkins.assertLogContains("hello", jenkins.buildAndAssertSuccess(job))
    }

    def "return step return result through StepWrapper"(){
        when: 
            createStep("test_step", """
            def call(){
                return "example"
            }
            """)
            WorkflowJob job = jenkins.createProject(WorkflowJob, "job"); 
            job.setDefinition(new CpsFlowDefinition(""" ${pipelineInit}
                def r = test_step()
                println "return is \${r}"
            """, false))
        then: 
            jenkins.assertLogContains("return is example", jenkins.buildAndAssertSuccess(job))
    }

    def "step method not found throws TemplateException"(){
        when: 
            createStep("test_step", "def call(){ println 'blah' }")
            WorkflowJob job = jenkins.createProject(WorkflowJob, "job"); 
            job.setDefinition(new CpsFlowDefinition(""" ${pipelineInit}
                test_step.nonexistent() 
            """, false))
            def build = job.scheduleBuild2(0).get() 
        then: 
            jenkins.assertBuildStatus(Result.FAILURE, build)
            jenkins.assertLogContains("TemplateException: Step test_step from the library test does not have the method nonexistent()", build)
    }

    def "step override during initialization throws exception"(){
        when: 
            TemplateBinding binding = new TemplateBinding()
            StepWrapper s = new StepWrapper(name: "test", library: "testlib")
            binding.setVariable("test", s) 
            binding.setVariable("test", "whatever")
                
        then: 
            TemplateException ex = thrown() 
            ex.message == "Library Step Collision. The step test already defined via the testlib library."
    }

    def "step override post initialization throws exception"(){
        when: 
            TemplateBinding binding = new TemplateBinding()
            StepWrapper s = new StepWrapper(name: "test", library: "testlib")
            binding.setVariable("test", s) 
            binding.lock()
            binding.setVariable("test", "whatever")
                
        then: 
            TemplateException ex = thrown() 
            ex.message == "Library Step Collision. The variable test is reserved as a library step via the testlib library."
    }

    def "@BeforeStep executes before step"(){
        when: 
            createStep("test_step", "def call(){ println 'testing' }")
            createStep("test_step2", """
            @BeforeStep
            def call(Map Context){ 
                println "before!" 
            }
            """)
            WorkflowJob job = jenkins.createProject(WorkflowJob, "job"); 
            job.setDefinition(new CpsFlowDefinition(""" ${pipelineInit}
                test_step()
            """, false))
            def build = job.scheduleBuild2(0).get() 
            def bN, sN 
            jenkins.getLog(build).eachLine{ line, N -> 
                if (line.contains("before!")) bN = N 
                if (line.contains("testing")) sN = N 
            }
        then: 
            jenkins.assertBuildStatusSuccess(build)
            jenkins.assertLogContains("before!", build)
            jenkins.assertLogContains("testing", build)
            assert bN < sN 
    } 

    def "@AfterStep executes after step"(){
        when: 
            createStep("test_step", "def call(){ println 'testing' }")
            createStep("test_step2", """
            @AfterStep
            def call(Map Context){ 
                println "after!" 
            }
            """)
            WorkflowJob job = jenkins.createProject(WorkflowJob, "job"); 
            job.setDefinition(new CpsFlowDefinition(""" ${pipelineInit}
                test_step()
            """, false))
            def build = job.scheduleBuild2(0).get() 
            def aN, sN 
            jenkins.getLog(build).eachLine{ line, N -> 
                if (line.contains("testing")) sN = N 
                if (line.contains("after!")) aN = N
            }
        then: 
            jenkins.assertBuildStatusSuccess(build)
            jenkins.assertLogContains("testing", build)
            jenkins.assertLogContains("after!", build)
            assert sN < aN 
    }

    def "@Notify executes after step"(){
        when: 
            createStep("test_step", "def call(){ println 'testing' }")
            createStep("test_step2", """
            @Notify
            def call(Map Context){ 
                println "notify!" 
            }
            """)
            WorkflowJob job = jenkins.createProject(WorkflowJob, "job"); 
            job.setDefinition(new CpsFlowDefinition(""" ${pipelineInit}
                test_step()
            """, false))
            def build = job.scheduleBuild2(0).get() 
            def nN, sN 
            jenkins.getLog(build).eachLine{ line, N -> 
                if (line.contains("testing")) sN = N 
                if (line.contains("notify!")) nN = N
            }
        then: 
            jenkins.assertBuildStatusSuccess(build)
            jenkins.assertLogContains("testing", build)
            jenkins.assertLogContains("notify!", build)
            assert sN < nN 
    }

}