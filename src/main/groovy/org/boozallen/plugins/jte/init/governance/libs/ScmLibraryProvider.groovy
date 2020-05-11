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

import hudson.Extension
import org.kohsuke.stapler.DataBoundConstructor
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
import hudson.util.ListBoxModel
import org.jenkinsci.plugins.workflow.cps.CpsScript
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

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


    public Boolean hasLibrary(FlowExecutionOwner flowOwner, String libName){
        SCMFileSystem fs = createFs(flowOwner)
        if (!fs) return false
        SCMFile lib = fs.child(prefixBaseDir(libName))
        return lib.isDirectory()
    }

    public List loadLibrary(FlowExecutionOwner flowOwner, Binding binding, String libName, Map libConfig){
        SCMFileSystem fs = createFs(flowOwner)
        if (!fs){ return }

        TemplateLogger logger = new TemplateLogger(flowOwner.getListener())

        ArrayList msg = [
            "Loading Library ${libName}",
            "-- scm: ${scm.getKey()}"
        ]
        logger.print(msg.join("\n"))

        SCMFile lib = fs.child(prefixBaseDir(libName))

        // do validation if the library configuration file is present
        SCMFile libConfigFile = lib.child(CONFIG_FILE)
        ArrayList libConfigErrors = []
        if(libConfigFile.exists() && libConfigFile.isFile()){
            libConfigErrors = doLibraryConfigValidation(flowOwner, libConfigFile.contentAsString(), libConfig)
            if(libConfigErrors){
                return [ "${libName}:" ] + libConfigErrors.collect{ " - ${it}" }
            }
        }else{
            logger.printWarning("Library ${libName} does not have a configuration file.")
        }

        def StepWrapper = LibraryLoader.getPrimitiveClass()
        lib.children().findAll{
            it.getName().endsWith(".groovy") &&
            !it.getName().endsWith("library_config.groovy") // exclude lib config file
        }.each{ stepFile ->
            def s = StepWrapper.createFromFile(stepFile, libName, binding, libConfig)
            binding.setVariable(s.getName(), s)
        }

        return libConfigErrors
    }

    public String prefixBaseDir(String s){
        return [baseDir, s?.trim()].findAll{ it }.join("/")
    }

    public SCMFileSystem createFs(FlowExecutionOwner flowOwner){
        return FileSystemWrapper.createFromSCM(flowOwner, scm) as SCMFileSystem
    }

    @Extension public static class DescriptorImpl extends LibraryProvider.LibraryProviderDescriptor{
        public String getDisplayName(){
            return "From SCM"
        }
    }
}
