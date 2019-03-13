package org.boozallen.plugins.jte

import hudson.plugins.git.BranchSpec
import hudson.plugins.git.GitSCM
import hudson.plugins.git.SubmoduleConfig
import hudson.plugins.git.extensions.GitSCMExtension
import hudson.scm.SCM
import jenkins.plugins.git.GitSampleRepoRule

import jenkins.scm.api.SCMFileSystem
import org.boozallen.plugins.jte.testcategories.InProgress
import org.boozallen.plugins.jte.testcategories.Unstable
import org.jenkinsci.plugins.workflow.cps.*
import org.jenkinsci.plugins.workflow.flow.FlowExecution
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.junit.experimental.categories.Category
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

    @Rule GitSampleRepoRule sampleRepo = new GitSampleRepoRule()
    SCM scm = null
    String cpsScriptPath = "cpsScript.groovy"
    String cpsScript = """
               import org.jenkinsci.plugins.workflow.cps.*
               import org.jenkinsci.plugins.workflow.job.*
              
"""

    def setup(){
        // initialize repository
        sampleRepo.init()


        sampleRepo.write(cpsScriptPath, cpsScript);
        sampleRepo.write("pipeline_config.groovy", """
        libraries{
            openshift{
                url = "whatever" 
            }
        }
        """)
        sampleRepo.git("add", "*")
        sampleRepo.git("commit", "--message=init")
        // create Governance Tier
        scm = new GitSCM(
                GitSCM.createRepoList(sampleRepo.toString(), null),
                Collections.singletonList(new BranchSpec("*/master")),
                false,
                Collections.<SubmoduleConfig>emptyList(),
                null,
                null,
                Collections.<GitSCMExtension>emptyList()
        )
    }

    def "Utils.createSCMFileSystemOrNull(scm,job); using Util.currentJob"(){
        project = groovyJenkinsRule.jenkins.createProject(WorkflowJob, "Utils.createSCMFileSystemOrNull(scm,job); using Util.currentJob");

        def cpsFlowDef = new CpsScmFlowDefinition(scm, cpsScriptPath)// false is needed to access CpsThread
        project.setDefinition(cpsFlowDef);

        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null

        GroovySpy(Utils.class, global:true)
        _ * Utils.getCurrentJob() >> { return job }
        _ * Utils.getListener() >> {return listener}
        _ * Utils.getLogger() >> {return logger}

        when:
        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(project);
        FlowExecution execution = build.execution
        listener = execution.owner.listener
        logger = listener.logger
        job = execution.owner.executable.parent

        SCMFileSystem scmfs = Utils.createSCMFileSystemOrNull(scm, Utils.getCurrentJob(), job.parent)

        then:
        notThrown(Exception)

        null != scmfs
    }

    def "Utils.FileSystemWrapper.fsFrom(job, listener, logger) !WorkflowMultiBranchProject"(){
        project = groovyJenkinsRule.jenkins.createProject(WorkflowJob, "Utils.FileSystemWrapper.fsFrom !WorkflowMultiBranchProject");

        def cpsFlowDef = new CpsScmFlowDefinition(scm, cpsScriptPath)// false is needed to access CpsThread
        project.setDefinition(cpsFlowDef);

        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null

        GroovySpy(Utils.class, global:true)
        _ * Utils.getCurrentJob() >> { return job }
        _ * Utils.getListener() >> {return listener}
        _ * Utils.getLogger() >> {return logger}

        when:
        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(project);
        FlowExecution execution = build.execution
        listener = execution.owner.listener
        logger = listener.logger
        job = execution.owner.executable.parent

        SCMFileSystem scmfs = Utils.FileSystemWrapper.fsFrom(job, listener, logger)

        then:
        notThrown(Exception)

        null != scmfs
    }

    def "Utils.FileSystemWrapper.fsFrom(job, listener, logger) !WorkflowMultiBranchProject,!CpsScmFlowDefinition"(){
        project = groovyJenkinsRule.jenkins.createProject(WorkflowJob, "Utils.FileSystemWrapper.fsFrom !WorkflowMultiBranchProject,!CpsScmFlowDefinition");

        def cpsFlowDef = new CpsFlowDefinition(cpsScript, false)// false is needed to access CpsThread
        project.setDefinition(cpsFlowDef);

        when:
        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(project);
        FlowExecution execution = build.execution
        TaskListener listener = execution.owner.listener
        PrintStream logger = listener.logger
        WorkflowJob job = execution.owner.executable.parent

        SCMFileSystem scmfs = Utils.FileSystemWrapper.fsFrom(job, listener, logger)

        then:
        notThrown(Exception)

        null == scmfs
    }

    @Category([InProgress])
    @Ignore
    def "Utils.FileSystemWrapper.fsFrom(job, listener, logger) WorkflowMultiBranchProject"(){
        WorkflowMultiBranchProject wfmbp = groovyJenkinsRule.jenkins.createProject(WorkflowMultiBranchProject, "Utils.FileSystemWrapper.fsFrom !WorkflowMultiBranchProject");
        project = wfmbp

        //wfmbp.
        def cpsFlowDef = new CpsScmFlowDefinition(scm, cpsScriptPath)// false is needed to access CpsThread
        project.setDefinition(cpsFlowDef);

        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null

        GroovySpy(Utils.class, global:true)
        _ * Utils.getCurrentJob() >> { return job }
        _ * Utils.getListener() >> {return listener}
        _ * Utils.getLogger() >> {return logger}

        when:
        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(project);
        FlowExecution execution = build.execution
        listener = execution.owner.listener
        logger = listener.logger
        job = execution.owner.executable.parent

        SCMFileSystem scmfs = Utils.FileSystemWrapper.fsFrom(job, listener, logger)

        then:
        notThrown(Exception)

        null != scmfs
    }

    @Category([Unstable])
    @Ignore // not getting scm file system
    def "Utils.CpsContext#getFileContents; using jenkinsRule"(){
        project = groovyJenkinsRule.jenkins.createProject(WorkflowJob, "Utils.CpsContext#getFileContents");

        def cpsFlowDef = new CpsFlowDefinition(cpsScript, false)// false is needed to access CpsThread

        project.setDefinition(cpsFlowDef);
        String filePath = "/pipeline_config.groovy"
        String fileContents = """
libraries{
  sdp{
    images{
      registry = "http://0.0.0.0:5000" // registry url
      cred = "sdp-docker-registry"// jenkins cred id to authenticate
      docker_args = "--network=try-it-out_sdp"  // docker runtime args
    }
  }
  github_enterprise
  sonarqube{
    enforce_quality_gate = true
  }
  docker{
    registry = "0.0.0.0:5000"
    cred = "sdp-docker-registry"
  }
}
"""

        SCM scm = new SingleFileSCM(filePath, fileContents) //new ExtractResourceSCM("/sample-app.zip")
        // Enqueue a build of the Pipeline, wait for it to complete, and assert success
        WorkflowRun build = null

        when:
        build = groovyJenkinsRule.buildAndAssertSuccess(project);
        FlowExecution execution = build.execution

        String output = Utils.getFileContents(filePath, scm, "[JTE]")

        then:
        notThrown(Exception)

        output == fileContents
    }

    def "SCMFileSystem.of; using jenkinsRule"(){// testing that setup of scm is correct
        project = groovyJenkinsRule.jenkins.createProject(WorkflowJob, "SCMFileSystem.of; using jenkinsRule");

        def cpsFlowDef = new CpsScmFlowDefinition(scm, cpsScriptPath)

        project.setDefinition(cpsFlowDef);

        when:
        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(project);
        FlowExecution execution = build.execution
        WorkflowJob job = execution.owner.executable.parent

        SCMFileSystem scmfs = SCMFileSystem.of(job, scm);

        then:
        notThrown(Exception)

        SCMFileSystem.supports(scm)
        null != scmfs
    }

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

