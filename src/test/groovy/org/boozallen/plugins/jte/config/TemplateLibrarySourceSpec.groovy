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


import org.boozallen.plugins.jte.Utils
import org.boozallen.plugins.jte.binding.StepWrapper
import org.boozallen.plugins.jte.binding.TemplateBinding
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

    def setup(){

        WorkflowJob job = jenkins.createProject(WorkflowJob)
        PrintStream logger = new PrintStream(System.out)
        GroovySpy(Utils, global:true)
        _ * Utils.getCurrentJob() >> job 
        _ * Utils.getLogger() >> logger

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
}