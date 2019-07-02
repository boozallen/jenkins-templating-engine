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

import org.boozallen.plugins.jte.binding.StepWrapper
import org.boozallen.plugins.jte.binding.TemplateBinding
import org.boozallen.plugins.jte.console.TemplateLogger
import org.boozallen.plugins.jte.utils.RunUtils
import spock.lang.*
import org.junit.Rule
import org.junit.ClassRule
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import jenkins.plugins.git.GitSampleRepoRule
import hudson.plugins.git.GitSCM
import hudson.plugins.git.BranchSpec
import hudson.plugins.git.extensions.GitSCMExtension
import hudson.plugins.git.SubmoduleConfig
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.WithoutJenkins
import org.jvnet.hudson.test.BuildWatcher
import org.jenkinsci.plugins.workflow.cps.CpsScript

class TemplateLibrarySourceSpec extends Specification{

    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()
    @Shared @ClassRule BuildWatcher bw = new BuildWatcher()
    @Rule GitSampleRepoRule repo = new GitSampleRepoRule()
    TemplateLibrarySource librarySource = new TemplateLibrarySource()
    WorkflowJob job = GroovyMock()
    PrintStream logger = Mock()
    String scmKey = null

    def setup(){

        WorkflowJob job = jenkins.createProject(WorkflowJob)


        GroovySpy(RunUtils, global:true)
        _ * RunUtils.getJob() >> job
        GroovyMock(TemplateLogger, global:true)
        _ * TemplateLogger.print(_,_)


        repo.init()

        GitSCM scm = new GitSCM(
            GitSCM.createRepoList(repo.toString(), null), 
            Collections.singletonList(new BranchSpec("*/master")), 
            false, 
            Collections.<SubmoduleConfig>emptyList(), 
            null, 
            null, 
            Collections.<GitSCMExtension>emptyList()
        )

        scmKey = scm.key

        librarySource.setScm(scm)
    }

    @WithoutJenkins
    def "hasLibrary returns true when library exists"(){
        setup:
            repo.write("test_library/a.groovy", "doesnt matter")
            repo.git("add", "*")
            repo.git("commit", "--message=init")
        when: 
            Boolean libExists = librarySource.hasLibrary("test_library")
        then: 
            libExists
    }

    @WithoutJenkins
    def "hasLibrary returns false when library doesn't exist"(){
        when: 
            Boolean libExists = librarySource.hasLibrary("test_library")
        then: 
            !libExists
    }

    @WithoutJenkins
    def "loadLibrary loads each groovy file in library as a step"(){
        setup: 
            repo.write("test_library/a.groovy", "doesnt matter")
            repo.write("test_library/b.groovy", "doesnt matter")
            repo.write("test_library/c.txt", "doesnt matter")
            repo.git("add", "*")
            repo.git("commit", "--message=init")

            TemplateBinding binding = Mock()
            CpsScript script = Mock{
                getBinding() >> binding 
            }

            GroovySpy(StepWrapper, global:true)
            StepWrapper.createFromFile(*_) >>> [ 
                new StepWrapper(name: "a"),
                new StepWrapper(name: "b"),
                new StepWrapper(name: "c")
            ]

        when: 
            librarySource.loadLibrary(script, "test_library", [:])
        then: 
            1 * binding.setVariable("a", _)
            1 * binding.setVariable("b", _)            
    }

    @WithoutJenkins
    def "loadLibrary ignores non-groovy files in library"(){
        setup: 
            repo.write("test_library/a.txt", "doesnt matter")
            repo.git("add", "*")
            repo.git("commit", "--message=init")

            TemplateBinding binding = Mock()
            CpsScript script = GroovyMock(){
                getBinding() >> binding 
            }
        when:
            librarySource.loadLibrary(script, "test_library", [:])
        then: 
            0 * binding.setVariable(_, _)
    }

    @WithoutJenkins
    def "loadLibrary only loads groovy files from 1 library"(){
        setup: 
            repo.write("test_library/a.groovy", "doesnt matter")
            repo.write("test_library/b.groovy", "doesnt matter")
            repo.write("other_library/c.groovy", "doesnt matter")
            repo.git("add", "*")
            repo.git("commit", "--message=init")

            TemplateBinding binding = Mock()
            CpsScript script = GroovyMock{
                getBinding() >> binding 
            }
            GroovySpy(StepWrapper, global:true)
            2 * StepWrapper.createFromFile(*_) >>> [
                new StepWrapper(name: "a"),
                new StepWrapper(name: "b")
            ]
        when: 
            librarySource.loadLibrary(script, "test_library", [:])
        then: 
            1 * binding.setVariable("a", _)
            1 * binding.setVariable("b", _)
            0 * binding.setVariable("c", _)
    }

    @Unroll
    @WithoutJenkins
    def "when baseDir='#baseDir' then prefixBaseDir('#arg') is #expected "(){
        setup: 
            TemplateLibrarySource libSource = new TemplateLibrarySource()
            libSource.setBaseDir(baseDir)
        expect: 
            libSource.prefixBaseDir(arg) == expected 

        where: 
        baseDir    | arg    || expected 
        " someDir" | "lib"  || "someDir/lib"
        "someDir " | "lib"  || "someDir/lib" 
        "someDir"  | " lib" || "someDir/lib"
        "someDir"  | "lib " || "someDir/lib" 
        "someDir"  | null   || "someDir"
        "someDir"  | ""     || "someDir"
        null       | "lib"  || "lib" 
        ""         | "lib"  || "lib" 
        null       | null   || ""
    }

    /*
    begin testing library config validation 
    */
    @Unroll
    @WithoutJenkins 
    def "when config value is '#actual' and expected type/value is #expected then result is #result"(){
        setup: 
            TemplateLibrarySource libSource = new TemplateLibrarySource() 
        expect: 
            libSource.validateType(actual, expected) == result 
        where: 
        actual      |     expected      | result 
        true        |      boolean      | true 
        false       |      boolean      | true 
        true        |      Boolean      | true 
        false       |      Boolean      | true 
        "nope"      |      boolean      | false 
        "hey"       |      String       | true 
        "${4}"      |      String       | true 
        4           |      String       | false 
        4           |      Integer      | true 
        4           |      int          | true 
        4.2         |      Integer      | false  
        4.2         |      int          | false 
        1           |      Double       | false 
        1.0         |      Double       | true
        1           |      Number       | true 
        1.0         |      Number       | true 
        "hey"       |     ~/.*he.*/     | true 
        "heyyy"     |     ~/^hey.*/     | true 
        "hi"        |     ~/^hey.*/     | false 
        "hi"        |    ["hi","hey"]   | true 
        "opt3"      |  ["opt1", "opt2"] | false 
        
    }


    @WithoutJenkins 
    def "Missing library config file prints warning"(){
        setup: 
            repo.write("test/a.txt", "_")
            repo.git("add", "*")
            repo.git("commit", "--message=init")

            TemplateBinding binding = Mock()
            CpsScript script = Mock{
                getBinding() >> binding 
            }

            PrintStream logger = Mock() 

            GroovySpy(TemplateLogger, global:true) 
            TemplateLogger.printWarning(*_) >> { args -> 
                logger.println(args[0])
            }
        when: 
            librarySource.loadLibrary(script, "test", [:])

        then: 
            1 * logger.println("Library test does not have a configuration file.")

    }

    @WithoutJenkins
    def "Library config file is not loaded as a step"(){
        setup: 
            repo.write("test/a.groovy", "_")
            repo.write("test/library_config.groovy", """
            fields{
                required{}
                optional{}
            }
            """)
            repo.git("add", "*")
            repo.git("commit", "--message=init")

            TemplateBinding binding = Mock()
            CpsScript script = Mock{
                getBinding() >> binding 
            }

            GroovySpy(StepWrapper, global:true) 
            StepWrapper.createFromFile(*_) >> { args -> 
                String name = args[0].getName() - ".groovy"
                return new StepWrapper(name: name)
            }

        when: 
            librarySource.loadLibrary(script, "test", [:])
            
        then: 
            1 * binding.setVariable("a", _)
            0 * binding.getVariable("library_config", _)
    }

    @WithoutJenkins
    def "Empty array returned when no errors present"(){
        setup: 
            repo.write("test/a.groovy", "_")
            repo.write("test/library_config.groovy", """
            fields{
                required{
                    field1 = Boolean
                }
                optional{}
            }
            """)
            repo.git("add", "*")
            repo.git("commit", "--message=init")

            TemplateBinding binding = Mock()
            CpsScript script = Mock{
                getBinding() >> binding 
            }

            GroovySpy(StepWrapper, global:true) 
            StepWrapper.createFromFile(*_) >> { args -> 
                String name = args[0].getName() - ".groovy"
                return new StepWrapper(name: name)
            }

            ArrayList libConfigErrors = [] 
        when: 
            libConfigErrors = librarySource.loadLibrary(script, "test", [field1: true])
            
        then: 
            libConfigErrors.isEmpty() 
    }

    @WithoutJenkins
    def "Library config error array contains library name as first element"(){
        setup: 
            repo.write("test/a.groovy", "_")
            repo.write("test/library_config.groovy", """
            fields{
                required{
                    field1 = Boolean
                }
                optional{}
            }
            """)
            repo.git("add", "*")
            repo.git("commit", "--message=init")

            TemplateBinding binding = Mock()
            CpsScript script = Mock{
                getBinding() >> binding 
            }

            GroovySpy(StepWrapper, global:true) 
            StepWrapper.createFromFile(*_) >> { args -> 
                String name = args[0].getName() - ".groovy"
                return new StepWrapper(name: name)
            }

            ArrayList libConfigErrors = [] 
        when: 
            libConfigErrors = librarySource.loadLibrary(script, "test", [field2: true])
            
        then: 
            libConfigErrors[0].trim().equals("test:")
    }

    @WithoutJenkins
    def "Missing required library config key throws error"(){
        setup:
        String requiredKey = "field1"
        repo.write("test/a.groovy", "_")
        repo.write("test/library_config.groovy", """
            fields{
                required{
                    ${requiredKey} = Boolean
                }
                optional{}
            }
            """)
        repo.git("add", "*")
        repo.git("commit", "--message=init")
        TemplateBinding binding = Mock()
        CpsScript script = Mock{
            getBinding() >> binding
        }

        GroovySpy(StepWrapper, global:true)
        StepWrapper.createFromFile(*_) >> { args ->
            String name = args[0].getName() - ".groovy"
            return new StepWrapper(name: name)
        }

        ArrayList libConfigErrors = []

        when:
        libConfigErrors = librarySource.loadLibrary(script, "test", [field2: true])

        then:

        libConfigErrors[1].trim().contains("Missing required field '${requiredKey}'")
    }

    @WithoutJenkins
    def "Missing optional library config key throws no error"(){}

    @WithoutJenkins
    def "Missing required block in library config file throws no error"(){}

    @WithoutJenkins
    def "Missing optional block in library config file throws no error"(){}

    @WithoutJenkins
    def "Extraneous library config key throws error"(){}

    @WithoutJenkins
    def "Library config key mismatch throws error"(){}

    @WithoutJenkins
    def "Library config enum returns appropriate error if not one of the options"(){}

    @WithoutJenkins
    def "Library config string pattern returns appropriate error if not matching"(){}

}