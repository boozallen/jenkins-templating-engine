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

import org.boozallen.plugins.jte.config.libraries.LibraryProvider
import org.boozallen.plugins.jte.config.libraries.LibraryConfiguration
import org.boozallen.plugins.jte.utils.RunUtils
import org.boozallen.plugins.jte.utils.FileSystemWrapper
import org.boozallen.plugins.jte.console.TemplateLogger
import org.boozallen.plugins.jte.job.TemplateFlowDefinition
import com.cloudbees.hudson.plugins.folder.AbstractFolder
import hudson.model.ItemGroup
import hudson.model.Descriptor.FormException
import org.kohsuke.stapler.DataBoundConstructor
import hudson.scm.SCM
import hudson.Extension
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted 
import net.sf.json.JSONObject
import org.kohsuke.stapler.StaplerRequest
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor
import hudson.scm.NullSCM
import hudson.Util
import jenkins.model.Jenkins

public class GovernanceTier extends AbstractDescribableImpl<GovernanceTier> implements Serializable{
    
    static final String CONFIG_FILE = "pipeline_config.groovy"
    static final String PIPELINE_TEMPLATE_DIRECTORY = "pipeline_templates"

    String baseDir
    SCM scm 
    List<LibraryConfiguration> libraries
    String pipelineConfig 

    // added for unit testing
    public GovernanceTier(){}

    @DataBoundConstructor public GovernanceTier(SCM scm, String baseDir, List<LibraryConfiguration> libraries){
        this.scm = scm
        this.baseDir = Util.fixEmptyAndTrim(baseDir)
        this.libraries = libraries 
    }

    public String getBaseDir(){ return baseDir }
    public SCM getScm(){ return scm }
    public List<LibraryConfiguration> getLibraries(){ return libraries }
    
    public String getPipelineConfig(){
        return pipelineConfig
    }

    public String setPipelineConfig(String pipelineConfig){
        this.pipelineConfig = pipelineConfig
    }

    @Whitelisted
    public TemplateConfigObject getConfig() throws Exception{
        TemplateConfigObject configObject 
        if(pipelineConfig){
            try{
                configObject = TemplateConfigDsl.parse(pipelineConfig)
            }catch(any){
                TemplateLogger.printError("Error parsing user provided pipeline configuration")
                throw any
            }
        }else if (scm && !(scm instanceof NullSCM)){
            FileSystemWrapper fsw = FileSystemWrapper.createFromSCM(scm)
            String filePath = "${baseDir ? "${baseDir}/" : ""}${CONFIG_FILE}"
            String configFile = fsw.getFileContents(filePath, "Template Configuration File")
            if (configFile){
                try{
                configObject = TemplateConfigDsl.parse(configFile)
                }catch(any){
                    TemplateLogger.printError("Error parsing scm provided pipeline configuration")
                    throw any
                }
            }
        }
        return configObject
    }

    @Whitelisted
    public String getJenkinsfile() throws Exception {
        String jenkinsfile 
        if(scm && !(scm instanceof NullSCM)){
            FileSystemWrapper fsw = FileSystemWrapper.createFromSCM(scm)
            String filePath = "${baseDir ? "${baseDir}/" : ""}Jenkinsfile"
            jenkinsfile = fsw.getFileContents(filePath, "Template")
        }
        return jenkinsfile 
    }

    @Whitelisted
    public String getTemplate(String template) throws Exception {
        String pipelineTemplate 
        if(scm && !(scm instanceof NullSCM)){
            FileSystemWrapper fsw = FileSystemWrapper.createFromSCM(scm)
            String filePath = "${baseDir ? "${baseDir}/" : ""}${PIPELINE_TEMPLATE_DIRECTORY}/${template}"
            pipelineTemplate = fsw.getFileContents(filePath, "Pipeline Template")
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
        WorkflowJob job = RunUtils.getJob()

        def flowDefinition = job.getDefinition() 
        if(flowDefinition instanceof TemplateFlowDefinition){
            String pipelineConfig = flowDefinition.getPipelineConfig()
            GovernanceTier jobTier = new GovernanceTier()
            jobTier.setPipelineConfig(pipelineConfig)
            h.push(jobTier)
        }
        // folder pipeline configs 
        ItemGroup<?> parent = job.getParent()
        while(parent instanceof AbstractFolder){
            GovernanceTier tier = parent.getProperties().get(TemplateConfigFolderProperty)?.getTier()
            if (tier){
                h.push(tier)
            }
            parent = parent.getParent()
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

        public static List<LibraryProvider.LibraryProviderDescriptor> getLibraryProviders(){
            return Jenkins.getActiveInstance().getExtensionList(LibraryProvider.LibraryProviderDescriptor)
        }

    }

}
