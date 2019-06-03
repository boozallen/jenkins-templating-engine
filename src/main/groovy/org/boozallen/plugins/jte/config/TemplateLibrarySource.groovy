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

import org.boozallen.plugins.jte.utils.FileSystemWrapper
import org.boozallen.plugins.jte.console.TemplateLogger
import org.boozallen.plugins.jte.binding.StepWrapper
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

public class TemplateLibrarySource extends AbstractDescribableImpl<TemplateLibrarySource> implements Serializable{

    public SCM scm
    public String baseDir

    @DataBoundConstructor public TemplateLibrarySource(){}

    @DataBoundSetter public void setBaseDir(String baseDir) {
        this.baseDir = Util.fixEmptyAndTrim(baseDir)
    }

    public String getBaseDir() { return baseDir }

    @DataBoundSetter public void setScm(SCM scm){ this.scm = scm }
    public SCM getScm(){ return scm }

    Boolean hasLibrary(String libName){
        SCMFileSystem fs = createFs()
        if (!fs) return false 
        SCMFile lib = fs.child(prefixBaseDir(libName))
        return lib.isDirectory()
    }

    public void loadLibrary(CpsScript script, String libName, Map libConfig){
        SCMFileSystem fs = createFs()
        if (!fs){ return }

        TemplateLogger.print("""Loading Library ${libName}
                                -- scm: ${scm.getKey()}""", [initiallyHidden:true])
        SCMFile lib = fs.child(prefixBaseDir(libName))
        lib.children().findAll{ 
            it.getName().endsWith(".groovy") 
        }.each{ stepFile ->
            StepWrapper s = StepWrapper.createFromFile(stepFile, libName, script, libConfig)
            script.getBinding().setVariable(s.getName(), s)
        }
    }

    public String prefixBaseDir(String s){
        return [baseDir, s?.trim()].findAll{ it }.join("/")
    }

    public SCMFileSystem createFs(){
        return FileSystemWrapper.createFromSCM(scm).fs
    }

    @Extension public static class DescriptorImpl extends Descriptor<TemplateLibrarySource> {}
}