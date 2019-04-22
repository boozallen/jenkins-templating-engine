package org.boozallen.plugins.jte.binding

import hudson.model.TaskListener
import hudson.plugins.git.BranchSpec
import hudson.plugins.git.GitSCM
import hudson.plugins.git.SubmoduleConfig
import hudson.plugins.git.extensions.GitSCMExtension
import hudson.scm.SCM
import jenkins.plugins.git.GitSampleRepoRule
import jenkins.scm.api.SCMFile
import jenkins.scm.api.SCMFileSystem
import org.boozallen.plugins.jte.Utils
import org.boozallen.plugins.jte.config.GovernanceTier
import org.boozallen.plugins.jte.config.TemplateConfigObject
import org.boozallen.plugins.jte.config.TemplateLibrarySource
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.workflow.flow.FlowExecution
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.junit.ClassRule
import org.jvnet.hudson.test.GroovyJenkinsRule
import spock.lang.Shared
import spock.lang.Specification

class LibraryLoaderSpec extends Specification {
    @Shared @ClassRule
    @SuppressWarnings('JUnitPublicField')
    public GroovyJenkinsRule groovyJenkinsRule = new GroovyJenkinsRule()

    @Shared
    @ClassRule GitSampleRepoRule sampleRepo = new GitSampleRepoRule()

    @Shared
    SCM scm = null

    @Shared
    String echoText = "printing echo"

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
            echox
        }
    """

    @Shared
    private WorkflowJob scmWorkflowJob = null

    @Shared
    CpsScmFlowDefinition cpsScmFlowDefinition = null

    @Shared
    String libsBaseDir = "sub-libs"

    @Shared
    String echoLibName = "echox"

    @Shared
    String echoStepName = "echo"

    def setupSpec(){
        // initialize repository
        sampleRepo.init()

        sampleRepo.write(cpsScriptPath, cpsScript);
        sampleRepo.write(pipelineConfigPath, pipelineConfigScript)

        // added for WorkflowMultibranchProject
        sampleRepo.write("Jenkinsfile", "echo \"branch=master\"; node {checkout scm; echo readFile('file')}");
        sampleRepo.write("file", "initial content");
        sampleRepo.write("${libsBaseDir}/${echoLibName}/${echoStepName}.groovy", "echo '${echoText}'");

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


    }

    def "librarysource.prefixBaseDir: with base directory"(){
        given:

        TemplateLibrarySource librarySource = new TemplateLibrarySource();// the thing being tested
        librarySource.baseDir = libsBaseDir
        librarySource.scm = scm

        when:
        String libPath = librarySource.prefixBaseDir(echoLibName)


        then:
        libPath != echoLibName
        libPath == "${libsBaseDir}/${echoLibName}"
    }

    def "librarysource.prefixBaseDir: without base directory"(){
        given:
        TemplateLibrarySource librarySource = new TemplateLibrarySource();// the thing being tested

        when:
        String libPath = librarySource.prefixBaseDir(echoLibName)

        then:
        libPath == echoLibName
    }

    def "Get Library directory: with source base directory"(){
        given:
        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null

        TemplateLibrarySource librarySource = new TemplateLibrarySource();// the thing being tested
        librarySource.baseDir = libsBaseDir
        librarySource.scm = scm

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

        SCMFileSystem fs = Utils.scmFileSystemOrNull(scm, job)
        String libPath = librarySource.prefixBaseDir(echoLibName)
        SCMFile libDir = fs.child(libPath)


        then:
        job != null
        fs != null

        true == libDir.exists()
        true == libDir.isDirectory()

    }

    def "Get Library step file: with base directory"(){
        given:
        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null

        TemplateLibrarySource librarySource = new TemplateLibrarySource();// the thing being tested
        librarySource.baseDir = libsBaseDir
        librarySource.scm = scm

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

        SCMFileSystem fs = Utils.scmFileSystemOrNull(scm, job)
        String libPath = librarySource.prefixBaseDir(echoLibName)
        SCMFile libDir = fs.child(libPath)
        boolean hasFile = false
        String content = null

        libDir.children().findAll{ it.getName().endsWith(".groovy") }.each { step ->
            if( echoStepName == (step.getName() - ".groovy")){
                hasFile = true
                content = step.contentAsString()
            }
        }


        then:
        true == hasFile
        null != content
        content.contains(echoText)

    }

    def "LibraryLoader.doInject; Library step file: with base directory"(){
        given:
        WorkflowJob job = null
        TaskListener listener = null
        PrintStream logger = null
        Script utilsScript = null

        TemplateLibrarySource librarySource = new TemplateLibrarySource();// the thing being tested
        librarySource.baseDir = libsBaseDir
        librarySource.scm = scm

        GovernanceTier tier = GroovyMock(GovernanceTier.class, global:true)
        _ * tier.getLibrarySources() >> { return Arrays.asList(librarySource) }
        _ * tier.scm >> { return scm }
        _ * tier.baseDir >> { return "" }
        _ * GovernanceTier.getHierarchy() >> { return [tier] }

        GroovySpy(Utils.class, global:true)
        _ * Utils.getCurrentJob() >> { return job }
        _ * Utils.getListener() >> {return listener}
        _ * Utils.getLogger() >> {return logger}
        _ * Utils.parseScript(_, _) >> { return utilsScript }

        def returnMap = [:]
        LinkedHashMap configMap = [libraries:[echox:returnMap]]
        TemplateConfigObject configObject = GroovyMock(TemplateConfigObject)
        configObject.config >> {return configMap}

        CpsScript script = new CpsScript() {
            @Override
            Object run() {
                return null
            }
        }

        utilsScript = new CpsScript() {
            @Override
            Object run() {
                return null
            }

            def call(Object... args){}
        }

        when:
        WorkflowRun build = groovyJenkinsRule.buildAndAssertSuccess(scmWorkflowJob);
        FlowExecution execution = build.execution
        listener = execution.owner.listener
        logger = listener.logger
        job = execution.owner.executable.parent

        LibraryLoader.doInject(configObject, script)


        then:
        null != script.getBinding().getVariable(echoStepName)

    }

}
