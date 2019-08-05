package org.boozallen.plugins.jte.config

import hudson.Extension 
import org.kohsuke.stapler.DataBoundConstructor

public class ScmLibraryProvider extends LibraryProvider{

    @DataBoundConstructor public ScmLibraryProvider(){}

    @Extension public static class DescriptorImpl extends LibraryProviderDescriptor{
        public String getDisplayName(){
            return "From SCM"
        }
    }

}