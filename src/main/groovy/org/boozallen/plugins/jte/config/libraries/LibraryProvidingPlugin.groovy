package org.boozallen.plugins.jte.config.libraries

import org.boozallen.plugins.jte.utils.FileSystemWrapper
import org.boozallen.plugins.jte.console.TemplateLogger
import org.boozallen.plugins.jte.binding.injectors.LibraryLoader
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

