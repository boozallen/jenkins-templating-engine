package org.boozallen.plugins.jte.config

import org.boozallen.plugins.jte.utils.FileSystemWrapper
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import hudson.scm.SCM
import hudson.scm.NullSCM
import hudson.Util
import hudson.Extension

public class NullPipelineConfigurationProvider extends PipelineConfigurationProvider{

    @DataBoundConstructor public NullPipelineConfigurationProvider(){}

    public TemplateConfigObject getConfig(){ return null }
    public String getJenkinsfile(){ return null }
    public String getTemplate(String template){ return null }

    @Extension public static class DescriptorImpl extends PipelineConfigurationProvider.PipelineConfigurationProviderDescriptor{
        public String getDisplayName(){
            return "None"
        }
    }
}