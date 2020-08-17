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
import org.boozallen.plugins.jte.console.TemplateLogger
import org.boozallen.plugins.jte.job.TemplateFlowDefinition
import com.cloudbees.hudson.plugins.folder.AbstractFolder
import hudson.model.ItemGroup
import hudson.model.Descriptor.FormException
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import hudson.scm.SCM
import hudson.Extension
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted 
import net.sf.json.JSONObject
import org.kohsuke.stapler.StaplerRequest
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor
import hudson.Util
import jenkins.model.Jenkins

public class GovernanceTier extends AbstractDescribableImpl<GovernanceTier> implements Serializable{
        
    PipelineConfigurationProvider configurationProvider = new NullPipelineConfigurationProvider()
    List<LibraryConfiguration> libraries

    @DataBoundConstructor public GovernanceTier(){}

    protected Object readResolve(){
        if(configurationProvider == null){
            configurationProvider = new NullPipelineConfigurationProvider()
        }
        return this 
    }

    @DataBoundSetter
    public void setConfigurationProvider(PipelineConfigurationProvider configurationProvider){
        if(configurationProvider == null){
            configurationProvider = new NullPipelineConfigurationProvider()
        }
        this.configurationProvider = configurationProvider
    }

    public PipelineConfigurationProvider getConfigurationProvider(){ 
        return configurationProvider
    }

    @DataBoundSetter 
    public void setLibraries(List<LibraryConfiguration> libraries){
        this.libraries = libraries 
    }
    public List<LibraryConfiguration> getLibraries(){ 
        return libraries 
    }
   
    @Whitelisted
    public TemplateConfigObject getConfig() throws Exception{
        return configurationProvider.getConfig()
    }

    @Whitelisted
    public String getJenkinsfile() throws Exception {
        return configurationProvider.getJenkinsfile()
    }

    @Whitelisted
    public String getTemplate(String template) throws Exception {
        return configurationProvider.getTemplate(template)
    }


    /*
        returns the job's GovernanceTier hierarchy in ascending order
        for governance, call .reverse() on the returned array to go top down
        for scoping of props, you can just iterate on the list
    */
    static List<GovernanceTier> getHierarchy(){
        List<GovernanceTier> h = new ArrayList()
        
        // folder pipeline configs 
        WorkflowJob job = RunUtils.getJob()
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
            return tier.libraries?.isEmpty() ? null : tier
        }

        public static List<LibraryProvider.LibraryProviderDescriptor> getLibraryProviders(){
            return Jenkins.getActiveInstance().getExtensionList(LibraryProvider.LibraryProviderDescriptor)
        }

        public static List<PipelineConfigurationProvider.PipelineConfigurationProviderDescriptor> getPipelineConfigurationProviders(){
            return Jenkins.getActiveInstance().getExtensionList(PipelineConfigurationProvider.PipelineConfigurationProviderDescriptor)
        }

        public Descriptor getDefaultConfigurationProvider(){
            return Jenkins.get().getDescriptor(NullPipelineConfigurationProvider)
        }

    }

}
