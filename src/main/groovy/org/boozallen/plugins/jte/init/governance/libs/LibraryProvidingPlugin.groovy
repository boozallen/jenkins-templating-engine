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
package org.boozallen.plugins.jte.init.governance.libs

import org.boozallen.plugins.jte.util.FileSystemWrapper
import org.boozallen.plugins.jte.util.TemplateLogger
import org.boozallen.plugins.jte.init.primitives.injectors.LibraryLoader
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import hudson.scm.SCM
import jenkins.scm.api.SCMFileSystem
import jenkins.scm.api.SCMFile
import hudson.Extension
import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor
import hudson.Util
import org.jenkinsci.plugins.workflow.cps.CpsScript
import hudson.ExtensionPoint
import hudson.ExtensionList
import jenkins.model.Jenkins
import hudson.PluginWrapper

abstract class LibraryProvidingPlugin extends AbstractDescribableImpl<LibraryProvidingPlugin>{
    public static class LibraryProvidingPluginDescriptor extends Descriptor<LibraryProvidingPlugin> {
        String getDisplayName(){
            PluginWrapper plugin = Jenkins.get().getPluginManager().whichPlugin(this.getClass())
            return plugin.getDisplayName()
        }
    }
}
