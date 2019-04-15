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

import spock.lang.* 
import org.junit.*
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.BuildWatcher
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jvnet.hudson.test.WithoutJenkins

class StageSpec extends Specification{

    @Rule JenkinsRule jenkinsRule = new JenkinsRule()
    @Shared @ClassRule BuildWatcher bw = new BuildWatcher()

    def "validate stage executes single step"(){
        given: 
            WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob, "job"); 
            job.setDefinition(new CpsFlowDefinition("""
            import org.boozallen.plugins.jte.binding.TemplateBinding
            import org.boozallen.plugins.jte.binding.Stage
            import org.boozallen.plugins.jte.config.TemplateConfigObject
            
            /*
                initialize
            */
            setBinding(new TemplateBinding())
            a = { println "running step A" }
            TemplateConfigObject config = new TemplateConfigObject(config: [
                stages: [
                    test_stage: [
                        a: [:]
                    ]
                ]
            ])            
            Stage.Injector.doInject(config, this)
            
            // run stage 
            test_stage()

            """, false))
            def build =  jenkinsRule.buildAndAssertSuccess(job)
        expect: 
            jenkinsRule.assertLogContains("running step A", build)
    }

    def "validate stage executes multple steps"(){
        given: 
            WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob, "job"); 
            job.setDefinition(new CpsFlowDefinition("""
            import org.boozallen.plugins.jte.binding.TemplateBinding
            import org.boozallen.plugins.jte.binding.Stage
            import org.boozallen.plugins.jte.config.TemplateConfigObject
            
            /*
                initialize
            */
            setBinding(new TemplateBinding())
            a = { println "running step A" }
            b = { println "running step B"}
            TemplateConfigObject config = new TemplateConfigObject(config: [
                stages: [
                    test_stage: [
                        a: [:],
                        b: [:]
                    ]
                ]
            ])            
            Stage.Injector.doInject(config, this)
            
            // run stage 
            test_stage()

            """, false))
            def build =  jenkinsRule.buildAndAssertSuccess(job)
        expect: 
            jenkinsRule.assertLogContains("running step A", build)
            jenkinsRule.assertLogContains("running step B", build)
    }

    def "validate stage executes steps in order"(){
        given: 
            WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob, "job"); 
            job.setDefinition(new CpsFlowDefinition("""
            import org.boozallen.plugins.jte.binding.TemplateBinding
            import org.boozallen.plugins.jte.binding.Stage
            import org.boozallen.plugins.jte.config.TemplateConfigObject
            
            /*
                initialize
            */
            setBinding(new TemplateBinding())
            a = { println "running step A" }
            b = { println "running step B"}
            TemplateConfigObject config = new TemplateConfigObject(config: [
                stages: [
                    test_stage: [
                        a: [:],
                        b: [:]
                    ]
                ]
            ])            
            Stage.Injector.doInject(config, this)
            
            // run stage 
            test_stage()

            """, false))
            def build =  jenkinsRule.buildAndAssertSuccess(job)
            String log = jenkinsRule.getLog(build)
            def aN, bN
            log.eachLine{ line, count -> 
                if (line.contains("running step A")) aN = count
                if (line.contains("running step B")) bN = count
            }
        expect: 
            assert aN < bN 
    }

    @WithoutJenkins
    def "validate override during initialization throws exception"(){
        when: 
            TemplateBinding binding = new TemplateBinding() 
            binding.setVariable("a", new Stage(name: "a"))
            binding.setVariable("a", 1)
        then: 
            TemplateException ex = thrown() 
            assert ex.message == "The Stage a is already defined."
    }

    @WithoutJenkins
    def "validate override post initialization throws exception"(){
        when: 
            TemplateBinding binding = new TemplateBinding() 
            binding.setVariable("a", new Stage(name: "a"))
            binding.lock()
            binding.setVariable("a", 1)
        then: 
            TemplateException ex = thrown() 
            assert ex.message == "The variable a is reserved as a template Stage."
    }

}