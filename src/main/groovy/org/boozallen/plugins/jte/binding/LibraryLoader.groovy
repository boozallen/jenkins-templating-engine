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

package org.boozallen.plugins.jte.binding

import org.boozallen.plugins.jte.config.TemplateConfigObject
import org.boozallen.plugins.jte.config.TemplateConfigException
import org.boozallen.plugins.jte.config.GovernanceTier
import org.boozallen.plugins.jte.Utils
import hudson.Extension 
import org.jenkinsci.plugins.workflow.cps.CpsScript
import hudson.FilePath
import jenkins.scm.api.SCMFileSystem
import jenkins.scm.api.SCMFile 
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import java.io.PrintStream
import hudson.model.ItemGroup
import jenkins.model.Jenkins


@Extension public class LibraryLoader extends TemplatePrimitiveInjector {
    /*
        TODO: 
           need to test an SCM that isn't supported by 
           SCMFileSystem. 
    */
    static void doInject(TemplateConfigObject config, CpsScript script){

        WorkflowJob job = Utils.getCurrentJob()
        PrintStream logger = Utils.logger 
        ItemGroup<?> parent = job.getParent() 
        List<GovernanceTier> tiers = GovernanceTier.getHierarchy() 

        for (configLibrary in config.getConfig().libraries){
            String libName = configLibrary.getKey()
            Map libConfig = configLibrary.getValue() 

            boolean foundLibrary = false 
            // check folders in ascending order for library in defined sources
            tierLoop: 
            for (tier in tiers){
                for(librarySource in tier.librarySources){
                    // try lighweight checkout 
                    SCMFileSystem fs = Utils.createSCMFileSystemOrNull(librarySource.scm, job, parent)
                    if (fs){ 
                        SCMFile libDir = fs.child(libName)
                        if (libDir.isDirectory()){
                            logger.println "[JTE] Loading Library ${libName} from ${librarySource.scm.getKey()}"
                            libDir.children().findAll{ it.getName().endsWith(".groovy") }.each{ step ->
                                String stepName = step.getName() - ".groovy" 
                                Script stepImpl = Utils.parseScript(step.contentAsString(), script.getBinding())
                                stepImpl.metaClass.getConfig << { return libConfig }
                                StepWrapper sw = new StepWrapper(script, stepImpl, stepName, libName) 
                                script.getBinding().setVariable(stepName, sw) 
                            }
                            foundLibrary = true 
                            break tierLoop
                        }else{
                            continue 
                        }
                    }
                    // try heavyweight checkout 
                    logger.println "[JTE] Checking ${librarySource.scm.getKey()} for ${libName} Library" 
                    FilePath dir = doHeavyWeightCheckout(librarySource.scm, job, parent) 
                    FilePath libDir = dir.child(libName)
                    if (libDir.isDirectory()){
                        logger.println "[JTE] Loading Library ${libName} from ${librarySource.scm.getKey()}"
                        for (step in libDir.list("*.groovy")){
                            String stepName = step.getBaseName()
                            Script stepImpl = Utils.parseScript(step.readToString(), script.getBinding())
                            stepImpl.metaClass.getConfig << { return libConfig }
                            StepWrapper sw = new StepWrapper(script, stepImpl, stepName, libName) 
                            script.getBinding().setVariable(stepName, sw) 
                        }
                        foundLibrary = true 
                        break tierLoop 
                    }
                }
            }

            if (!foundLibrary){
                throw new TemplateConfigException("Library ${libName} Not Found.") 
            }

        }
    }

    /*
        plug any holes in the template 
    */
    static void doPostInject(TemplateConfigObject config, CpsScript script){

        // create default step implementation Script 
        String defaultStepImplString = Jenkins.instance
                                    .pluginManager
                                    .uberClassLoader
                                    .loadClass("org.boozallen.plugins.jte.binding.LibraryLoader")
                                    .getResource("defaultStepImplementation.groovy")
                                    .text        

        Map pipelineTemplateMethods = config.getConfig().template_methods
        pipelineTemplateMethods.each{ it.value.isDefined = false }

        Map configSteps = config.getConfig().steps 
        configSteps.each{ it.value.isDefined = true }

        Map possibleSteps = pipelineTemplateMethods + configSteps
        Map stepsToCreate = possibleSteps.findAll{ !(it.key in script.getBinding().registry) }

        stepsToCreate.each{ stepName, stepConfig -> 
            if (stepConfig.isDefined){
                Script defaultStepImpl = Utils.parseScript(defaultStepImplString, script.getBinding())
                //stepConfig["step"] = stepName 
                defaultStepImpl.metaClass.getConfig << { return stepConfig }
                StepWrapper sw = new StepWrapper(script, defaultStepImpl, stepName, "Default Step Implementation") 
                script.getBinding().setVariable(stepName, sw)
            } else {
                /*
                    TODO: 
                        design question. 
                        right now -> steps not defined are just a silent passthrough. 

                        need a clean implementation for required steps causing failure
                        and overriding the default step implementation. 

                        the passthrough could happen in the default step implementation
                        or the StepWrapper but i'm weary of blurring the lines between
                        abstractions. 
                */
                script.getBinding().setVariable(stepName, { -> })
            }
        }

    }

}