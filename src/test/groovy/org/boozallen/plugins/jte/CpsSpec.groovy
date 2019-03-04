package org.boozallen.plugins.jte

import hudson.model.TaskListener
import org.jenkinsci.plugins.workflow.cps.*
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import spock.lang.*


class CpsSpec extends Specification {

    def setup(){

    }

    @Ignore
    def "with CPS Thread yields, job"(){
        WorkflowJob job = GroovyMock(WorkflowJob)
        WorkflowRun workflowRun = GroovyMock(WorkflowRun)

        TaskListener listener = GroovyMock(TaskListener){
            1 * getLogger() >> GroovyMock(PrintStream)
        }

        FlowExecutionOwner owner = GroovyMock(FlowExecutionOwner){
            1 * getListener() >> listener
        }

        CpsFlowExecution execution = GroovyMock(CpsFlowExecution)

        CpsThread cpsThread = GroovyMock(CpsThread){
            asBoolean() >> true
        }

        GroovyMock(CpsThread.class, global:true)


        WorkflowJob currentJob = null

        when:
        currentJob = Utils.getCurrentJob()

        then:
        1 * CpsThread.current() >> cpsThread
        1 * cpsThread.getExecution() >> execution
        1 * execution.getOwner() >> owner
        1 * owner.getExecutable() >> workflowRun
        1 * workflowRun.getParent() >> job
        verifyAll {

            null != currentJob
            currentJob == job
        }
    }

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

        // org.jenkinsci.plugins.workflow.job.WorkflowJob job = Mock(org.jenkinsci.plugins.workflow.job.WorkflowJob)

        CpsThread current = null


        when:
        current = CpsThread.current()

        then:
        execution == current?.execution
        workflowRun == current?.execution?.owner.executable

        // job == current?.execution?.owner.executable.parent
    }

    def "with CPS Thread yields, thread"(){
        CpsThread cpsThread = GroovyMock(CpsThread)
        GroovyMock(CpsThread.class, global:true)

        1 * CpsThread.current() >> cpsThread

        CpsThread current = null

        when:
        current = CpsThread.current()

        then:
        current == cpsThread

    }

}

