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

package org.boozallen.plugins.jte.job

import hudson.model.InvisibleAction
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import hudson.model.TaskListener
import org.boozallen.plugins.jte.binding.TemplateBinding
import org.boozallen.plugins.jte.config.PipelineConfig
import org.boozallen.plugins.jte.config.GovernanceTier
import org.boozallen.plugins.jte.config.TemplateConfigObject
import org.boozallen.plugins.jte.config.TemplateConfigDsl
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.boozallen.plugins.jte.utils.FileSystemWrapper
import org.boozallen.plugins.jte.console.TemplateLogger
import groovy.lang.Lazy


/*
    The PipelineDecorator is responsible for augmenting a pipeline's runtime environment
    to "hydrate" a pipeline template by performing the Jenkins Templating Engine's
    initialization process, which consists of: 
        1. aggregate pipeline configurations
        2. initialize the TemplateBinding with TemplatePrimitives 
        3. determining the pipeline template for this run 
*/
class PipelineDecorator extends InvisibleAction { 

    FlowExecutionOwner flowOwner
    TemplateBinding binding 
    String template

    PipelineDecorator(FlowExecutionOwner flowOwner){
        this.flowOwner = flowOwner
    }

    void initialize(){
        PipelineConfig pipelineConfig = aggregatePipelineConfigurations()
        binding = new TemplateBinding(flowOwner)
        injectPrimitives()
    }

    PipelineConfig aggregatePipelineConfigurations(){
        PipelineConfig pipelineConfig = new PipelineConfig(flowOwner) 
        List<GovernanceTier> tiers = GovernanceTier.getHierarchy(getJob())
       
        //  we get the configs in ascending order of governance
        //  so reverse the list to get the highest precedence first
        tiers.reverse().each{ tier ->
            TemplateConfigObject config = tier.getConfig(flowOwner)
            if (config){
                pipelineConfig.join(config) 
            }
        }

        // get job level configuration 
        TemplateConfigObject jobConfig = getJobPipelineConfiguration(getJob())
        if(jobConfig){
            pipelineConfig.join(jobConfig)
        }

        return pipelineConfig
    }

    TemplateConfigObject getJobPipelineConfiguration(WorkflowJob job){
        TemplateConfigObject jobConfig = null 
        def flowDefinition = job.getDefinition() 
        if(flowDefinition instanceof TemplateFlowDefinition){
            String jobConfigString = flowDefinition.getPipelineConfig()
            if(jobConfigString){
                try{
                    jobConfig = new TemplateConfigDsl(run: flowOwner.run()).parse(jobConfigString)
                }catch(any){
                    getLogger().printError("Error parsing ${job.getName()}'s configuration file.")
                    throw any 
                }
            }
        } else { 
            // get job config if present 
            FileSystemWrapper fsw = FileSystemWrapper.createFromJob()
            String repoConfigFile = fsw.getFileContents(ScmPipelineConfigurationProvider.CONFIG_FILE, "Template Configuration File", false)
            if (repoConfigFile){
                try{
                    jobConfig = new TemplateConfigDsl(run: flowOwner.run()).parse(repoConfigFile)
                }catch(any){
                    getLogger().printError("Error parsing ${job.getName()}'s configuration file in SCM.")
                    throw any 
                }                
            }
        }
        return jobConfig 
    }

    void injectPrimitives(){
        binding.setVariable("somePipelineVar", 11)
    }

    String determinePipelineTemplate(){}
    String getTemplate(){
        return """
        println "template"
        println "somePipelineVar -> \${somePipelineVar}"
        """
    }

    TemplateLogger getLogger(){
        return new TemplateLogger(flowOwner.getListener())
    }

    WorkflowJob getJob(){
        return flowOwner.run().getParent()
    }

}