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
import org.boozallen.plugins.jte.console.TemplateLogger
import org.boozallen.plugins.jte.utils.FileSystemWrapper
import org.boozallen.plugins.jte.utils.RunUtils
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.ClassRule
import org.jvnet.hudson.test.GroovyJenkinsRule
import org.jvnet.hudson.test.WithoutJenkins
import spock.lang.Shared
import spock.lang.Specification
import org.jenkinsci.plugins.workflow.cps.EnvActionImpl 


class TemplateEntryPointVariableSpec extends Specification {
    @Shared
    @ClassRule
    @SuppressWarnings('JUnitPublicField')
    public GroovyJenkinsRule groovyJenkinsRule = new GroovyJenkinsRule()

    def setup(){
        templateLoggerSetup()
        GroovySpy(TemplateConfigDsl, global:true)
        _ * TemplateConfigDsl.getEnvironment() >> GroovyMock(EnvActionImpl)
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

        GroovySpy(RunUtils, global: true)
        RunUtils.getJob() >> { return job }

        FileSystemWrapper fsw = GroovySpy(FileSystemWrapper, global:true)
        1 * FileSystemWrapper.createFromJob() >> {return fsw}
        1 * fsw.getFileContents(_,_,_) >> {return null}

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
