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

class TemplateBindingSpec extends Specification{

    @Rule JenkinsRule jenkinsRule = new JenkinsRule()
    @Shared @ClassRule BuildWatcher bw = new BuildWatcher()

    TemplateBinding binding = new TemplateBinding() 

    /*
        dummy primitive to be used for testing 
    */
    class TestPrimitive extends TemplatePrimitive{
        void throwPreLockException(){
            throw new TemplateException ("pre-lock exception")
        }

        void throwPostLockException(){
            throw new TemplateException ("post-lock exception")
        }
    }

    /*
        dummy primitive to be used for testing 
    */
    class TestPrimitiveGetValue extends TemplatePrimitive{
        void throwPreLockException(){
            throw new TemplateException ("pre-lock exception")
        }

        void throwPostLockException(){
            throw new TemplateException ("post-lock exception")
        }

        String getValue(){
            return "dummy value" 
        }
    }

    @WithoutJenkins
    def "non-primitive variable set in binding maintains value"(){
        when: 
            binding.setVariable("x", 3)
        then: 
            assert binding.getVariable("x") == 3 
    }

    @WithoutJenkins
    def "Normal variable does not get inserted into registry"(){
        when: 
            binding.setVariable("x", 3)
        then: 
            assert !("x" in binding.registry)
    }

    @WithoutJenkins
    def "template primitive inserted into registry"(){
        when: 
            binding.setVariable("x", new TestPrimitive())
        then: 
            assert "x" in binding.registry 
    }

    @WithoutJenkins
    def "binding collision pre-lock throws pre-lock exception"(){
        when: 
            binding.setVariable("x", new TestPrimitive())
            binding.setVariable("x", 3)
        then: 
            TemplateException ex = thrown() 
            assert ex.message == "pre-lock exception"
    }

    @WithoutJenkins
    def "binding collision post-lock throws post-lock exception"(){
        when: 
            binding.setVariable("x", new TestPrimitive())
            binding.lock()
            binding.setVariable("x", 3)
        then: 
            TemplateException ex = thrown() 
            assert ex.message == "post-lock exception"
    }
    
    @WithoutJenkins
    def "missing variable throws MissingPropertyException"(){
        when: 
            binding.getVariable("doesntexist")
        then: 
            thrown MissingPropertyException
    }
    
    @WithoutJenkins
    def "getValue overrides actual value set"(){
        when: 
            binding.setVariable("x", new TestPrimitiveGetValue())
        then: 
            assert binding.getVariable("x") == "dummy value" 
    }

    @WithoutJenkins
    def "primitive with no getValue returns same object set"(){
        setup: 
            TestPrimitive test = new TestPrimitive() 
        when: 
            binding.setVariable("x", test)
        then:
            assert binding.getVariable("x") == test 
    }

    @WithoutJenkins
    def "can't overwrite library config variable"(){
        when: 
            binding.setVariable(StepWrapper.libraryConfigVariable, "test")
        then: 
            thrown(TemplateException)
    }

    def "validate TemplateBinding sets 'steps' var"(){
        given: 
            WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob, "job"); 
            job.setDefinition(new CpsFlowDefinition("""
            import org.boozallen.plugins.jte.binding.TemplateBinding
            setBinding(new TemplateBinding())
            node{
                sh "echo hello" 
            }
            """))
        expect: 
            jenkinsRule.assertLogContains("hello", jenkinsRule.buildAndAssertSuccess(job))
    }

}