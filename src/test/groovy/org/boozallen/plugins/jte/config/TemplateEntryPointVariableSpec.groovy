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


import org.boozallen.plugins.jte.TemplateEntryPointVariable
import org.boozallen.plugins.jte.Utils
import org.boozallen.plugins.jte.console.TemplateLogger
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.ClassRule
import org.jvnet.hudson.test.GroovyJenkinsRule
import org.jvnet.hudson.test.WithoutJenkins
import spock.lang.Shared
import spock.lang.Specification

class TemplateEntryPointVariableSpec extends Specification {
    @Shared
    @ClassRule
    @SuppressWarnings('JUnitPublicField')
    public GroovyJenkinsRule groovyJenkinsRule = new GroovyJenkinsRule()

    def setupSpec(){

    }

    def setup(){
        templateLoggerSetup()
    }

    @WithoutJenkins
    def 'aggregateTemplateConfigurations with empty tiers on empty pipeline config'(){
        setup:
        TemplateEntryPointVariable t = new TemplateEntryPointVariable()
        PipelineConfig p = new PipelineConfig(TemplateConfigDsl.parse(""))

        def configs = [""" """, """ """, """ """]

        def tiers = configs.collect{ c ->
            GovernanceTier g = GroovyMock(GovernanceTier)
            def tc = TemplateConfigDsl.parse(c)
            g.config >> { return tc }
            g
        }

        GroovySpy(GovernanceTier, global:true)
        GovernanceTier.getHierarchy() >> { return tiers }

        WorkflowJob job = groovyJenkinsRule.jenkins.createProject(WorkflowJob,"aggregateTemplateConfigurations.1")

        GroovySpy(Utils, global: true)
        Utils.getLogger() >> { return System.out }
        Utils.getCurrentJob() >> { return job }


        when:
        t.aggregateTemplateConfigurations(p)
        TemplateConfigObject configObject = p.config

        then:
        configObject.config == [:]
        configObject.merge.isEmpty()
        configObject.override.isEmpty()
    }

    void templateLoggerSetup(){
        GroovySpy(TemplateLogger, global: true)
        TemplateLogger.print(_, _) >> { s, c -> return System.out.print(s) }
    }

}
