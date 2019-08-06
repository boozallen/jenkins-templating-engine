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

package org.boozallen.plugins.jte.config.libraries

import org.boozallen.plugins.jte.utils.FileSystemWrapper
import org.boozallen.plugins.jte.config.TemplateConfigDsl
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


abstract class LibraryProvider extends AbstractDescribableImpl<LibraryProvider>{
    public static final String CONFIG_FILE = "library_config.groovy" 

    /*
        implementing methods return true if library is present 
        and false if not. 
    */
    abstract public Boolean hasLibrary(String libraryName)

    /*
        implementing methods should check for the existence of 
        CONFIG_FILE in the library and pass file contents as string to 
        doLibraryConfigValidation
    */
    abstract public List loadLibrary(CpsScript script, String libName, Map libConfig)

    public Map libConfigToMap(String configFile) {
        return TemplateConfigDsl.parse(configFile).getConfig()
    }

    public List doLibraryConfigValidation(String configFile, Map libConfig){
        Map allowedConfig = libConfigToMap(configFile)

        ArrayList libConfigErrors = [] 

        // define keysets in dot notation 
        ArrayList keys = getNestedKeys(libConfig).collect{ it.toString() }
        ArrayList required = getNestedKeys(allowedConfig.fields.required).collect{ it.toString() }
        ArrayList optional = getNestedKeys(allowedConfig.fields.optional).collect{ it.toString() }

        // validate required keys 
        required.each{ requiredKey  -> 
            if(requiredKey in keys){
                keys -= requiredKey
                def actual = getProp(libConfig, requiredKey)
                def expected = getProp(allowedConfig.fields.required, requiredKey)
                if (!validateType(actual, expected)){
                    if (expected instanceof java.util.regex.Pattern){
                        libConfigErrors << "Field ${requiredKey} must be a String matching ${expected} but is [${actual}]"
                    } else if (expected instanceof ArrayList){
                        libConfigErrors << "Field '${requiredKey}' must be one of ${expected} but is [${actual}]"
                    } else {
                        libConfigErrors << "Field '${requiredKey}' must be a ${expected.getSimpleName()} but is a ${actual.getClass().getSimpleName()}"
                    }
                }
            } else{
                libConfigErrors << "Missing required field '${requiredKey}'" 
            }
        }

        // validate optional keys 
        optional.each{ optionalKey -> 
            if(optionalKey in keys){
                keys -= optionalKey 
                def actual = getProp(libConfig, optionalKey)
                def expected = getProp(allowedConfig.fields.optional, optionalKey)
                if (!validateType(actual, expected)){
                    if (expected instanceof java.util.regex.Pattern){
                        libConfigErrors << "Field ${optionalKey} must be a String matching ${expected} but is [${actual}]"
                    } else if (expected instanceof ArrayList){
                        libConfigErrors << "Field '${optionalKey}' must be one of ${expected} but is [${actual}]"
                    } else {
                        libConfigErrors << "Field '${optionalKey}' must be a ${expected.getSimpleName()} but is a ${actual.getClass().getSimpleName()}"
                    }
                }
            }
        }

        // validate that there are no extraneous keys 
        keys.each{ key -> 
            libConfigErrors << "Field '${key}' is not used." 
        }

        return libConfigErrors
    }

    public def getProp(o, p){
        return p.tokenize('.').inject(o){ obj, prop ->       
            obj?."$prop"
        }   
    }

    public def getNestedKeys(map, result = [], String keyPrefix = '') {
        map.each { key, value ->
            if (value instanceof Map) {
                getNestedKeys(value, result, "${keyPrefix}${key}.")
            } else {
                result << "${keyPrefix}${key}"
            }
        }
        return result
    }

    /*
        In general here, we're looking to validate intent 
        over specifics of what class they want.  It's unlikely
        the difference between boolean or Boolean, or Double 
        vs BigDecimal vs Float will make a difference for a
        JTE configuration file and we should strive to avoid 
        confusion when people specify a validation. 
    */
    public Boolean validateType(actual, expected){
        switch(expected){ 
            case [ boolean, Boolean ]: 
                return actual.getClass() in [ boolean, Boolean ]
                break     
            case String: 
                return actual.getClass() in [ String,  org.codehaus.groovy.runtime.GStringImpl ]
                break
            case [ Integer, int]: 
                return actual.getClass() in [ Integer, int ]
                break        
            case [ Double, BigDecimal, Float ]: 
                return actual.getClass() in [ Double, BigDecimal, Float ]
                break 
            case Number: 
                return actual instanceof Number
                break
            case { expected instanceof java.util.regex.Pattern }: 
                if(!(actual.getClass() in [ String,  org.codehaus.groovy.runtime.GStringImpl ])){
                    return false 
                }
                return actual.matches(expected)
                break
            case { expected instanceof ArrayList }:
                return actual in expected
                break
            default: 
                TemplateLogger.printWarning("Library Validator: Not sure how to handle value ${expected} with class ${expected.class}")
                return true 
                break
        } 
    }

}
