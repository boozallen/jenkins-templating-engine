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
package org.boozallen.plugins.jte.init.governance

import hudson.Extension
import com.cloudbees.hudson.plugins.folder.AbstractFolder
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor
import org.kohsuke.stapler.DataBoundConstructor

/*
    Adds a folder property to store the Governance Tiers via the
    job hierarchy
*/
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
            return "Jenkins Templating Engine"
        }

    }

}