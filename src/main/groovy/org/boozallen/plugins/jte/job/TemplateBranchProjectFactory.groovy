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

import org.boozallen.plugins.jte.config.GovernanceTier
import hudson.Extension
import jenkins.branch.MultiBranchProject
import org.jenkinsci.plugins.workflow.flow.FlowDefinition
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import javax.annotation.Nonnull
import hudson.model.TaskListener
import jenkins.scm.api.SCMSource
import jenkins.scm.api.SCMSourceCriteria
import jenkins.scm.api.SCMProbeStat
import jenkins.scm.api.SCMFile
import org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory
import org.jenkinsci.plugins.workflow.multibranch.AbstractWorkflowBranchProjectFactory

public class TemplateBranchProjectFactory extends WorkflowBranchProjectFactory {

    Boolean filterBranches

    @DataBoundConstructor public TemplateBranchProjectFactory() {}

    public Object readResolve() {
        if (this.filterBranches == null) {
            this.filterBranches = false;
        }
        return this;
    }

    @DataBoundSetter
    public void setFilterBranches(Boolean filterBranches){
        this.filterBranches = filterBranches
    }

    public Boolean getFilterBranches(){
        return filterBranches
    }

    @Override 
    protected FlowDefinition createDefinition() {
        return new MultibranchTemplateFlowDefinition()
    }

    @Override
    protected SCMSourceCriteria getSCMSourceCriteria(SCMSource source) {
        return new SCMSourceCriteria() {
            @Override public boolean isHead(SCMSourceCriteria.Probe probe, TaskListener listener) throws IOException {
                // default behavior is to create jobs for each branch
                if(!filterBranches){
                    return true 
                }

                // if user chose to filter branches, check for pipeline config file 
                SCMProbeStat stat = probe.stat(ScmPipelineConfigurationProvider.CONFIG_FILE);
                switch (stat.getType()) {
                    case SCMFile.Type.NONEXISTENT:
                        if (stat.getAlternativePath() != null) {
                            listener.getLogger().format("      ‘%s’ not found (but found ‘%s’, search is case sensitive)%n", ScmPipelineConfigurationProvider.CONFIG_FILE, stat.getAlternativePath());
                        } else {
                            listener.getLogger().format("      ‘%s’ not found%n", ScmPipelineConfigurationProvider.CONFIG_FILE);
                        }
                        return false;
                    case SCMFile.Type.DIRECTORY:
                        listener.getLogger().format("      ‘%s’ found but is a directory not a file%n", ScmPipelineConfigurationProvider.CONFIG_FILE);
                        return false;
                    default:
                        listener.getLogger().format("      ‘%s’ found%n", ScmPipelineConfigurationProvider.CONFIG_FILE);
                        return true;
                }
            }

            @Override
            public int hashCode() {
                return getClass().hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                return getClass().isInstance(obj);
            }
        };
    }

    @Extension
    public static class DescriptorDefaultImpl extends AbstractWorkflowBranchProjectFactory.AbstractWorkflowBranchProjectFactoryDescriptor {

        @Override
        public boolean isApplicable(Class<? extends MultiBranchProject> clazz) {
            return MultiBranchProject.class.isAssignableFrom(clazz)
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Jenkins Templating Engine" 
        }

    }

}
