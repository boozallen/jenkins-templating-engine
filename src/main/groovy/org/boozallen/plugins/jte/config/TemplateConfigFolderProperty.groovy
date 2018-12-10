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

import java.io.IOException

import org.boozallen.plugins.jte.Utils
import com.cloudbees.hudson.plugins.folder.AbstractFolder
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor

import hudson.model.Descriptor.FormException
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import hudson.scm.SCM
import hudson.Extension
import hudson.model.Queue
import hudson.model.Run
import hudson.model.TaskListener
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted 
import net.sf.json.JSONObject
import org.kohsuke.stapler.StaplerRequest
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import hudson.model.Descriptor

public class TemplateConfigFolderProperty extends AbstractFolderProperty<AbstractFolder<?>> {
    private GovernanceTier tier 

    @DataBoundConstructor public TemplateConfigFolderProperty(GovernanceTier tier){
        this.tier = tier 
    }

    public GovernanceTier getTier(){
        return tier 
    }

    @Extension 
    public final static class DescriptorImpl extends AbstractFolderPropertyDescriptor {

        @Override
		public String getDisplayName() {
			return "Solutions Delivery Platform"
		}

    }

}