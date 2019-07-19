package org.boozallen.plugins.jte.utils

import org.jenkinsci.plugins.workflow.cps.*
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import spock.lang.*

import hudson.model.*
import org.junit.*;
import org.jvnet.hudson.test.*;

class CpsSpec extends Specification {
    @Shared
    @ClassRule
    @SuppressWarnings('JUnitPublicField')
    public GroovyJenkinsRule groovyJenkinsRule = new GroovyJenkinsRule()

    @Shared
    private WorkflowJob project

    def "with CPS Thread; using jenkinsRule"(){// testing test harness setup
        project = groovyJenkinsRule.jenkins.createProject(WorkflowJob, "CpsSpec.project");
        project.setDefinition(new CpsFlowDefinition("""
               import org.jenkinsci.plugins.workflow.cps.*
               import org.jenkinsci.plugins.workflow.job.*
               
def currentJob = CpsThread.current()?.execution?.owner?.executable.parent

assert currentJob instanceof WorkflowJob
""", false));// false is needed to access CpsThread

        // Enqueue a build of the Pipeline, wait for it to complete, and assert success

        when:
        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(project);
        WorkflowJob job = build.parent

        // Assert that the console log contains the output we expect
        // groovyJenkinsRule.assertLogContains("hello", build);

        then:
        notThrown(Exception)
        null != job
    }

    def "RunUtils.getJob(), thread.execution"(){
        WorkflowJob job = GroovyMock(WorkflowJob)
            
        GroovyMock(RunUtils.class, global:true)
        1 * RunUtils.getJob() >> job

        when:
        WorkflowJob result = RunUtils.getJob()

        then:
        null != result

    }

}

