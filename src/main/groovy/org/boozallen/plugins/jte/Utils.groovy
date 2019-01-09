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
        return CpsThread.current().getExecution().getOwner().getListener().getLogger()
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

        if (!scm) scm = getSCMFromJob(job, parent)

        // try lightweight checkout 
        SCMFileSystem fs = createSCMFileSystemOrNull(scm, job, parent)
        if (fs){
            try {
                SCMFile f = fs.child(filePath)
                if (!f.exists()){
                    logger.println "[JTE] ${filePath} does not exist"
                    return null 
                }
                if(!f.isFile()){
                    throw new Exception("${filePath} is not a file.")
                } 
                if (loggingDescription){
                    logger.println "[JTE] Obtained ${loggingDescription} ${filePath} from ${scm.getKey()}"
                }
                return f.contentAsString()
            } catch(any) {

            } finally{
                fs.close() 
            }
            
        }

        // lightweight apparently didn't work out.
        if (scm){
            FilePath dir = doHeavyWeightCheckout(scm, job, parent) 
            FilePath configFile = dir.child(filePath)
            if (!configFile.absolutize().getRemote().replace('\\', '/').startsWith(dir.absolutize().getRemote().replace('\\', '/') + '/')) { // TODO JENKINS-26838
                throw new IOException("${configFile} is not inside ${dir}")
            }
            if (!configFile.exists()) {
                return null 
            }
            if (loggingDescription){
                logger.println "[JTE] Obtained ${loggingDescription} ${filePath} from ${scm.getKey()}"
            }
            return configFile.readToString()
        }

        return null 
    }

    /*
        attempts to checkout the SCM to a node.
        returns the directory w/ the SCM contents. 
    */
    static FilePath doHeavyWeightCheckout(SCM scm, WorkflowJob job, ItemGroup<?> parent, FilePath providedDir = null){
        FilePath dir
        Node node = Jenkins.getActiveInstance()
        PrintStream logger = getLogger() 
        if (providedDir){
            dir = providedDir 
        }else{
            if (job instanceof TopLevelItem) {
                FilePath baseWorkspace = node.getWorkspaceFor((TopLevelItem) job)
                if (!baseWorkspace) {
                    throw new IOException("${node.getDisplayName()} may be offline")
                }
                dir = baseWorkspace.withSuffix("${System.getProperty(WorkspaceList.class.getName(), "@")}script")
            } else { // should not happen, but just in case:
                dir = new FilePath(owner.getRootDir())
            }
        }
        Computer computer = node.toComputer()
        if (!computer) {
            throw new IOException("${node.getDisplayName()} may be offline")
        }
        SCMStep delegate = new GenericSCMStep(scm)
        WorkspaceList.Lease lease = computer.getWorkspaceList().acquire(dir)

        // do the checkout.  try the number of times configured in Jenkins
        for (int retryCount = Jenkins.getInstance().getScmCheckoutRetryCount(); retryCount >= 0; retryCount--) {
            try {
                delegate.checkout(build, dir, listener, node.createLauncher(listener))
                break
            } catch (AbortException e) {
                // abort exception might have a null message.
                // If so, just skip echoing it.
                if (e.getMessage()) {
                    logger.println e.getMessage()
                }
            } catch (InterruptedIOException e) {
                throw e
            } catch (IOException e) {
                logger.println "Checkout failed: ${e}"
            }

            if (retryCount == 0){
                // all attempts failed
                throw new AbortException("Maximum checkout retry attempts reached, aborting")
            }
                
            logger.println("Retrying after 10 seconds")
            Thread.sleep(10000)
        }

        return dir 
    }

    /*
        returns an SCM from the current running job 
        null in case of regular pipeline job
    */
    static SCM getSCMFromJob(WorkflowJob job, ItemGroup<?> parent){
        def logger = getLogger() 
        if (parent instanceof WorkflowMultiBranchProject){
            // ensure branch is defined 
            BranchJobProperty property = job.getProperty(BranchJobProperty.class)
            if (!property){
                throw new IllegalStateException("inappropriate context")
            }

            Branch branch = property.getBranch()

            // get scm source for specific branch and ensure present
            // (might not be if branch deleted after job triggered)
            String branchName = branch.getSourceId()
            SCMSource scmSource = parent.getSCMSource(branchName)
            if (!scmSource) {
                throw new IllegalStateException("${branch.getSourceId()} not found")
            }

            // attempt lightweight checkout
            /*
                some hacky stuff here.. can't make GitSCMFileSystem from a PR.. so if PR -> use source branch
            */ 
            SCMHead head = branch.getHead()
            if (head instanceof PullRequestSCMHead){
                head = new BranchSCMHead(head.sourceBranch)
            }
            SCMRevision tip = scmSource.fetch(head, listener)

            if (tip){
                SCMRevision rev = scmSource.getTrustedRevision(tip, listener)
                return scmSource.build(head,rev) 
            }else{
                return branch.getScm()
            }
        } else {
            FlowDefinition definition = job.getDefinition() 
            if (definition instanceof CpsScmFlowDefinition){
                return definition.getScm()
            }else{
                return null 
            }
        }         
    }

    /*
        this code is not the greatest. TODO: refactor
    */
    static SCMFileSystem createSCMFileSystemOrNull(SCM scm, WorkflowJob job, ItemGroup<?> parent){
        PrintStream logger = getLogger() 
        if (scm){
            try{
                return SCMFileSystem.of(job, scm)
            }catch(any){
                logger.println any 
                return null 
            }
        }else{              
            if (parent instanceof WorkflowMultiBranchProject){
                // ensure branch is defined 
                BranchJobProperty property = job.getProperty(BranchJobProperty.class)
                if (!property){
                    throw new IllegalStateException("inappropriate context")
                }
                Branch branch = property.getBranch()

                // get scm source for specific branch and ensure present
                // (might not be if branch deleted after job triggered)
                SCMSource scmSource = parent.getSCMSource(branch.getSourceId())
                if (!scmSource) {
                    throw new IllegalStateException("${branch.getSourceId()} not found")
                }

                SCMHead head = branch.getHead()
                SCMRevision tip = scmSource.fetch(head, listener)
                if (tip){
                    SCMRevision rev = scmSource.getTrustedRevision(tip, listener)
                    try{
                        return SCMFileSystem.of(scmSource, head, rev)
                    }catch(any){
                        logger.println any 
                        return null 
                    }
                }else{
                    scm = branch.getScm()
                    try{
                        return SCMFileSystem.of(job, scm)
                    }catch(any){
                        logger.println any 
                        return null 
                    }
                }
            } else {
                FlowDefinition definition = job.getDefinition() 
                if (definition instanceof CpsScmFlowDefinition){
                    scm = definition.getScm()
                    try{
                        return SCMFileSystem.of(job, scm)
                    }catch(any){
                        logger.println any 
                        return null 
                    }
                }else{
                    return null 
                }
            }
        } 
    }

    static WorkflowJob getCurrentJob(){
        // assumed this is being run from a job
        CpsThread thread = CpsThread.current()
        if (!thread){
            throw new IllegalStateException("CpsThread not present")
        }

        this.owner = thread.getExecution().getOwner()
        this.listener = owner.getListener()      

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

}