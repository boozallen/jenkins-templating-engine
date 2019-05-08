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

package org.boozallen.plugins.jte.utils

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

class FileSystemWrapper {
    SCM scm 
    SCMFileSystem fs
    String scmKey

    FileSystemWrapper(SCM scm = null){
        this.scm = scm 
        (fs, scmKey) = createSCMFileSystem(scm)
    }

    /*
        retrieves a SCMFile if present.
        if ignoreMissing = true, missing files arent logged. 
        returns null if file not present
    */
    String getFileContents(String filePath, String loggingDescription = null, Boolean logMissingFile = true){
        if (!fs){
            return null 
        }
        SCMFile f = fs.child(filePath)
        if (!f.exists()){
            if( logMissingFile ){
                TemplateLogger.printWarning("""${filePath} does not exist.
                                                -- scm: ${scmKey}""", true)
            }
            return null
        }
        if(!f.isFile()){
            TemplateLogger.printWarning("""${filePath} exists but is not a file.
                                            -- scm: ${scmKey}""", true) 
            return null
        }
        if (loggingDescription){
            TemplateLogger.print("""Obtained ${loggingDescription}
                                    -- scm: ${scmKey}
                                    -- file path: ${filePath}""", true) 
        }
        fs.close()
        return f.contentAsString()
    }



    static SCMFileSystem createSCMFileSystem(SCM scm = null){
        WorkflowJob job = Utils.getCurrentJob()
        if(scm){
            try{
                return SCMFileSystem.of(job, scm)
            }catch(any){
                Utils.getLogger().println any
                return null 
            }
        }else{
            return fsFrom(job)
        }
    }

    /*
        return[0]: SCMFileSystem
        return[1]: String: key from scm
    */
    static def fsFrom(WorkflowJob job){
        ItemGroup<?> parent = job.getParent()
        TaskListener listener = Utils.getListener()
        String key = null
        SCMFileSystem fs = null

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

                key = branch.getScm().getKey()

                if (tip) {
                    SCMRevision rev = scmSource.getTrustedRevision(tip, listener)
                    fs = SCMFileSystem.of(scmSource, head, rev)
                    return [fs, key]
                } else {
                    SCM scm = branch.getScm()
                    fs = SCMFileSystem.of(job, scm)
                    return [fs, key]
                }
            } else {
                FlowDefinition definition = job.getDefinition()
                if (definition instanceof CpsScmFlowDefinition) {
                    SCM scm = definition.getScm()
                    key = scm.getKey()
                    fs = SCMFileSystem.of(job, scm)
                    return [fs, key]
                } else {
                    return [fs, key]
                }
            }
        }catch(JTEException jteex){//throw our exception
            throw (jteex.cause ?: jteex)
        }catch(any){// ignore but print every other exception
            TemplateLogger.printWarning(any.toString())
        }

        return [fs, key]
    }

}