package org.boozallen.plugins.jte.utils

import hudson.model.Action
import hudson.model.TaskListener
import hudson.plugins.git.BranchSpec
import hudson.plugins.git.GitSCM
import hudson.plugins.git.SubmoduleConfig
import hudson.plugins.git.extensions.GitSCMExtension
import hudson.scm.SCM
import jenkins.branch.BranchProperty
import jenkins.branch.BranchSource
import jenkins.branch.DefaultBranchPropertyStrategy
import jenkins.plugins.git.GitSCMSource
import jenkins.plugins.git.GitSampleRepoRule
import jenkins.plugins.git.traits.BranchDiscoveryTrait
import jenkins.scm.api.SCMFileSystem
import org.boozallen.plugins.jte.Utils
import org.boozallen.plugins.jte.testcategories.*
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import org.jenkinsci.plugins.workflow.flow.FlowExecution
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.junit.ClassRule
import org.junit.Rule
import org.jvnet.hudson.test.BuildWatcher
import org.jvnet.hudson.test.GroovyJenkinsRule
import org.jvnet.hudson.test.WithoutJenkins
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

class ScmSpec extends Specification {
    @Shared
    @ClassRule
    @SuppressWarnings('JUnitPublicField')
    public GroovyJenkinsRule groovyJenkinsRule = new GroovyJenkinsRule()

    @Shared
    public BuildWatcher bw = new BuildWatcher()

    @Shared
    private WorkflowJob project

    @Shared
    @ClassRule GitSampleRepoRule sampleRepo = new GitSampleRepoRule()

    @Shared
    SCM scm = null

    @Shared
    String cpsScriptPath = "cpsScript.groovy"

    @Shared
    String cpsScript = """
               import org.jenkinsci.plugins.workflow.cps.*
               import org.jenkinsci.plugins.workflow.job.*
              
"""
    @Shared
    String pipelineConfigPath = "pipeline_config.groovy"


    @Shared
    String pipelineConfigScript = """
        libraries{
            openshift{
                url = "whatever" 
            }
        }
    """

    @Shared
    String cpsScriptPath2 = "cpsScript2.groovy"

    @Shared
    String cpsScript2 = """
               import org.jenkinsci.plugins.workflow.cps.*
               import org.jenkinsci.plugins.workflow.job.*
               import static  org.boozallen.plugins.jte.Utils.*

            println getFileContents('pipeline_config.groovy', null, "template configuration file")               

"""

    @Shared
    private WorkflowJob scmWorkflowJob = null

    @Shared WorkflowJob stdWorkflowJob = null

    @Shared
    CpsScmFlowDefinition cpsScmFlowDefinition = null

    @Rule public GitSampleRepoRule localRepo = new GitSampleRepoRule();

    def setupSpec(){
        // initialize repository
        sampleRepo.init()


        sampleRepo.write(cpsScriptPath, cpsScript);
        sampleRepo.write(pipelineConfigPath, pipelineConfigScript)
        sampleRepo.write(cpsScriptPath2, cpsScript2)

        // added for WorkflowMultibranchProject
        sampleRepo.write("Jenkinsfile", "echo \"branch=master\"; node {checkout scm; echo readFile('file')}");
        sampleRepo.write("file", "initial content"); ;
        sampleRepo.git("add", "*")
        sampleRepo.git("commit", "--all", "--message=master");


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

        scmWorkflowJob = groovyJenkinsRule.jenkins.createProject(WorkflowJob, "scmWorkflowJob");
        cpsScmFlowDefinition = new CpsScmFlowDefinition(scm, cpsScriptPath)
        
        scmWorkflowJob.setDefinition(cpsScmFlowDefinition);

        stdWorkflowJob = groovyJenkinsRule.jenkins.createProject(WorkflowJob, "stdWorkflowJob");

        def cpsFlowDef = new CpsFlowDefinition(cpsScript, false)// false is needed to access CpsThread
        stdWorkflowJob.setDefinition(cpsFlowDef);

    }



    @Ignore //can't mock SCMFileSystem.of probably because it is Abstract
    @WithoutJenkins
    def "Utils.scmFileSystemOrNull(scm,job); SCMFileSystem.of Exception"(){
        given: "mocked job, listener, a valid printstream for 'logger'"

        WorkflowJob job = GroovyMock(WorkflowJob)
        TaskListener listener = GroovyMock(TaskListener)
        ByteArrayOutputStream bs = new ByteArrayOutputStream()
        PrintStream logger = new PrintStream(bs)
        String message = "SCMFileSystem.of Exception"

        GroovyMock(global: true, SCMFileSystem)
        SCMFileSystem.of(job, scm) >> { throw new Exception("message") }

        GroovySpy(Utils.class, global:true)
        _ * Utils.getCurrentJob() >> { return job }
        _ * Utils.getListener() >> {return listener}
        _ * Utils.getLogger() >> {return logger}

        when:"Utils.scmFileSystemOrNull is called with the project's job and scm"

        def(SCMFileSystem scmfs, String scmKey) = Utils.scmFileSystemOrNull(scm, job)

        then:"it should throw internal exception and be logged"
        notThrown(Exception)
        null == scmfs
        bs.toString("UTF-8") == message
        null == scmKey
    }


    @WithoutJenkins
    def "Utils.scmFileSystemOrNull(scm,job); using Util.currentJob"(){
        given: "a workflowjob project with a valid scm"

        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null

        GroovySpy(Utils.class, global:true)
        _ * Utils.getCurrentJob() >> { return job }
        _ * Utils.getListener() >> {return listener}
        _ * Utils.getLogger() >> {return logger}

        when:"Utils.scmFileSystemOrNull is called with the project's job and scm"
        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(scmWorkflowJob);
        FlowExecution execution = build.execution
        listener = execution.owner.listener
        logger = listener.logger
        job = execution.owner.executable.parent

        def(SCMFileSystem scmfs, String scmKey) = Utils.scmFileSystemOrNull(scm, Utils.getCurrentJob())

        then:"it should return a valid SCM filesystem"
        notThrown(Exception)
        null != scmfs
        null != scmKey
    }

    @WithoutJenkins
    def "Utils.scmFileSystemOrNull(null,job); using Util.currentJob"(){
        given: "a workflowjob project with a valid scm"

        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null

        GroovySpy(Utils.class, global:true)
        _ * Utils.getCurrentJob() >> { return job }
        _ * Utils.getListener() >> {return listener}
        _ * Utils.getLogger() >> {return logger}

        when:"Utils.scmFileSystemOrNull is called with the project's job and *no* scm"
        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(scmWorkflowJob);
        FlowExecution execution = build.execution
        listener = execution.owner.listener
        logger = listener.logger
        job = execution.owner.executable.parent

        def(SCMFileSystem scmfs, String scmKey) = Utils.scmFileSystemOrNull(null, Utils.getCurrentJob())


        then:"it should return a valid SCM filesystem"
        notThrown(Exception)
        null != scmfs
        null != scmKey
        groovyJenkinsRule.assertLogNotContains("[inferred]", build);
        groovyJenkinsRule.assertLogContains(scmKey, build);

    }

    @WithoutJenkins
    def "Utils.createSCMFileSystemOrNull(scm,job); using Util.currentJob"(){
        given: "a workflowjob project with a valid scm; valid Utils CPS properties"

        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null

        GroovySpy(Utils.class, global:true)
        _ * Utils.getCurrentJob() >> { return job }
        _ * Utils.getListener() >> {return listener}
        _ * Utils.getLogger() >> {return logger}

        when:"Utils.createSCMFileSystemOrNull is called with the project's job and scm"
        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(scmWorkflowJob);
        FlowExecution execution = build.execution
        listener = execution.owner.listener
        logger = listener.logger
        job = execution.owner.executable.parent

        SCMFileSystem scmfs = Utils.createSCMFileSystemOrNull(scm, Utils.getCurrentJob(), job.parent)

        then:"it should return a valid SCM filesystem"
        notThrown(Exception)

        null != scmfs
    }

    @WithoutJenkins
    def "Utils.FileSystemWrapper.fsFrom(job, listener, logger) !WorkflowMultiBranchProject"(){

        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null

        GroovySpy(Utils.class, global:true)
        _ * Utils.getCurrentJob() >> { return job }
        _ * Utils.getListener() >> {return listener}
        _ * Utils.getLogger() >> {return logger}

        when:
        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(scmWorkflowJob);
        FlowExecution execution = build.execution
        listener = execution.owner.listener
        logger = listener.logger
        job = execution.owner.executable.parent

        def(SCMFileSystem scmfs, String scmKey) = Utils.FileSystemWrapper.fsFrom(job, listener, logger)

        then:
        notThrown(Exception)

        null != scmfs
        null != scmKey
    }

    def "Utils.FileSystemWrapper.fsFrom(job, listener, logger) !WorkflowMultiBranchProject,!CpsScmFlowDefinition"(){

        when:
        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(stdWorkflowJob);
        FlowExecution execution = build.execution
        TaskListener listener = execution.owner.listener
        PrintStream logger = listener.logger
        WorkflowJob job = execution.owner.executable.parent

        def(SCMFileSystem scmfs, String scmKey) = Utils.FileSystemWrapper.fsFrom(job, listener, logger)

        then:
        notThrown(Exception)

        null == scmfs
        null == scmKey
    }


    def "Utils.FileSystemWrapper.fsFrom(job, listener, logger) WorkflowMultiBranchProject"(){

        WorkflowMultiBranchProject workflowMultiBranchProject = groovyJenkinsRule.jenkins.createProject(WorkflowMultiBranchProject,
                "Utils.FileSystemWrapper.fsFrom WorkflowMultiBranchProject");


        workflowMultiBranchProject.getSourcesList().add(
                new BranchSource(new GitSCMSource(null, sampleRepo.toString(), "", "*", "", false),
                        new DefaultBranchPropertyStrategy(new BranchProperty[0])));


        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null

        GroovySpy(Utils.class, global:true)
        _ * Utils.getCurrentJob() >> { return job }
        _ * Utils.getListener() >> {return listener}
        _ * Utils.getLogger() >> {return logger}

        when:

        workflowMultiBranchProject.scheduleBuild2(0).getFuture().get();
        groovyJenkinsRule.waitUntilNoActivity();
        job = workflowMultiBranchProject.getItem("master");
        WorkflowRun build = job.getLastBuild()
        FlowExecution execution = build.execution
        listener = execution.owner.listener
        logger = listener.logger

        def(SCMFileSystem scmfs, String key) = Utils.FileSystemWrapper.fsFrom(job, listener, logger)

        then:
        notThrown(Exception)
        null != scmfs
        null != key
        groovyJenkinsRule.assertLogContains("branch=master", build);
    }

    def "Utils.FileSystemWrapper.fsFrom(job, listener, logger) WorkflowMultiBranchProject:dev/main"(){

        WorkflowMultiBranchProject mp = groovyJenkinsRule.jenkins.createProject(WorkflowMultiBranchProject.class, "wfmbp:dev/main");
        GitSCMSource source = new GitSCMSource(sampleRepo.toString());
        source.setTraits(Collections.singletonList(new BranchDiscoveryTrait()));
        mp.getSourcesList().add(new BranchSource(source));

        sampleRepo.git("checkout", "-b", "dev/main");
        sampleRepo.write(cpsScriptPath, cpsScript);
        sampleRepo.write(pipelineConfigPath, pipelineConfigScript)

        sampleRepo.write("Jenkinsfile", '''echo "branch=${env.BRANCH_NAME}"
                        node {
                          checkout scm
                          echo "workspace=${pwd().replaceFirst('.+dev', 'dev')}"
                          echo readFile('file')
                        };''');
        sampleRepo.write("file", "initial dev/main content"); ;
        sampleRepo.git("add", "*")
        sampleRepo.git("commit", "--all", "--message=dev/main");

        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null

        GroovySpy(Utils.class, global:true)
        _ * Utils.getCurrentJob() >> { return job }
        _ * Utils.getListener() >> {return listener}
        _ * Utils.getLogger() >> {return logger}

        when:

        mp.scheduleBuild2(0).getFuture().get();
        groovyJenkinsRule.waitUntilNoActivity();
        job = mp.getItem("dev/main"); // works with dev%2Fmain

        WorkflowRun build = job.getLastBuild()
        def ep = build.getExecutionPromise()
        FlowExecution execution = ep.get()
        listener = execution.owner.listener
        logger = listener.logger

        def(SCMFileSystem scmfs, String key) = Utils.FileSystemWrapper.fsFrom(job, listener, logger)

        then:
        notThrown(Exception)
        null != scmfs
        null != key
        groovyJenkinsRule.assertLogContains("branch=dev/main", build);
    }

    def "Utils.getFileContents"(){

        given:
        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null

        GroovySpy(Utils.class, global:true)
        _ * Utils.getCurrentJob() >> { return job }
        _ * Utils.getListener() >> {return listener}
        _ * Utils.getLogger() >> {return logger}

        when:
        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(scmWorkflowJob);
        FlowExecution execution = build.execution
        listener = execution.owner.listener
        logger = listener.logger
        job = execution.owner.executable.parent

        String output = Utils.getFileContents(cpsScriptPath, scm, "[JTE]")

        then:
        notThrown(Exception)

        output == cpsScript
    }

    def "Utils.getFileContents; no scm argument"(){

        WorkflowMultiBranchProject workflowMultiBranchProject = groovyJenkinsRule.jenkins.createProject(WorkflowMultiBranchProject,
                "Utils.getFileContents; no scm argument");


        workflowMultiBranchProject.getSourcesList().add(
                new BranchSource(new GitSCMSource(null, sampleRepo.toString(), "", "*", "", false),
                        new DefaultBranchPropertyStrategy(new BranchProperty[0])));


        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null

        GroovySpy(Utils.class, global:true)
        _ * Utils.getCurrentJob() >> { return job }
        _ * Utils.getListener() >> {return listener}
        _ * Utils.getLogger() >> {return logger}

        when:

        workflowMultiBranchProject.scheduleBuild2(0).getFuture().get();
        groovyJenkinsRule.waitUntilNoActivity();
        job = workflowMultiBranchProject.getItem("master");
        WorkflowRun build = job.getLastBuild()
        FlowExecution execution = build.execution
        listener = execution.owner.listener
        logger = listener.logger

        String output = Utils.getFileContents(pipelineConfigPath, null, "pipeline config")

        then:
        notThrown(Exception)
        output == pipelineConfigScript
    }


    def "SCMFileSystem.of; using jenkinsRule"(){// testing that setup of scm is correct

        when:
        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(scmWorkflowJob);
        FlowExecution execution = build.execution
        WorkflowJob job = execution.owner.executable.parent

        SCMFileSystem scmfs = SCMFileSystem.of(job, scm);

        then:
        notThrown(Exception)

        SCMFileSystem.supports(scm)
        null != scmfs
    }

    def "Utils.getFileContents; no scm argument; testing logging output"(){

        def project = groovyJenkinsRule.jenkins.createProject(WorkflowJob, "scmSandbox_project");
        project.setDefinition(new SandboxCpsScmFlowDefinition(scm, cpsScriptPath2));

        when:

        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(project);



        then:
        notThrown(Exception)
        groovyJenkinsRule.assertLogNotContains("[inferred]", build);
        groovyJenkinsRule.assertLogContains(pipelineConfigPath, build);
        groovyJenkinsRule.assertLogContains("template configuration file", build);
        groovyJenkinsRule.assertLogContains(pipelineConfigScript, build);

    }

    static class SandboxCpsScmFlowDefinition extends CpsScmFlowDefinition {

        public SandboxCpsScmFlowDefinition(SCM scm, String scriptPath) {
            super(scm, scriptPath)
        }

        @Override
        CpsFlowExecution create(FlowExecutionOwner owner, TaskListener listener, List<? extends Action> actions) throws Exception {
            CpsFlowExecution ex = super.create(owner, listener, actions)

            return new CpsFlowExecution(ex.script, false, ex.owner, ex.durabilityHint)
        }
    }

}
