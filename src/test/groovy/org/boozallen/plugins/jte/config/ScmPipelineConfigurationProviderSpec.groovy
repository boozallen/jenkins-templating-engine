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

import spock.lang.*
import hudson.scm.SCM
import hudson.scm.NullSCM
import org.boozallen.plugins.jte.util.FileSystemWrapper
import org.boozallen.plugins.jte.util.TemplateLogger

class ScmPipelineConfigurationProviderSpec extends Specification{

    def "getConfig returns null if scm not defined"(){
        setup:
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()

        expect:
        p.getConfig() == null
    }

    def "getConfig returns null if scm is NullSCM"(){
        setup:
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()
        NullSCM scm = Mock()
        p.setScm(scm)

        expect:
        p.getConfig() == null
    }

    def "getConfig properly sets filePath if baseDir is null"(){
        setup:
            ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()
            SCM scm = Mock()
            p.setScm(scm)

            FileSystemWrapper fsw = Mock()
            GroovySpy(FileSystemWrapper, global: true)
            FileSystemWrapper.createFromSCM(_) >> fsw

            String configFile = "config"
            fsw.getFileContents(_, _) >> configFile

            GroovySpy(TemplateConfigDsl, global: true)
            TemplateConfigDsl.parse(_) >> Mock(PipelineConfigurationObject)
        when:
            p.getConfig()

        then:
            1 * fsw.getFileContents(ScmPipelineConfigurationProvider.CONFIG_FILE, _)
    }

    def "getConfig properly sets filePath if baseDir is not null"(){
        setup:
            ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()
            SCM scm = Mock()
            p.setScm(scm)

            String baseDir = "pipeline-configuration"
            p.setBaseDir(baseDir)

            FileSystemWrapper fsw = Mock()
            GroovySpy(FileSystemWrapper, global: true)
            FileSystemWrapper.createFromSCM(_) >> fsw

            String configFile = "config"
            fsw.getFileContents(_, _) >> configFile

            GroovySpy(TemplateConfigDsl, global: true)
            TemplateConfigDsl.parse(_) >> Mock(PipelineConfigurationObject)
        when:
            p.getConfig()

        then:
            1 * fsw.getFileContents("${baseDir}/${ScmPipelineConfigurationProvider.CONFIG_FILE}", _)
    }

    def "getConfig returns null if config file not present in scm"(){
        setup:
            ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()
            SCM scm = Mock()
            p.setScm(scm)

            String baseDir = "pipeline-configuration"
            p.setBaseDir(baseDir)

            FileSystemWrapper fsw = Mock()
            GroovySpy(FileSystemWrapper, global: true)
            FileSystemWrapper.createFromSCM(_) >> fsw

            fsw.getFileContents(_, _) >> null

            GroovySpy(TemplateConfigDsl, global: true)
            TemplateConfigDsl.parse(_) >> Mock(PipelineConfigurationObject)

        expect:
            p.getConfig() == null
    }

    def "getConfig returns PipelineConfigurationObject if config file present and compilable"(){
        setup:
            ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()
            SCM scm = Mock()
            p.setScm(scm)

            String baseDir = "pipeline-configuration"
            p.setBaseDir(baseDir)

            FileSystemWrapper fsw = Mock()
            GroovySpy(FileSystemWrapper, global: true)
            FileSystemWrapper.createFromSCM(_) >> fsw

            String configFile = "config"
            fsw.getFileContents(_, _) >> configFile

            PipelineConfigurationObject confObj = Mock()
            GroovySpy(TemplateConfigDsl, global: true)
            TemplateConfigDsl.parse(_) >> confObj

        expect:
            p.getConfig() == confObj
    }

    def "getConfig prints error and throws exception if config file not compilable"(){
        setup:
            ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()
            SCM scm = Mock()
            p.setScm(scm)

            String baseDir = "pipeline-configuration"
            p.setBaseDir(baseDir)

            FileSystemWrapper fsw = Mock()
            GroovySpy(FileSystemWrapper, global: true)
            FileSystemWrapper.createFromSCM(_) >> fsw

            String configFile = "config"
            fsw.getFileContents(_, _) >> configFile

            PipelineConfigurationObject confObj = Mock()
            GroovySpy(TemplateConfigDsl, global: true)
            TemplateConfigDsl.parse(_) >> {
                throw new Exception()
            }

            PrintStream logger = Mock()
            GroovySpy(TemplateLogger, global:true)
            TemplateLogger.printError(*_) >> { args ->
                logger.println(args[0])
            }

        when:
            p.getConfig()

        then:
            1 * logger.println("Error parsing scm provided pipeline configuration")
            thrown(Exception)
    }

    def "getJenkinsfile returns null if scm not defined"(){
        setup:
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()

        expect:
        p.getJenkinsfile() == null
    }

    def "getJenkinsfile returns null if scm is NullSCM"(){
        setup:
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()
        NullSCM scm = Mock()
        p.setScm(scm)

        expect:
        p.getJenkinsfile() == null
    }

    def "getJenkinsfile properly sets filePath if baseDir is null"(){
        setup:
            ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()
            SCM scm = Mock()
            p.setScm(scm)

            FileSystemWrapper fsw = Mock()
            GroovySpy(FileSystemWrapper, global: true)
            FileSystemWrapper.createFromSCM(_) >> fsw

            fsw.getFileContents(*_) >> null

            GroovySpy(TemplateConfigDsl, global: true)
            TemplateConfigDsl.parse(_) >> Mock(PipelineConfigurationObject)
        when:
            p.getJenkinsfile()

        then:
            1 * fsw.getFileContents("Jenkinsfile", _)
    }

    def "getJenkinsfile properly sets filePath if baseDir is not null"(){
        setup:
            ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()
            SCM scm = Mock()
            p.setScm(scm)
            String baseDir = "pipeline-configuration"
            p.setBaseDir(baseDir)

            FileSystemWrapper fsw = Mock()
            GroovySpy(FileSystemWrapper, global: true)
            FileSystemWrapper.createFromSCM(_) >> fsw

            fsw.getFileContents(*_) >> null

            GroovySpy(TemplateConfigDsl, global: true)
            TemplateConfigDsl.parse(_) >> Mock(PipelineConfigurationObject)
        when:
            p.getJenkinsfile()

        then:
            1 * fsw.getFileContents("${baseDir}/Jenkinsfile", _)
    }

    def "getJenkinsfile returns null if Jenkinsfile not present in scm"(){
        setup:
            ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()
            SCM scm = Mock()
            p.setScm(scm)
            String baseDir = "pipeline-configuration"
            p.setBaseDir(baseDir)

            FileSystemWrapper fsw = Mock()
            GroovySpy(FileSystemWrapper, global: true)
            FileSystemWrapper.createFromSCM(_) >> fsw

            fsw.getFileContents("Jenkinsfile", _) >> null

            GroovySpy(TemplateConfigDsl, global: true)
            TemplateConfigDsl.parse(_) >> Mock(PipelineConfigurationObject)
        expect:
            p.getJenkinsfile() == null
    }

    def "getJenkinsfile returns Jenkinsfile if present in scm"(){
        setup:
            ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()
            SCM scm = Mock()
            p.setScm(scm)

            FileSystemWrapper fsw = Mock()
            GroovySpy(FileSystemWrapper, global: true)
            FileSystemWrapper.createFromSCM(_) >> fsw

            String jenkinsfile = "Jenkinsfile"
            fsw.getFileContents("Jenkinsfile", _) >> jenkinsfile

            GroovySpy(TemplateConfigDsl, global: true)
            TemplateConfigDsl.parse(_) >> Mock(PipelineConfigurationObject)
        expect:
            p.getJenkinsfile() == jenkinsfile
    }

    def "getTemplate returns null if scm not defined"(){
        setup:
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()

        expect:
        p.getTemplate("template") == null
    }

    def "getTemplate returns null if scm is NullSCM"(){
        setup:
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()
        NullSCM scm = Mock()
        p.setScm(scm)

        expect:
        p.getTemplate("template") == null
    }

    def "getTemplate properly sets filePath if baseDir is null"(){
        setup:
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()
        SCM scm = Mock()
        p.setScm(scm)

        FileSystemWrapper fsw = Mock()
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(_) >> fsw

        fsw.getFileContents(*_) >> null

        String templateName = "template"

        when:
        p.getTemplate(templateName)

        then:
        1 * fsw.getFileContents("${ScmPipelineConfigurationProvider.PIPELINE_TEMPLATE_DIRECTORY}/${templateName}", _)
    }

    def "getTemplate properly sets filePath if baseDir is not null"(){
        setup:
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()
        SCM scm = Mock()
        p.setScm(scm)
        String baseDir = "pipeline-configuration"
        p.setBaseDir(baseDir)

        FileSystemWrapper fsw = Mock()
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(_) >> fsw

        fsw.getFileContents(*_) >> null

        String templateName = "template"
        when:
        p.getTemplate(templateName)

        then:
        1 * fsw.getFileContents("${baseDir}/${ScmPipelineConfigurationProvider.PIPELINE_TEMPLATE_DIRECTORY}/${templateName}", _)
    }

    def "getTemplate returns null if template not present in scm"(){
        setup:
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()
        SCM scm = Mock()
        p.setScm(scm)

        FileSystemWrapper fsw = Mock()
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(_) >> fsw

        fsw.getFileContents(*_) >> null

        String templateName = "template"

        expect:
        p.getTemplate(templateName) == null
    }

    def "getTemplate returns template if present in scm"(){
        setup:
        ScmPipelineConfigurationProvider p = new ScmPipelineConfigurationProvider()
        SCM scm = Mock()
        p.setScm(scm)

        FileSystemWrapper fsw = Mock()
        GroovySpy(FileSystemWrapper, global: true)
        FileSystemWrapper.createFromSCM(_) >> fsw

        String templateName = "template"
        String template = "the template"
        fsw.getFileContents(*_) >> template

        expect:
        p.getTemplate(templateName) == template
    }

}
