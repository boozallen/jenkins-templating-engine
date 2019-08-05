package org.boozallen.plugins.jte.config

import org.boozallen.plugins.jte.console.TemplateLogger
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.io.BufferedReader
import java.nio.charset.StandardCharsets
import hudson.Extension 
import org.kohsuke.stapler.DataBoundConstructor

class PluginLibraryProvider extends LibraryProvider{

    @DataBoundConstructor public PluginLibraryProvider(){}

    @Extension public static class DescriptorImpl extends LibraryProviderDescriptor{
        public String getDisplayName(){
            return "From a Library Providing Plugin"
        }
    }

}