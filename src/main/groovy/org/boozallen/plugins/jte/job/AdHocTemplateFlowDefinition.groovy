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
import hudson.Util
import jenkins.model.Jenkins
import org.boozallen.plugins.jte.init.PipelineDecorator
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.cps.FlowHead
import org.jenkinsci.plugins.workflow.cps.persistence.PersistIn
import org.jenkinsci.plugins.workflow.flow.DurabilityHintProvider
import org.jenkinsci.plugins.workflow.flow.FlowDefinition
import org.jenkinsci.plugins.workflow.flow.FlowDefinitionDescriptor
import org.jenkinsci.plugins.workflow.flow.FlowDurabilityHint
import org.jenkinsci.plugins.workflow.flow.FlowExecution
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.jenkinsci.plugins.workflow.flow.GlobalDefaultFlowDurabilityLevel
import org.jenkinsci.plugins.workflow.graph.FlowStartNode
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.kohsuke.stapler.DataBoundConstructor
import static org.jenkinsci.plugins.workflow.cps.persistence.PersistenceContext.*

class AdHocTemplateFlowDefinition extends TemplateFlowDefinition {

    private final boolean providePipelineTemplate
    public String template
    private final boolean providePipelineConfig
    public String pipelineConfig

    @DataBoundConstructor public AdHocTemplateFlowDefinition(boolean providePipelineTemplate, String template, boolean providePipelineConfig, String pipelineConfig){
        this.providePipelineTemplate = providePipelineTemplate
        this.template = providePipelineTemplate ? Util.fixEmptyAndTrim(template) : null
        this.providePipelineConfig = providePipelineConfig
        this.pipelineConfig = providePipelineConfig ? Util.fixEmptyAndTrim(pipelineConfig) : null
    }

    public boolean getProvidePipelineTemplate(){ return providePipelineTemplate }
    public String getTemplate() { return template }
    public boolean getProvidePipelineConfig(){ return providePipelineConfig }
    public String getPipelineConfig(){ return pipelineConfig }

    @Extension
    public static class DescriptorImpl extends FlowDefinitionDescriptor {
        @Override
        public String getDisplayName() {
            return "Jenkins Templating Engine"
        }
    }

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