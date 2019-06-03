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
import org.boozallen.plugins.jte.console.TemplateLogger
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
               import org.boozallen.plugins.jte.utils.*

            def fsw = FileSystemWrapper.createFromJob()
            println fsw.getFileContents('pipeline_config.groovy', "template configuration file")               

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



    @WithoutJenkins
    def "initialize FileSystemWrapper with scm; using RunUtils.job"(){
        given: "a workflowjob project with a valid scm"

        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null

        GroovySpy(RunUtils, global:true)
        _ * RunUtils.getJob() >> { return job }
        _ * RunUtils.getListener() >> {return listener}
        _ * RunUtils.getLogger() >> {return logger}

        GroovyMock(TemplateLogger, global:true)
        _ * TemplateLogger.print(_,_)

        FileSystemWrapper fsw = null

        when:"new FileSystemWrapper(scm) is called with the project's job"
        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(scmWorkflowJob);
        FlowExecution execution = build.execution
        listener = execution.owner.listener
        logger = listener.logger
        job = execution.owner.executable.parent

        fsw = FileSystemWrapper.createFromSCM(scm)

        then:"it should return a valid SCM filesystem"
        notThrown(Exception)
        null != fsw.fs
        null != fsw.scmKey
        scm.key == fsw.scmKey
    }

    @WithoutJenkins
    def "initialize FileSystemWrapper with no scm; using RunUtils.job"(){
        given: "a workflowjob project with a valid scm"

        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null

        GroovySpy(RunUtils, global:true)
        _ * RunUtils.getJob() >> { return job }
        _ * RunUtils.getListener() >> {return listener}
        _ * RunUtils.getLogger() >> {return logger}

        GroovyMock(TemplateLogger, global:true)
        _ * TemplateLogger.print(_,_)

        FileSystemWrapper fsw = null

        when:"Utils.scmFileSystemOrNull is called with the project's job and *no* scm"
        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(scmWorkflowJob);
        FlowExecution execution = build.execution
        listener = execution.owner.listener
        logger = listener.logger
        job = execution.owner.executable.parent

        fsw = FileSystemWrapper.createFromJob()


        then:"it should return a valid SCM filesystem"
        notThrown(Exception)
        null != fsw.fs
        null != fsw.scmKey
        groovyJenkinsRule.assertLogNotContains("[inferred]", build);
        groovyJenkinsRule.assertLogContains(fsw.scmKey, build);

    }

    def "getFileContents when FileSystemWrapper#fs == null"(){
        given:
        FileSystemWrapper fsw = new FileSystemWrapper()

        when:
        String res = fsw.getFileContents(pipelineConfigPath)

        then:
        null == res
    }

    @WithoutJenkins
    def "FileSystemWrapper#fsFrom(job); job: !WorkflowMultiBranchProject"(){

        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null

        GroovySpy(RunUtils, global:true)
        _ * RunUtils.getJob() >> { return job }
        _ * RunUtils.getListener() >> {return listener}
        _ * RunUtils.getLogger() >> {return logger}

        GroovyMock(TemplateLogger, global:true)
        _ * TemplateLogger.print(_,_)

        when:
        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(scmWorkflowJob);
        FlowExecution execution = build.execution
        listener = execution.owner.listener
        logger = listener.logger
        job = execution.owner.executable.parent

        def(SCMFileSystem scmfs, String scmKey) = (new FileSystemWrapper()).fsFrom(job)

        then:
        notThrown(Exception)

        null != scmfs
        null != scmKey
    }

    def "FileSystemWrapper#fsFrom(job); job: !WorkflowMultiBranchProject,!CpsScmFlowDefinition"(){

        when:
        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(stdWorkflowJob);
        FlowExecution execution = build.execution
        TaskListener listener = execution.owner.listener
        PrintStream logger = listener.logger
        WorkflowJob job = execution.owner.executable.parent

        GroovySpy(RunUtils, global:true)
        _ * RunUtils.getJob() >> { return job }
        _ * RunUtils.getListener() >> {return listener}
        _ * RunUtils.getLogger() >> {return logger}

        GroovyMock(TemplateLogger, global:true)
        _ * TemplateLogger.print(_,_)

        def(SCMFileSystem scmfs, String scmKey) = (new FileSystemWrapper()).fsFrom(job)

        then:
        notThrown(Exception)

        null == scmfs
        null == scmKey
    }


    def "FileSystemWrapper#fsFrom(job); job: WorkflowMultiBranchProject"(){

        WorkflowMultiBranchProject workflowMultiBranchProject = groovyJenkinsRule.jenkins.createProject(WorkflowMultiBranchProject,
                "Utils.FileSystemWrapper.fsFrom WorkflowMultiBranchProject");


        workflowMultiBranchProject.getSourcesList().add(
                new BranchSource(new GitSCMSource(null, sampleRepo.toString(), "", "*", "", false),
                        new DefaultBranchPropertyStrategy(new BranchProperty[0])));


        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null

        GroovySpy(RunUtils, global:true)
        _ * RunUtils.getJob() >> { return job }
        _ * RunUtils.getListener() >> {return listener}
        _ * RunUtils.getLogger() >> {return logger}

        GroovyMock(TemplateLogger, global:true)
        _ * TemplateLogger.print(_,_)

        when:

        workflowMultiBranchProject.scheduleBuild2(0).getFuture().get();
        groovyJenkinsRule.waitUntilNoActivity();
        job = workflowMultiBranchProject.getItem("master");
        WorkflowRun build = job.getLastBuild()
        FlowExecution execution = build.execution
        listener = execution.owner.listener
        logger = listener.logger

        def(SCMFileSystem scmfs, String scmKey) = (new FileSystemWrapper()).fsFrom(job)

        then:
        notThrown(Exception)
        null != scmfs
        null != scmKey
        groovyJenkinsRule.assertLogContains("branch=master", build);
    }

    def "FileSystemWrapper#fsFrom(job); job: WorkflowMultiBranchProject:dev/main"(){

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

        GroovySpy(RunUtils, global:true)
        _ * RunUtils.getJob() >> { return job }
        _ * RunUtils.getListener() >> {return listener}
        _ * RunUtils.getLogger() >> {return logger}

        GroovyMock(TemplateLogger, global:true)
        _ * TemplateLogger.print(_,_)

        when:

        mp.scheduleBuild2(0).getFuture().get();
        groovyJenkinsRule.waitUntilNoActivity();
        job = mp.getItem("dev/main"); // works with dev%2Fmain

        WorkflowRun build = job.getLastBuild()
        def ep = build.getExecutionPromise()
        FlowExecution execution = ep.get()
        listener = execution.owner.listener
        logger = listener.logger

        def(SCMFileSystem scmfs, String scmKey) = (new FileSystemWrapper()).fsFrom(job)

        then:
        notThrown(Exception)
        null != scmfs
        null != scmKey
        groovyJenkinsRule.assertLogContains("branch=dev/main", build);
    }

    def "FileSystemWrapper#getFileContents"(){

        given:
        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null

        GroovySpy(RunUtils, global:true)
        _ * RunUtils.getJob() >> { return job }
        _ * RunUtils.getListener() >> {return listener}
        _ * RunUtils.getLogger() >> {return logger}

        GroovyMock(TemplateLogger, global:true)
        _ * TemplateLogger.print(_,_)

        FileSystemWrapper fsw = null

        when:
        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(scmWorkflowJob);
        FlowExecution execution = build.execution
        listener = execution.owner.listener
        logger = listener.logger
        job = execution.owner.executable.parent

        fsw = FileSystemWrapper.createFromSCM(scm)
        String output = fsw.getFileContents(cpsScriptPath, "[JTE]")

        then:
        notThrown(Exception)

        output == cpsScript
    }

    def "FileSystemWrapper#getFileContents; no scm"(){

        WorkflowMultiBranchProject workflowMultiBranchProject = groovyJenkinsRule.jenkins.createProject(WorkflowMultiBranchProject,
                "Utils.getFileContents; no scm argument");


        workflowMultiBranchProject.getSourcesList().add(
                new BranchSource(new GitSCMSource(null, sampleRepo.toString(), "", "*", "", false),
                        new DefaultBranchPropertyStrategy(new BranchProperty[0])));


        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null

        GroovySpy(RunUtils, global:true)
        _ * RunUtils.getJob() >> { return job }
        _ * RunUtils.getListener() >> {return listener}
        _ * RunUtils.getLogger() >> {return logger}

        GroovyMock(TemplateLogger, global:true)
        _ * TemplateLogger.print(_,_)

        FileSystemWrapper fsw = null

        when:

        workflowMultiBranchProject.scheduleBuild2(0).getFuture().get();
        groovyJenkinsRule.waitUntilNoActivity();
        job = workflowMultiBranchProject.getItem("master");
        WorkflowRun build = job.getLastBuild()
        FlowExecution execution = build.execution
        listener = execution.owner.listener
        logger = listener.logger

        fsw = FileSystemWrapper.createFromJob()
        String output = fsw.getFileContents(pipelineConfigPath, "pipeline config")

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

    def "getFileContents in script; no scm argument; testing logging output"(){

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
