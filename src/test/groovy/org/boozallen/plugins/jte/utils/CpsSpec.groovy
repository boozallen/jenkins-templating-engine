package org.boozallen.plugins.jte.utils

import org.boozallen.plugins.jte.Utils
import org.jenkinsci.plugins.workflow.cps.*
import org.jenkinsci.plugins.workflow.flow.FlowExecution
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
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
        FlowExecution execution = build.execution
        FlowExecutionOwner owner = execution.owner

        // Assert that the console log contains the output we expect
        // groovyJenkinsRule.assertLogContains("hello", build);

        then:
        notThrown(Exception)
        null != job
    }

    @Ignore // no longer needed just mock the static utils.get*
    def "with CPS Thread yields, thread.execution"(){
        WorkflowJob job = GroovyMock(WorkflowJob)
        WorkflowRun workflowRun = GroovyMock(WorkflowRun){
            getParent() >> job
        }
        TaskListener listener = GroovyMock(TaskListener){
            getLogger() >> GroovyMock(PrintStream)
        }
        FlowExecutionOwner owner = GroovyMock(FlowExecutionOwner){
            getListener() >> listener
            getExecutable() >> workflowRun
        }
        CpsFlowExecution execution = GroovyMock(CpsFlowExecution){
            getOwner() >> owner
        }
        CpsThread cpsThread = GroovyMock(CpsThread){
            getExecution() >> execution
        }

        GroovyMock(CpsThread.class, global:true)
        1 * CpsThread.current() >> cpsThread

        when:
        CpsThread current = CpsThread.current()

        then:
        execution == current?.execution
        workflowRun == current?.execution?.owner.executable

        // issue mocking workflowRun.getParent()
        // job == current?.execution?.owner.executable.parent
    }

    def "Utils.getCurrentJob(), thread.execution"(){
        WorkflowJob job = GroovyMock(WorkflowJob)
            
        GroovyMock(Utils.class, global:true)
        1 * Utils.getCurrentJob() >> job

        when:
        WorkflowJob result = Utils.getCurrentJob()

        then:
        null != result

    }

}

