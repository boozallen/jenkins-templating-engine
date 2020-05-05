package org.boozallen.plugins.jte.config

import org.boozallen.plugins.jte.utils.FileSystemWrapper
import org.boozallen.plugins.jte.console.TemplateLogger
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import hudson.scm.SCM
import hudson.scm.NullSCM
import hudson.Util
import hudson.Extension
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

public class ScmPipelineConfigurationProvider extends PipelineConfigurationProvider{

    static final String CONFIG_FILE = "pipeline_config.groovy"
    static final String PIPELINE_TEMPLATE_DIRECTORY = "pipeline_templates"
    String baseDir
    SCM scm 

    @DataBoundConstructor public ScmPipelineConfigurationProvider(){}

    @DataBoundSetter 
    public setBaseDir(String baseDir){ 
        this.baseDir = Util.fixEmptyAndTrim(baseDir)
    }
    public String getBaseDir(){ return baseDir }

    @DataBoundSetter
    public setScm(SCM scm){ this.scm = scm }
    public SCM getScm(){ return scm }

    public TemplateConfigObject getConfig(FlowExecutionOwner owner){
        TemplateConfigObject configObject 
        if (scm && !(scm instanceof NullSCM)){
            FileSystemWrapper fsw = FileSystemWrapper.createFromSCM(owner, scm)
            String filePath = "${baseDir ? "${baseDir}/" : ""}${CONFIG_FILE}"
            String configFile = fsw.getFileContents(filePath, "Pipeline Configuration File")
            if (configFile){
                try{
                    configObject = new TemplateConfigDsl(run: owner.run()).parse(configFile)
                }catch(any){
                    new TemplateLogger(owner.getListener()).printError("Error parsing scm provided pipeline configuration")
                    throw any
                }
            }     
        }
        return configObject
    }

    public String getJenkinsfile(FlowExecutionOwner owner){
        String jenkinsfile 
        if(scm && !(scm instanceof NullSCM)){
            FileSystemWrapper fsw = FileSystemWrapper.createFromSCM(scm)
            String filePath = "${baseDir ? "${baseDir}/" : ""}Jenkinsfile"
            jenkinsfile = fsw.getFileContents(filePath, "Template")
        }
        return jenkinsfile 
    }

    public String getTemplate(FlowExecutionOwner owner, String template){
        String pipelineTemplate 
        if(scm && !(scm instanceof NullSCM)){
            FileSystemWrapper fsw = FileSystemWrapper.createFromSCM(scm)
            String filePath = "${baseDir ? "${baseDir}/" : ""}${PIPELINE_TEMPLATE_DIRECTORY}/${template}"
            pipelineTemplate = fsw.getFileContents(filePath, "Pipeline Template")
        } 
        return pipelineTemplate 
    }


    @Extension public static class DescriptorImpl extends PipelineConfigurationProvider.PipelineConfigurationProviderDescriptor{
        public String getDisplayName(){
            return "From SCM"
        }
    }
}