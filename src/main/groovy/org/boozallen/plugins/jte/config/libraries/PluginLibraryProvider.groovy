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
import org.boozallen.plugins.jte.binding.injectors.LibraryLoader


class PluginLibraryProvider extends LibraryProvider{

    public LibraryProvidingPlugin plugin 
    public HashMap libraries = [:]

    @DataBoundConstructor PluginLibraryProvider(LibraryProvidingPlugin plugin){
        this.plugin = plugin 
        initialize()
    }

    /*
        analyzes the contributing plugin's jar file to find libraries 
        housed in the resources directory. 

        populates this.libraries with data from parsing
    */
    void initialize(){
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
                // create new library entry
                if(!libraries[libName]){
                    libraries[libName] = [
                        steps: [:],
                        config: null 
                    ]
                }
                // store config or step 
                String fileName = parts.last()
                String fileContents = getFileContents(zipFile, zipEntry) 
                if(parts.size() == 3 && fileName.equals(CONFIG_FILE)){
                    libraries[libName].config = fileContents 
                }else{
                    libraries[libName].steps["${fileName - ".groovy"}"] = fileContents 
                }
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
        TemplateLogger.print("""Loading Library ${libName}
                                -- plugin: ${getPluginDisplayName() ?: "can't determine plugin"}""", [initiallyHidden:true])

        // do library configuration 
        ArrayList libConfigErrors = []
        if(libraries[libName].config){
            libConfigErrors = doLibraryConfigValidation(libraries[libName].config, libConfig)
            if(libConfigErrors){
                return [ "${libName}:" ] + libConfigErrors.collect{ " - ${it}" }
            }
        }else{
            TemplateLogger.printWarning("Library ${libName} does not have a configuration file.")
        }

        // load steps 
        def StepWrapper = LibraryLoader.getPrimitiveClass()
        libraries[libName].steps.each{ stepName, stepContents -> 
            def s = StepWrapper.createFromString(stepContents, script, stepName, libName, libConfig)
            script.getBinding().setVariable(stepName, s)
        }
        return libConfigErrors
    }

    /*
        returns null if for some reason plugin descriptor can't be found
        -- this shouldn't technically be possible. 
    */
    public String getPluginDisplayName(){
        List<LibraryProvidingPlugin> plugins = DescriptorImpl.getLibraryProvidingPlugins()
        Descriptor pluginDescriptor = Descriptor.findByDescribableClassName(plugins, plugin.getClass().getName())
        return pluginDescriptor?.getDisplayName()
    }
    
    @Extension public static class DescriptorImpl extends LibraryProviderDescriptor{
        public String getDisplayName(){
            return "From a Library Providing Plugin"
        }

        public static List<LibraryProvidingPlugin> getLibraryProvidingPlugins(){
            return Jenkins.getActiveInstance().getExtensionList(LibraryProvidingPluginDescriptor)
        }
    }

    /*
        hide this plugin as an option if there aren't any plugin providing libraries 
        installed on the jenkins instance
    */
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