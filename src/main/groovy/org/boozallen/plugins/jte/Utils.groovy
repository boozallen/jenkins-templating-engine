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

package org.boozallen.plugins.jte

import org.boozallen.plugins.jte.binding.TemplateBinding
import org.boozallen.plugins.jte.config.*
import org.jenkinsci.plugins.workflow.cps.CpsThread
import org.jenkinsci.plugins.workflow.cps.CpsThreadGroup
// for get branch file utils 
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty
import jenkins.branch.Branch
import hudson.model.ItemGroup
import jenkins.scm.api.SCMHead
import jenkins.scm.api.SCMRevision
import jenkins.scm.api.SCMFileSystem
import jenkins.scm.api.SCMSource
import jenkins.scm.api.SCMFile
import jenkins.scm.api.SCMRevisionAction
import hudson.scm.SCM
import hudson.Functions
import hudson.FilePath
import hudson.model.Node
import hudson.model.TopLevelItem
import hudson.model.Computer
import org.jenkinsci.plugins.workflow.steps.scm.GenericSCMStep
import org.jenkinsci.plugins.workflow.steps.scm.SCMStep
import org.jenkinsci.plugins.workflow.support.actions.WorkspaceActionImpl
import hudson.slaves.WorkspaceList
import org.jenkinsci.plugins.workflow.steps.scm.SCMStep
import jenkins.model.Jenkins
import hudson.AbortException
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import hudson.model.Queue
import java.io.PrintStream
import hudson.model.TaskListener
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.flow.FlowDefinition
import org.jenkinsci.plugins.workflow.cps.CpsThreadGroup
import org.jenkinsci.plugins.workflow.cps.CpsGroovyShell
import java.lang.reflect.Field
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.codehaus.groovy.control.CompilerConfiguration
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted 
import org.jenkinsci.plugins.github_branch_source.BranchSCMHead
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead


class Utils implements Serializable{

    static TaskListener listener
    static FlowExecutionOwner owner 
    static WorkflowRun build 
    static PrintStream logger 
    static WorkflowJob currentJob 

    /*
        We do a lot of executing the code inside files and we're also
        dependent on the binding being preserved (as it stores step impls).

        The default CpsGroovyShell leveraged in CpsScript's evaluate method
        is insufficient for our needs because it instantiates each shell with
        a new Binding() instead of using getBinding().

        Of course, there's no setContext() method on GroovyShell or 
        CpsGroovyShell to override the binding used in the constructor, 
        so we've gotta use reflection to override it directly. 

        /rant
    */
    static Script parseScript(String scriptText, Binding b){
        
        // get shell for parsing step 
        /*
            technically returns a CpsGroovyShell.. accessing that class
            during runtime results in IllegalAccessError. 

            kinda silly all you have to do bypass that is cast to the superclass. 
        */
        GroovyShell shell = CpsThreadGroup.current().getExecution().getTrustedShell()

        // make the shell's CompilerConfiguration accessible
        Field configF = GroovyShell.class.getDeclaredField("config")
        configF.setAccessible(true)

        /*
            TODO: 
                this should probably be an extension point to allow additional 
                boiler plate when parsing files into scripts for invocation? 

                TemplateScriptCustomizer.all() returning List<CompilationCustomizer>

        */

        // define auto importing of JTE hook annotations
        ImportCustomizer ic = new ImportCustomizer()
        ic.addStarImports("org.boozallen.plugins.jte.hooks")
        CompilerConfiguration cc = configF.get(shell)
        cc.addCompilationCustomizers(ic)

        // modify the shell 
        configF.set(shell, cc)

        // parse the script 
        Script script = shell.getClassLoader().parseClass(scriptText).newInstance()

        // set the script binding to our TemplateBinding
        script.setBinding(b)

        return script 
    }

    static PrintStream getLogger(){
        if (logger){
            return logger 
        }
        getCurrentJob()
        return logger 
    }

    static TaskListener getListener(){
        if (listener){
            return listener
        }
        getCurrentJob()
        return listener 
    }

    /*
        get a file contents from an SCM source

        if no SCM is supplied, well try to infer it from the job
        
        return null if file not present 
        throw exception if checkout failed or filePath is directory 
    */
    static String getFileContents(String filePath, SCM scm, String loggingDescription){
        
        String file 
        
        WorkflowJob job = getCurrentJob()
        ItemGroup<?> parent = job.getParent()
        PrintStream logger = getLogger() 

        // create SCMFileSystem 
        SCMFileSystem fs = null

        // if provided a SCM, try to build directly:
        if (scm){
            if (SCMFileSystem.supports(scm)){
                fs = SCMFileSystem.of(job, scm)
            }else{
                return null
            }
        }else{ // try to infer SCM info from job properties

            fs = FileSystemWrapper.fsFrom(job, listener, logger)
        }

        if (fs){
            FileSystemWrapper fsw = new FileSystemWrapper(fs: fs, log: new Logger(desc: loggingDescription), scmKey: scm?.key)
            return fsw.getFileContents(filePath)
        }

        return null 
    }

    /*
        this code is not the greatest. TODO: refactor
    */
    static SCMFileSystem createSCMFileSystemOrNull(SCM scm, WorkflowJob job, ItemGroup<?> parent, PrintStream logger = getLogger() ){

        if (scm){
            try{
                return SCMFileSystem.of(job, scm)
            }catch(any){
                logger.println any 
                return null 
            }
        }else{              
            return FileSystemWrapper.fsFrom(job, listener, logger)
        } 
    }

    /**
     * @param scm
     * @param job
     * @param logger optional a printStream to send error/logging messages
     * @return null or a valid SCMFileSystem
    */
    static SCMFileSystem getSCMFileSystemOrNull(SCM scm, WorkflowJob job, PrintStream logger = getLogger() ){

        if (scm){
            try{
                return SCMFileSystem.of(job, scm)
            }catch(any){
                logger.println any
                return null
            }
        }else{
            return FileSystemWrapper.fsFrom(job, listener, logger)
        }
    }


    /**
     * Note: this method initializes the class/CpsThread level job,logger,listener properties
     * @return the job for the current CPS thread
     * @throws IllegalStateException if is called outside of a CpsThread or is not executed in a WorkflowRun
     */
    static WorkflowJob getCurrentJob(){

        if (currentJob){
            return currentJob
        }

        // assumed this is being run from a job
        CpsThread thread = CpsThread.current()
        if (!thread){
            throw new IllegalStateException("CpsThread not present")
        }

        this.owner = thread.getExecution().getOwner()
        this.listener = owner.getListener()    
        this.logger = listener.getLogger()   

        Queue.Executable exec = owner.getExecutable()
        if (!(exec instanceof WorkflowRun)) {
            throw new IllegalStateException("Must be run from a WorkflowRun, found: ${exec.getClass()}")
        }

        this.build = (WorkflowRun) exec
        WorkflowJob job = build.getParent()

        return job 
    }

    @Whitelisted
    static String getTemplate(Map config){

        // tenant Jenkinsfile if allowed 
        String repoJenkinsfile = getFileContents("Jenkinsfile", null, "Repository Jenkinsfile")
        if (repoJenkinsfile){
            if (config.allow_scm_jenkinsfile){
                return repoJenkinsfile
            }else{
                getLogger().println "[JTE] Warning: Repository provided Jenkinsfile that will not be used, per organizational policy."
            }
        }

        // specified pipeline template from pipeline template directories in governance tiers
        List<GovernanceTier> tiers = GovernanceTier.getHierarchy()
        if (config.pipeline_template){ 
            for (tier in tiers){
                String pipelineTemplate = tier.getTemplate(config.pipeline_template)
                if (pipelineTemplate){
                    return pipelineTemplate 
                }
            }
            throw new TemplateConfigException("Pipeline Template ${config.pipeline_template} could not be found in hierarchy.")
        }

        /*
            look for default Jenkinsfile in ascending order of governance tiers
        */
        for (tier in tiers){
            String pipelineTemplate = tier.getJenkinsfile()
            if (pipelineTemplate){
                return pipelineTemplate 
            }
        }

        throw new TemplateConfigException("Could not determine pipeline template.")

    }

    @Whitelisted
    static void findAndRunTemplate(LinkedHashMap pipelineConfig, TemplateBinding binding){
        String template = getTemplate(pipelineConfig)
        parseScript(template, binding).run() 
    }

    static class Logger {

        String desc = ""
        String key = ""
        String prologue = "[JTE] "
        PrintStream printStream

        PrintStream getLogger() {
            return printStream ?: Utils.getLogger()
        }
    }

    static class JTEException extends Exception {
        JTEException(String message){
            super(message)
        }

        JTEException(String message, Throwable t){
            super(message, t)
        }

        JTEException(Throwable t){
            super(t)
        }
    }

    static class FileSystemWrapper {// inner class
        SCMFileSystem fs
        String scmKey
        Logger log

        // move these into a logging related class
        boolean logMissingFile = true

        Logger getLogger(){
            log ?: new Logger(key: scmKey)
        }

        static SCMFileSystem fsFrom(WorkflowJob job, TaskListener listener, PrintStream logger){
            ItemGroup<?> parent = job.getParent()
            try {
                if (parent instanceof WorkflowMultiBranchProject) {
                    // ensure branch is defined
                    BranchJobProperty property = job.getProperty(BranchJobProperty.class)
                    if (!property) {
                        throw new JTEException("inappropriate context") // removed IllegalStateEx as an example
                    }
                    Branch branch = property.getBranch()

                    // get scm source for specific branch and ensure present
                    // (might not be if branch deleted after job triggered)
                    SCMSource scmSource = parent.getSCMSource(branch.getSourceId())
                    if (!scmSource) {
                        throw new JTEException(new IllegalStateException("${branch.getSourceId()} not found"))
                    }

                    SCMHead head = branch.getHead()
                    SCMRevision tip = scmSource.fetch(head, listener)

                    if (tip) {
                        SCMRevision rev = scmSource.getTrustedRevision(tip, listener)
                        return SCMFileSystem.of(scmSource, head, rev)
                    } else {
                        SCM scm = branch.getScm()
                        return SCMFileSystem.of(job, scm)
                    }
                } else {
                    FlowDefinition definition = job.getDefinition()
                    if (definition instanceof CpsScmFlowDefinition) {
                        SCM scm = definition.getScm()
                        return SCMFileSystem.of(job, scm)
                    } else {
                        return null
                    }
                }
            }catch(JTEException jteex){//throw our exception
                throw (jteex.cause ?: jteex)
            }catch(any){// ignore but print every other exception
                logger.println any
            }

            return null
        }

        String getFileContents(String filePath){
            return FileSystemWrapper.getFileContents( filePath, fs, logger, logMissingFile)
        }

        static String getFileContents(String filePath, SCMFileSystem fs,
                                      Logger log = new Logger(), boolean logMissingFile = true ){

            if (fs){
                try {
                    SCMFile f = fs.child(filePath)
                    if (!f.exists()){
                        if( logMissingFile ) {
                            log?.logger?.println "${log?.prologue}${filePath} does not exist"
                        }
                        return null
                    }
                    if(!f.isFile()){
                        log?.logger?.println "${log?.prologue}${filePath} is not a file."
                        return null
                        //throw new JTEException("${filePath} is not a file.")
                    }
                    if (log?.desc){
                        log?.logger?.println "${log?.prologue}Obtained ${log?.desc} ${filePath} from ${log?.key ?:"[inferred]"}"
                    }

                    return f.contentAsString()

                } catch(any) {
                    log?.logger.println "${log?.prologue}exception ${any} for ${filePath} from ${log?.key ?:"[inferred]"}"
                } finally{
                    fs.close()
                }
            }

            return null
        }

    }
}