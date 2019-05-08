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
import hudson.scm.SCM
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import hudson.model.Queue
import hudson.model.TaskListener
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import org.jenkinsci.plugins.workflow.flow.FlowDefinition

import java.lang.reflect.Field
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.codehaus.groovy.control.CompilerConfiguration
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted 

class Utils implements Serializable{

    static TaskListener listener
    static FlowExecutionOwner owner 
    static WorkflowRun build 
    static PrintStream logger 
    static WorkflowJob currentJob 

    static PrintStream getLogger(){
        getCurrentJob()
        return logger 
    }

    static TaskListener getListener(){
        getCurrentJob()
        return listener 
    }

    /**
     * Note: this method initializes the class/CpsThread level job,logger,listener properties
     * @return the job for the current CPS thread
     * @throws IllegalStateException if is called outside of a CpsThread or is not executed in a WorkflowRun
     */
    static WorkflowJob getCurrentJob(){
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
        this.currentJob = build.getParent()

        return currentJob
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