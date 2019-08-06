package org.boozallen.plugins.jte.config.libraries

import org.boozallen.plugins.jte.console.TemplateLogger
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.io.BufferedReader
import java.nio.charset.StandardCharsets
import hudson.Extension 
import org.kohsuke.stapler.DataBoundConstructor
import hudson.model.DescriptorVisibilityFilter
import org.jenkinsci.plugins.workflow.cps.CpsScript
import hudson.model.Descriptor
import jenkins.model.Jenkins

class PluginLibraryProvider extends LibraryProvider{

    public LibraryProvidingPlugin plugin 
    public HashMap libraries = [:]

    @DataBoundConstructor PluginLibraryProvider(LibraryProvidingPlugin plugin){
        this.plugin = plugin 

        // initialize libraries 
        def src = plugin.getClass().getProtectionDomain().getCodeSource()
        URL jar = src.getLocation()
        ZipFile zipFile = new ZipFile(new File(jar.toURI()))  
        ZipInputStream zipStream = new ZipInputStream(jar.openStream())
        ZipEntry zipEntry
        while( (zipEntry = zipStream.getNextEntry()) != null   ){
            String path = zipEntry.getName().toString()
            ArrayList parts = path.split("/")
            if(path.startsWith("libraries/") && path.endsWith(".groovy") && parts.size() >= 3){
                String libName = parts.getAt(1)
                String stepName = parts.last() - ".groovy" 
                if(!libraries[libName]){
                    libraries[libName] = [:]
                }
                libraries[libName][stepName] = getFileContents(zipFile, zipEntry) 
            } 
        }

    }

    String getFileContents(ZipFile z, ZipEntry e){
        InputStream stream = z.getInputStream(e)            
        StringBuilder stringBuilder = new StringBuilder()
        ArrayList lines = [] 
        try{
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
            String line 
            while ((line = bufferedReader.readLine()) != null) {
                lines << line
            }
        }catch(any){}
        
        return lines.join("\n") 
    }

    public Boolean hasLibrary(String libName){
        return libName in libraries.keySet()
    }

    public List loadLibrary(CpsScript script, String libName, Map libConfig){
        TemplateLogger.print "Loading jar library ${libName}"
        libraries[libName].each{ stepName, stepContent -> 
            TemplateLogger.print "loading step -> ${stepName}"
            TemplateLogger.print stepContent
        }
        
        return new ArrayList()
    }
    
    @Extension public static class DescriptorImpl extends LibraryProviderDescriptor{
        public String getDisplayName(){
            return "From a Library Providing Plugin"
        }

        public static List<LibraryProvidingPlugin> getLibraryProvidingPlugins(){
            return Jenkins.getActiveInstance().getExtensionList(LibraryProvidingPluginDescriptor)
        }
    }

    @Extension public static class FilterImpl extends DescriptorVisibilityFilter {
        @Override
        public boolean filter(Object context, Descriptor descriptor) {
            if (descriptor instanceof DescriptorImpl){
                return !DescriptorImpl.getLibraryProvidingPlugins().isEmpty()
            }
            return true 
        }
    }

}