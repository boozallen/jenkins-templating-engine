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

package org.boozallen.plugins.jte.config

import java.io.IOException

import org.boozallen.plugins.jte.Utils
import com.cloudbees.hudson.plugins.folder.AbstractFolder
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor

import hudson.model.ItemGroup
import hudson.model.Descriptor.FormException
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import hudson.scm.SCM
import hudson.Extension
import hudson.model.Queue
import hudson.model.Run
import hudson.model.TaskListener
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted 
import net.sf.json.JSONObject
import org.kohsuke.stapler.StaplerRequest
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor
import hudson.scm.NullSCM
import org.jenkinsci.plugins.workflow.cps.CpsThread


public class GovernanceTier extends AbstractDescribableImpl<GovernanceTier> implements Serializable{
    
    static final String CONFIG_FILE = "pipeline_config.groovy"
    static final String PIPELINE_TEMPLATE_DIRECTORY = "pipeline_templates"

    String baseDir
    SCM scm 
    List<TemplateLibrarySource> librarySources = new ArrayList() 

    @DataBoundConstructor public GovernanceTier(SCM scm, String baseDir, List<TemplateLibrarySource> librarySources){
        this.scm = scm
        this.baseDir = baseDir
        this.librarySources = librarySources
    }

    public String getBaseDir(){ return baseDir }
    public SCM getScm(){ return scm }
    public List<TemplateLibrarySource> getLibrarySources(){ return librarySources }

    @Whitelisted
    public TemplateConfigObject getConfig() throws Exception{
        TemplateConfigObject configObject 
        if (scm && !(scm instanceof NullSCM)){
            String configFile = Utils.getFileContents("${baseDir ? "${baseDir}/" : ""}${GovernanceTier.CONFIG_FILE}", scm, "Template Configuration File")
            if (configFile) configObject = TemplateConfigDsl.parse(configFile)
        }
        return configObject
    }

    @Whitelisted
    public String getJenkinsfile() throws Exception {
        String jenkinsfile 
        if(scm && !(scm instanceof NullSCM)){
            jenkinsfile = Utils.getFileContents("${baseDir ? "${baseDir}/" : ""}Jenkinsfile", scm, "Template")
        }
        return jenkinsfile 
    }

    @Whitelisted
    public String getTemplate(String template) throws Exception {
        String pipelineTemplate 
        if(scm && !(scm instanceof NullSCM)){
            pipelineTemplate = Utils.getFileContents("${baseDir ? "${baseDir}/" : ""}${GovernanceTier.PIPELINE_TEMPLATE_DIRECTORY}/${template}", scm, "Pipeline Template")
        } 
        return pipelineTemplate 
    }
   

    /*
        returns the job's GovernanceTier hierarchy in ascending order
        for governance, call .reverse() on the returned array to go top down
        for scoping of props, you can just iterate on the list
    */
    static List<GovernanceTier> getHierarchy(){
        List<GovernanceTier> h = new ArrayList()
        
        // recurse through job hierarchy and get template configs 
        WorkflowJob job = Utils.getCurrentJob() 
        ItemGroup<?> parent = job.getParent()
        
        while(parent){
            if (parent instanceof AbstractFolder){
                GovernanceTier tier = parent.getProperties().get(TemplateConfigFolderProperty)?.getTier()
                if (tier){
                    h.push(tier)
                }
                parent = parent.getParent()
            } else break
        }

        // global config 
        GovernanceTier tier = TemplateGlobalConfig.get().getTier() 
        if (tier){
            h.push(tier) 
        }
        return h
    }

    
    @Extension public final static class DescriptorImpl extends Descriptor<GovernanceTier> {
        
        @Override public GovernanceTier newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            GovernanceTier tier = (GovernanceTier) super.newInstance(req, formData);
            return tier.librarySources?.isEmpty() ? null : tier
        }

    }

}
