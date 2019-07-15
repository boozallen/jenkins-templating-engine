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

package org.boozallen.plugins.jte.binding.injectors

import org.boozallen.plugins.jte.binding.*
import org.boozallen.plugins.jte.utils.TemplateScriptEngine
import org.boozallen.plugins.jte.config.TemplateConfigObject
import org.boozallen.plugins.jte.config.TemplateConfigException
import org.boozallen.plugins.jte.config.TemplateLibrarySource
import org.boozallen.plugins.jte.config.GovernanceTier
import org.boozallen.plugins.jte.console.TemplateLogger
import hudson.Extension 
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.cps.CpsScript

@Extension public class LibraryLoader extends TemplatePrimitiveInjector {

    static void doInject(TemplateConfigObject config, CpsScript script){
        // 1. Inject steps from loaded libraries
        List<GovernanceTier> tiers = GovernanceTier.getHierarchy() 
        List<TemplateLibrarySource> sources = tiers.collect{ it.librarySources }.flatten().minus(null)
        
        config.getConfig().libraries.each{ libName, libConfig ->
            TemplateLibrarySource s = sources.find{ it.hasLibrary(libName) }
            if (!s){ 
                throw new TemplateConfigException("Library ${libName} Not Found.") 
            }
            s.loadLibrary(script, libName, libConfig)
        }
        // 2. Inject steps with default step implementation for configured steps
        TemplateBinding binding = script.getBinding() 
        config.getConfig().steps.findAll{ stepName, stepConfig ->
            if (binding.hasStep(stepName)){
                TemplateLogger.printWarning """Configured step ${stepName} ignored.
                                               -- Loaded by the ${binding.getStep(stepName).library} Library."""
                return false 
            }
            return true 
        }.each{ stepName, stepConfig ->
            TemplateLogger.print "Creating step ${stepName} from the default step implementation."
            Class StepWrapper = getPrimitiveClass() 
            binding.setVariable(stepName, StepWrapper.createDefaultStep(script, stepName, stepConfig))
        }
    }

    static void doPostInject(TemplateConfigObject config, CpsScript script){
        // 3. Inject a passthrough step for steps not defined (either as steps or other primitives)
        TemplateBinding binding = script.getBinding()
        config.getConfig().template_methods.findAll{ step ->
            !(step.key in binding.registry)
        }.each{ step -> 
            Class StepWrapper = getPrimitiveClass() 
            binding.setVariable(step.key, StepWrapper.createNullStep(step.key, script))
        }
    }

    static getPrimitiveClass(){
        String self = "org.boozallen.plugins.jte.binding.injectors.LibraryLoader"
        String classText = Jenkins.instance
                                    .pluginManager
                                    .uberClassLoader
                                    .loadClass(self)
                                    .getResource("StepWrapper.groovy")
                                    .text
        return TemplateScriptEngine.parseClass(classText)
    }

}