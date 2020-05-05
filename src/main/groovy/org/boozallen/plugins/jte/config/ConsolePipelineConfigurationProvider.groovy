package org.boozallen.plugins.jte.config

import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import hudson.Extension
import hudson.util.FormValidation
import org.kohsuke.stapler.QueryParameter
import hudson.RelativePath
import hudson.Util
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

public class ConsolePipelineConfigurationProvider extends PipelineConfigurationProvider{

    boolean providePipelineConfig 
    String pipelineConfig
    boolean provideDefaultTemplate
    String defaultTemplate
    List<ConsolePipelineTemplate> pipelineCatalog

    @DataBoundConstructor 
    public ConsolePipelineConfigurationProvider(boolean providePipelineConfig, String pipelineConfig,
                                                boolean provideDefaultTemplate, String defaultTemplate, 
                                                List<ConsolePipelineTemplate> pipelineCatalog){
        this.providePipelineConfig = providePipelineConfig
        this.pipelineConfig = providePipelineConfig ? Util.fixEmptyAndTrim(pipelineConfig) : null 
        this.provideDefaultTemplate = provideDefaultTemplate
        this.defaultTemplate = provideDefaultTemplate ? Util.fixEmptyAndTrim(defaultTemplate) : null 
        this.pipelineCatalog = pipelineCatalog
    }

    public boolean getProvidePipelineConfig(){ return providePipelineConfig }
    public String getPipelineConfig(){ return pipelineConfig }
    public boolean getProvideDefaultTemplate(){ return provideDefaultTemplate }
    public String getDefaultTemplate(){ return defaultTemplate }
    public List<ConsolePipelineTemplate> getPipelineCatalog(){ return pipelineCatalog }

    public TemplateConfigObject getConfig(FlowExecutionOwner owner){
        return pipelineConfig ? new TemplateConfigDsl(run: owner.run()).parse(pipelineConfig) : null 
    }

    public String getJenkinsfile(FlowExecutionOwner owner){
        return defaultTemplate
    }

    public String getTemplate(FlowExecutionOwner owner, String templateName){
        ConsolePipelineTemplate template = pipelineCatalog.find{ it.getName() == templateName }
        return template ? template.getTemplate() : null 
    }

    @Extension public static class DescriptorImpl extends PipelineConfigurationProvider.PipelineConfigurationProviderDescriptor{
        public String getDisplayName(){
            return "From Console"
        }
    }

}