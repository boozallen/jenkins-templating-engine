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

import org.boozallen.plugins.jte.config.TemplateConfigObject
import org.boozallen.plugins.jte.config.TemplateGlobalConfig
import org.boozallen.plugins.jte.config.TemplateLibrarySource
import org.boozallen.plugins.jte.config.GovernanceTier
import spock.lang.* 
import spock.util.mop.ConfineMetaClassChanges
import org.junit.ClassRule
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.BuildWatcher
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import jenkins.plugins.git.GitSampleRepoRule
import hudson.plugins.git.GitSCM
import hudson.plugins.git.BranchSpec
import hudson.plugins.git.extensions.GitSCMExtension
import hudson.plugins.git.SubmoduleConfig
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import hudson.model.Result
import jenkins.scm.api.SCMFile 
import org.jenkinsci.plugins.workflow.cps.CpsScript

class StepWrapperSpec extends Specification{

    @Rule JenkinsRule jenkins = new JenkinsRule()
    @Shared @ClassRule BuildWatcher bw = new BuildWatcher()
    @Rule GitSampleRepoRule librarySource = new GitSampleRepoRule()
    GovernanceTier tier

    /*
        boiler plate to prepare JTE environment for 
        pipeline jobs 
    */
    String jenkinsfile = """ 
    import org.boozallen.plugins.jte.binding.StepWrapper
    import org.boozallen.plugins.jte.binding.TemplateBinding
    import org.boozallen.plugins.jte.config.TemplateConfigObject 

    setBinding(new TemplateBinding())
    """

    /*
        appends steps to be created directly into the binding onto 
        the Jenkinsfile to be ran as part of the test. 

        implictely tests createFromString a
    */
    void createStep(String name, String impl, Map config = [:]){
        jenkinsfile += """
        ${name} = StepWrapper.createFromString('''${impl}''', this, "${name}", "test", ${config.inspect()})
        """
    }

    /*
        returns a CpsFlowDefinition based upon the aggregated Jenkinsfile 
        created by the boiler plate, step creations, and passed pipeline (s). 
    */
    CpsFlowDefinition createFlowDefinition(String s){
        jenkinsfile += s 
        CpsFlowDefinition f = new CpsFlowDefinition(jenkinsfile, false)
        println f.getScript()
        return f 
    }

    @ConfineMetaClassChanges([StepWrapper])
    def "Validate createFromFile properly calles createFromString"(){
        setup: 
            String stepName = "test_step" 
            String stepText = "def call(){ println 'test' }"
            CpsScript script = GroovyMock(CpsScript) 
            
            SCMFile file = GroovyMock(SCMFile)
            _ * file.getName() >> "${stepName}.groovy"
            _ * file.contentAsString() >> stepText

            /*
                admittedly a weird way to test this.
                having problem mocking static call within createFromFile 
                to createFromString. 

                This overrides static createFromString and asserts the expected
                arguments were passed. 

                createFromString itself gets tested by every other test 
                in this Specification
            */
            StepWrapper.metaClass.static.createFromString = { String t, CpsScript s, String n, String l, Map m ->
                assert t == stepText 
                assert s == script 
                assert n == stepName 
                assert l == "test"
                assert m == [:]
            }

        when:     
            StepWrapper.createFromFile(file, "test", script, [:])
        then: 
            assert true  // need an assertion in then
    }

    def "steps invocable via call shorthand with no params"(){
        when: 
            createStep("test_step", """
            def call(){ 
                println "test"
            }
            """)
            WorkflowJob job = jenkins.createProject(WorkflowJob, "job"); 
            job.setDefinition(createFlowDefinition("test_step()"))
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
            job.setDefinition(createFlowDefinition("test_step('message')"))
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
            job.setDefinition(createFlowDefinition("test_step('message', 3)"))
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
            job.setDefinition(createFlowDefinition("test_step.other()"))
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
            job.setDefinition(createFlowDefinition("test_step.other('example')"))
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
            job.setDefinition(createFlowDefinition("test_step.other('example1', 'example2')"))
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
            """, [random: "random", eleven: 11] )
            WorkflowJob job = jenkins.createProject(WorkflowJob, "job"); 
            job.setDefinition(createFlowDefinition("test_step()"))
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
            job.setDefinition(createFlowDefinition("test_step()"))
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
            job.setDefinition(createFlowDefinition("""
                def r = test_step()
                println "return is \${r}"
            """))
        then: 
            jenkins.assertLogContains("return is example", jenkins.buildAndAssertSuccess(job))
    }

    def "step method not found throws TemplateException"(){
        when: 
            createStep("test_step", "def call(){ println 'blah' }")
            WorkflowJob job = jenkins.createProject(WorkflowJob, "job"); 
            job.setDefinition(createFlowDefinition("test_step.nonexistent()"))
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
            job.setDefinition(createFlowDefinition("test_step()"))
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
            job.setDefinition(createFlowDefinition("test_step()"))
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
            job.setDefinition(createFlowDefinition("test_step()"))
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