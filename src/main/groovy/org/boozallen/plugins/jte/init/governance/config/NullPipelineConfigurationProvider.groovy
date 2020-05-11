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
package org.boozallen.plugins.jte.init.governance.config

import hudson.Extension
import hudson.scm.NullSCM
import hudson.scm.SCM
import hudson.Util
import org.boozallen.plugins.jte.init.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.util.FileSystemWrapper
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter

public class NullPipelineConfigurationProvider extends PipelineConfigurationProvider{

    @DataBoundConstructor public NullPipelineConfigurationProvider(){}

    public PipelineConfigurationObject getConfig(FlowExecutionOwner owner){ return null }
    public String getJenkinsfile(FlowExecutionOwner owner){ return null }
    public String getTemplate(FlowExecutionOwner owner, String template){ return null }

    @Extension public static class DescriptorImpl extends PipelineConfigurationProvider.PipelineConfigurationProviderDescriptor{
        public String getDisplayName(){
            return "None"
        }
    }
}
