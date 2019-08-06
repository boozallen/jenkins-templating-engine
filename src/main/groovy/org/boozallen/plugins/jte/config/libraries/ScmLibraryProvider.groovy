package org.boozallen.plugins.jte.config.libraries

import hudson.Extension 
import org.kohsuke.stapler.DataBoundConstructor
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
import hudson.util.ListBoxModel
import org.jenkinsci.plugins.workflow.cps.CpsScript
import jenkins.model.Jenkins

public class ScmLibraryProvider extends LibraryProvider{

    public SCM scm
    public String baseDir

    @DataBoundConstructor public ScmLibraryProvider(){}

    @DataBoundSetter public void setBaseDir(String baseDir) {
        this.baseDir = Util.fixEmptyAndTrim(baseDir)
    }

    public String getBaseDir() { return baseDir }

    @DataBoundSetter public void setScm(SCM scm){ this.scm = scm }
    public SCM getScm(){ return scm }

    // @DataBoundSetter public void setLibProvider(LibraryProvider libProvider) {
    //     this.libProvider = libProvider
    // }
    // public LibraryProvider getLibProvider(){ return this.libProvider }


    public Boolean hasLibrary(String libName){
        SCMFileSystem fs = createFs()
        if (!fs) return false 
        SCMFile lib = fs.child(prefixBaseDir(libName))
        return lib.isDirectory()
    }

    public List loadLibrary(CpsScript script, String libName, Map libConfig){
        SCMFileSystem fs = createFs()
        if (!fs){ return }

        TemplateLogger.print("""Loading Library ${libName}
                                -- scm: ${scm.getKey()}""", [initiallyHidden:true])

        SCMFile lib = fs.child(prefixBaseDir(libName))

        // do validation if the library configuration file is present
        SCMFile libConfigFile = lib.child(CONFIG_FILE)
        ArrayList libConfigErrors = []
        if(libConfigFile.exists() && libConfigFile.isFile()){
            libConfigErrors = doLibraryConfigValidation(libConfigFile.contentAsString(), libConfig)
            if(libConfigErrors){
                return [ "${libName}:" ] + libConfigErrors.collect{ " - ${it}" }
            }
        }else{
            TemplateLogger.printWarning("Library ${libName} does not have a configuration file.")
        }

        lib.children().findAll{ 
            it.getName().endsWith(".groovy") && 
            !it.getName().endsWith("library_config.groovy") // exclude lib config file 
        }.each{ stepFile ->
            def StepWrapper = LibraryLoader.getPrimitiveClass()
            def s = StepWrapper.createFromFile(stepFile, libName, script, libConfig)
            script.getBinding().setVariable(s.getName(), s)
        }

        return libConfigErrors
    }

    public String prefixBaseDir(String s){
        return [baseDir, s?.trim()].findAll{ it }.join("/")
    }

    public SCMFileSystem createFs(){
        return FileSystemWrapper.createFromSCM(scm) as SCMFileSystem
    }

    @Extension public static class DescriptorImpl extends LibraryProviderDescriptor{
        public String getDisplayName(){
            return "From SCM"
        }
    }
}