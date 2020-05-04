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

import hudson.Extension
import hudson.model.*
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.flow.FlowDefinition
import org.jenkinsci.plugins.workflow.flow.FlowDefinitionDescriptor
import org.jenkinsci.plugins.workflow.flow.FlowExecution
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.jenkinsci.plugins.workflow.flow.FlowDurabilityHint
import org.jenkinsci.plugins.workflow.flow.DurabilityHintProvider
import org.jenkinsci.plugins.workflow.flow.GlobalDefaultFlowDurabilityLevel
import org.jenkinsci.plugins.workflow.cps.persistence.PersistIn
import static org.jenkinsci.plugins.workflow.cps.persistence.PersistenceContext.*
import org.kohsuke.stapler.DataBoundConstructor
import hudson.Util

@PersistIn(JOB)
class TemplateFlowDefinition extends FlowDefinition {

    private final boolean providePipelineTemplate
    public String template
    private final boolean providePipelineConfig
    public String pipelineConfig

    @DataBoundConstructor public TemplateFlowDefinition(boolean providePipelineTemplate, String template, boolean providePipelineConfig, String pipelineConfig){
        this.providePipelineTemplate = providePipelineTemplate
        this.template = providePipelineTemplate ? Util.fixEmptyAndTrim(template) : null 
        this.providePipelineConfig = providePipelineConfig
        this.pipelineConfig = providePipelineConfig ? Util.fixEmptyAndTrim(pipelineConfig) : null 
    }

    public boolean getProvidePipelineTemplate(){ return providePipelineTemplate }
    public String getTemplate() { return template }
    public boolean getProvidePipelineConfig(){ return providePipelineConfig }
    public String getPipelineConfig(){ return pipelineConfig }

    @Override
    public FlowExecution create(FlowExecutionOwner owner, TaskListener listener, List<? extends Action> actions) throws Exception {
        FlowDurabilityHint hint = getFlowDurabilityHint(owner)

        PipelineDecorator decorator = new PipelineDecorator(owner)
        decorator.initialize() // runs the initialization process for JTE 
        String template = decorator.getTemplate() 
        owner.run().with{
            addAction(decorator)
            save() // persist action
        }

        return new CpsFlowExecution(template, true, owner, hint);
    }

    private FlowDurabilityHint getFlowDurabilityHint(FlowExecutionOwner owner){
        Jenkins jenkins = Jenkins.getInstance()
        if (jenkins == null) {
            throw new IllegalStateException("inappropriate context")
        }
        Queue.Executable exec = owner.getExecutable()
        if (!(exec instanceof WorkflowRun)) {
            throw new IllegalStateException("inappropriate context")
        }
        FlowDurabilityHint hint = (exec instanceof Item) ? DurabilityHintProvider.suggestedFor((Item)exec) : GlobalDefaultFlowDurabilityLevel.getDefaultDurabilityHint()
    }


    @Extension
    public static class DescriptorImpl extends FlowDefinitionDescriptor {

        @Override
        public String getDisplayName() {
            return "Jenkins Templating Engine"
        }

    }

    /**
     * Want to display this in the r/o configuration for a branch project, but not offer it on standalone jobs or in any other context.
     */
    @Extension
    public static class HideMeElsewhere extends DescriptorVisibilityFilter {

        @Override
        public boolean filter(Object context, Descriptor descriptor) {
            if (descriptor instanceof DescriptorImpl) {
                return context instanceof WorkflowJob && !(((WorkflowJob) context).getParent() instanceof WorkflowMultiBranchProject)
            }
            return true
        }

    }
}
