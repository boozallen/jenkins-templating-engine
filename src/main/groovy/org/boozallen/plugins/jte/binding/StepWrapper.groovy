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

import org.boozallen.plugins.jte.config.*
import org.boozallen.plugins.jte.hooks.*
import org.boozallen.plugins.jte.Utils
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.codehaus.groovy.runtime.InvokerHelper
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted 
import jenkins.model.Jenkins

/*
    represents a library step
*/
class StepWrapper extends TemplatePrimitive{
    public static final String libraryConfigVariable = "config" 
    private Object impl
    private CpsScript script
    private String name
    private String library 
    
    StepWrapper(){}

    
    StepWrapper(CpsScript script, Object impl, String name, String library){ 
        this.script = script
        
        if(!InvokerHelper.getMetaClass(impl).respondsTo(impl, "call")){
            throw new TemplateException ("StepWrapper impl object must respond to 'call'. Was passed ${impl.getClass()}")
        }

        this.impl = impl
        this.name = name
        this.library = library 
    }

    @Whitelisted
    def call(Object... args){
        /*
            any invocation of pipeline code from a plugin class of more than one
            executable script or closure requires to be parsed through the execution
            shell or the method returns prematurely after the first execution
        */
        String invoke =  Jenkins.instance
                                .pluginManager
                                .uberClassLoader
                                .loadClass("org.boozallen.plugins.jte.binding.StepWrapper")
                                .getResource("StepWrapperImpl.groovy")
                                .text
        return Utils.parseScript(invoke, script.getBinding())(name, library, script, impl, args)
    }

    void throwPreLockException(){
        throw new TemplateException ("Library Step Collision. The step ${name} already defined via the ${library} library.")
    }
    void throwPostLockException(){
        throw new TemplateException ("Library Step Collision. The variable ${name} is reserved as a library step via the ${library} library.")
    }

}

