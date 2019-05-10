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

    @Shared
    public String basePipelineConfig = null

    @Shared
    public def basePipelineConfigMap = null

    def setupSpec(){
        basePipelineConfig = PipelineConfig.baseConfigContentsFromLoader(groovyJenkinsRule.jenkins.getPluginManager()
                .uberClassLoader)

        basePipelineConfigMap = TemplateConfigDsl.parse(basePipelineConfig).config
    }

    def setup(){
        // this caused other test classes to fail
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


    def 'Join empty TemplateConfigObjects to PipelineConfig'(){
        setup:

        PipelineConfig p = new PipelineConfig(TemplateConfigDsl.parse(""))

        def configs = [""" """, """ """, """ """]

        configs.each{ c ->
            TemplateConfigObject config = TemplateConfigDsl.parse(c)
            if (config){
                p.join(config)
            }
        }

        when:
        TemplateConfigObject configObject = p.config

        then:
        configObject.config == [:]
        configObject.merge.isEmpty()
        configObject.override.isEmpty()
    }

    def 'Flat Keys Configuration'(){
        setup:

        String c1 = """
            a = 3
            b = "hi" 
            c = true 
            """

        when:
        TemplateConfigObject configObject = combine(c1)

        then:
        configObject.config == (basePipelineConfigMap + [
                a: 3,
                b: "hi",
                c: true
        ])
        configObject.merge.isEmpty()
        configObject.override.isEmpty()
    }

    def 'Keys 2 tiers Configuration'(){
        setup:

        String c1 = """
            a = 3
            b = "hi" 
            c = true 
            """

        String c2 = """
            a = 4
            """
        when:
        TemplateConfigObject configObject = combine(c1, c2)

        then:
        configObject.config.equals(basePipelineConfigMap + [
                a: 3,
                b: "hi",
                c: true
        ])
        configObject.merge.isEmpty()
        configObject.override.isEmpty()
    }

    def 'Keys with 2 tier override Configuration'(){
        setup:

        String c1 = """
            application_environments{ 
            dev {
              override = true
              long_name = 'Dev'
            }
            }
            a = 3
            b = "hi" 
            c = true 
            """

        String c2 = """
            a = 4
            application_environments{ 
            dev {
              long_name = 'Develop'
            }
            }
            """
        when:
        TemplateConfigObject configObject = combine(c1, c2)

        then:// while the override occurs, the final result has not overrides
        configObject.config == (basePipelineConfigMap + [
                application_environments:[
                dev:[long_name:'Develop']
                ],
                a: 3,
                b: "hi",
                c: true
        ])
        configObject.merge.isEmpty()
        configObject.override.isEmpty()
    }

    def 'Keys with 3 tier override Configuration'(){
        setup:

        String c1 = """
            application_environments{ 
            dev {
              override = true
              long_name = 'Dev'
            }
            }
            a = 3
            b = "hi" 
            c = true 
            """

        String c2 = """
            a = 4
            application_environments{ 
            dev {
              long_name = 'Develop'
            }
            }
            """

        String c3 = """
            a = 5
            application_environments{ 
            dev {
              long_name = 'Development'
            }
            }
            """
        when:
        TemplateConfigObject configObject = combine(c1, c2, c3)

        then:// override and merge only apply to the next level
        configObject.config == (basePipelineConfigMap + ([
                application_environments:[
                        dev:[long_name:'Develop']
                ],
                a: 3,
                b: "hi",
                c: true
        ] as LinkedHashMap))
        configObject.merge.isEmpty()
        configObject.override.isEmpty()
    }

    def 'Keys with 2 tier/level override to empty Configuration'(){
        setup:

        String c1 = """
            application_environments{ 
            dev {
              override = true
              names {
                Develop
                Development
                Dev
                dev
                devel
                develop
                development
              }
              long_name = 'Dev'
            }
            }
            a = 3
            b = "hi" 
            c = true 
            """

        String c2 = """
            a = 4
            application_environments{ 
            dev {
              long_name = 'Develop'
              names
            }
            }
            """
        when:
        TemplateConfigObject configObject = combine(c1, c2)

        then:// while the override occurs, the final result has not overrides
        configObject.config == (basePipelineConfigMap + [
                application_environments:[
                        dev:[long_name:'Develop', names:[:]]
                ],
                a: 3,
                b: "hi",
                c: true
        ])
        configObject.merge.isEmpty()
        configObject.override.isEmpty()
    }

    def 'Keys with 2 tier/level override to deeper Configuration'(){
        setup:

        String c1 = """
            application_environments{ 
            dev {
              override = true
              names 
              long_name = 'Dev'
            }
            }
            a = 3
            b = "hi" 
            c = true 
            """

        String c2 = """
            a = 4
            application_environments{ 
            dev {
              long_name = 'Develop'
              names {
                Develop {
                  merge = true
                  name = 'Develop'
                }
                Development
                Dev
                dev
                devel
                develop
                development
              }
            }
            }
            """
        when:
        TemplateConfigObject configObject = combine(c1, c2)

        then:// while the override occurs, the final result has not overrides
        configObject.config == (basePipelineConfigMap + [
                application_environments:[
                        dev:[long_name:'Develop', names:[Develop: [name:'Develop'],Development:[:],
                        Dev:[:],dev:[:],devel:[:],develop:[:],development:[:]]]
                ],
                a: 3,
                b: "hi",
                c: true
        ])
        !configObject.merge.isEmpty()
        (['application_environments.dev.names.Develop'] as Set).equals(configObject.merge)
        configObject.override.isEmpty()
    }

    /*
    helpers
     */

    void templateLoggerSetup(){
        GroovySpy(TemplateLogger, global: true)
        TemplateLogger.print(_, _, _, _) >> { s, h, l, t -> return System.out.print(s) }
    }

    // helper
    TemplateConfigObject combine(String ... configs){

        PipelineConfig p = new PipelineConfig(TemplateConfigDsl.parse(basePipelineConfig))

        println(""); println( "=== combine ===" )
        configs[0..-1].eachWithIndex{ c, i ->
            TemplateConfigObject config = c ? TemplateConfigDsl.parse(c) : null
            if (config){
                println( "config:${i}" )
                print( TemplateConfigDsl.serialize(config) )
                println( "end config:${i}" )

                p.join(config)

                println(""); println( "end p.config:${i}" )
            }
        }

        // p.printChanges(System.out)

        println( "=== end combine ===" ); println("")

        p.config
    }
}
