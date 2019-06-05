package org.boozallen.plugins.jte.utils

import jenkins.scm.api.SCMFile
import jenkins.scm.api.SCMFileSystem
import org.boozallen.plugins.jte.console.TemplateLogger
import spock.lang.Specification


class FileSpec extends Specification {


    def "getFileContents where SCMFileSystem says f.exists() == false, logMissingFile:true -> log missing file"(){
        given:
        String filePath = "pipeline_config.groovy"
        SCMFile childFile = GroovyMock(SCMFile)
        1 * childFile.exists() >> false

        SCMFileSystem fs = GroovyMock(SCMFileSystem)
        1 * fs.asBoolean() >> true
        1 * fs.child("pipeline_config.groovy") >> childFile

        FileSystemWrapper fsw = new FileSystemWrapper()
        fsw.fs = fs

        GroovyMock(TemplateLogger, global:true)
        1 * TemplateLogger.printWarning(_, _)

        String retVal = ""

        when:
        retVal = fsw.getFileContents(filePath, "file not found", true)

        then:
        null == retVal
        0 * childFile.isFile()
        0 * childFile.contentAsString()
        1 * fs.close()

    }

    def "getFileContents where SCMFileSystem says !f.exists(),  logMissingFile: false -> no log"(){
        given:
        String filePath = "pipeline_config.groovy"
        SCMFile childFile = GroovyMock(SCMFile)
        1 * childFile.exists() >> false

        SCMFileSystem fs = GroovyMock(SCMFileSystem)
        1 * fs.asBoolean() >> true
        1 * fs.child("pipeline_config.groovy") >> childFile

        FileSystemWrapper fsw = new FileSystemWrapper()
        fsw.fs = fs

        GroovyMock(TemplateLogger, global:true)
        0 * TemplateLogger.printWarning(_, _)

        String retVal = ""

        when:
        retVal = fsw.getFileContents(filePath, "file not found", false)

        then:
        null == retVal
        0 * childFile.isFile()
        0 * childFile.contentAsString()
        1 * fs.close()

    }

    def "getFileContents where SCMFileSystem says f.isFile() == false"(){
        given:
        String filePath = "pipeline_config.groovy"
        SCMFile childFile = GroovyMock(SCMFile)
        1 * childFile.exists() >> true
        1 * childFile.isFile() >> false

        SCMFileSystem fs = GroovyMock(SCMFileSystem)
        1 * fs.asBoolean() >> true
        1 * fs.child("pipeline_config.groovy") >> childFile

        FileSystemWrapper fsw = new FileSystemWrapper()
        fsw.fs = fs

        GroovyMock(TemplateLogger, global:true)
        1 * TemplateLogger.printWarning(_, _)

        String retVal = ""

        when:
        retVal = fsw.getFileContents(filePath, "not a file", true)

        then:
        null == retVal
        0 * childFile.contentAsString()
        1 * fs.close()


    }

    def "getFileContents with no logging description"(){
        given:
        String fileContents = "woot, a file"
        String filePath = "pipeline_config.groovy"
        SCMFile childFile = GroovyMock(SCMFile)
        1 * childFile.exists() >> true
        1 * childFile.isFile() >> true
        1 * childFile.contentAsString() >> fileContents

        SCMFileSystem fs = GroovyMock(SCMFileSystem)
        1 * fs.asBoolean() >> true
        1 * fs.child("pipeline_config.groovy") >> childFile

        FileSystemWrapper fsw = new FileSystemWrapper()
        fsw.fs = fs

        GroovyMock(TemplateLogger, global:true)
        0 * TemplateLogger.printWarning(_, _)

        String retVal = ""

        when:
        retVal = fsw.getFileContents(filePath, null,true)

        then:
        fileContents == retVal
        1 * fs.close()

    }

    def "getFileContents where SCMFileSystem has valid file with loggingdescription"(){
        given:
        String fileContents = "woot, a file"
        String filePath = "pipeline_config.groovy"
        SCMFile childFile = GroovyMock(SCMFile)
        1 * childFile.exists() >> true
        1 * childFile.isFile() >> true
        1 * childFile.contentAsString() >> fileContents

        SCMFileSystem fs = GroovyMock(SCMFileSystem)
        1 * fs.asBoolean() >> true
        1 * fs.child("pipeline_config.groovy") >> childFile

        FileSystemWrapper fsw = new FileSystemWrapper()
        fsw.fs = fs

        GroovyMock(TemplateLogger, global:true)
        1 * TemplateLogger.print("""Obtained valid file
                                        -- scm: null
                                        -- file path: pipeline_config.groovy""", [initiallyHidden:true])

        String retVal = ""

        when:
        retVal = fsw.getFileContents(filePath, "valid file", true)

        then:
        fileContents == retVal
        1 * fs.close()

    }

    def "getFileContents where SCMFileSystem has valid file and default values"(){
        given:
        String fileContents = "woot, a file"
        String filePath = "pipeline_config.groovy"
        SCMFile childFile = GroovyMock(SCMFile)
        1 * childFile.exists() >> true
        1 * childFile.isFile() >> true
        1 * childFile.contentAsString() >> fileContents

        SCMFileSystem fs = GroovyMock(SCMFileSystem)
        1 * fs.asBoolean() >> true
        1 * fs.child("pipeline_config.groovy") >> childFile

        FileSystemWrapper fsw = new FileSystemWrapper()
        fsw.fs = fs

        GroovyMock(TemplateLogger, global:true)
        0 * TemplateLogger.printWarning(_, _)

        String retVal = ""

        when:
        retVal = fsw.getFileContents(filePath)

        then:
        fileContents == retVal
        1 * fs.close()

    }

    def "getFileContents where !f.exists -> log missing file by default"(){
        given:
        String filePath = "pipeline_config.groovy"
        SCMFile childFile = GroovyMock(SCMFile)
        1 * childFile.exists() >> false

        SCMFileSystem fs = GroovyMock(SCMFileSystem)
        1 * fs.asBoolean() >> true
        1 * fs.child("pipeline_config.groovy") >> childFile

        FileSystemWrapper fsw = new FileSystemWrapper()
        fsw.fs = fs

        GroovyMock(TemplateLogger, global:true)
        1 * TemplateLogger.printWarning(_, _)


        String retVal = ""

        when:
        retVal = fsw.getFileContents(filePath)

        then:
        null == retVal
        0 * childFile.isFile()
        0 * childFile.contentAsString()
        1 * fs.close()

    }

    def "'as SCMFileSystem' returns valid fs property"() {
        given:
        SCMFileSystem fs = GroovyMock(SCMFileSystem)

        FileSystemWrapper fsw = new FileSystemWrapper()
        fsw.fs = fs

        when:
        def res = fsw as SCMFileSystem

        then:
        res != null
        res == fs
    }

    def "'as SCMFileSystem' returns null fs property"() {
        given:

        FileSystemWrapper fsw = new FileSystemWrapper()

        when:
        def res = fsw as SCMFileSystem

        then:
        res == null
    }

    def "'as' !(SCMFileSystem|Object) returns null"() {
        given:

        SCMFileSystem fs = GroovyMock(SCMFileSystem)

        FileSystemWrapper fsw = new FileSystemWrapper()
        fsw.fs = fs

        when:
        def res = fsw as Map

        then:
        res == null
        null != fsw.fs
    }

    def "'as' Object returns this"() {
        given:

        SCMFileSystem fs = GroovyMock(SCMFileSystem)

        FileSystemWrapper fsw = new FileSystemWrapper()
        fsw.fs = fs

        when:
        def res = fsw as Object

        then:
        res == fsw
    }
}

