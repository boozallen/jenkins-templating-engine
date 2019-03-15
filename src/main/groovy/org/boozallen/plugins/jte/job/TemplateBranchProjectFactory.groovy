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
import jenkins.branch.MultiBranchProject
import org.jenkinsci.plugins.workflow.flow.FlowDefinition
import org.kohsuke.stapler.DataBoundConstructor
import javax.annotation.Nonnull
import hudson.model.TaskListener
import jenkins.scm.api.SCMSource
import jenkins.scm.api.SCMSourceCriteria
import org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory
import org.jenkinsci.plugins.workflow.multibranch.AbstractWorkflowBranchProjectFactory

public class TemplateBranchProjectFactory extends WorkflowBranchProjectFactory {

    @DataBoundConstructor public TemplateBranchProjectFactory() {}

    @Override protected FlowDefinition createDefinition() {
        return new TemplateFlowDefinition()
    }

    @Override
    protected SCMSourceCriteria getSCMSourceCriteria(SCMSource source) {
        return new SCMSourceCriteria() {
            @Override
            public boolean isHead(Probe probe, TaskListener listener) throws IOException {
                return true
            }
        }
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
