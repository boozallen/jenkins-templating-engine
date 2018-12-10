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

//import org.boozallen.plugins.jte.extensions.*
import org.boozallen.plugins.jte.config.*
import org.boozallen.plugins.jte.hooks.*
import org.boozallen.plugins.jte.Utils
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.InvokerInvocationException
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted 
import hudson.Extension 

/*
    represents a library step
*/
class StepWrapper extends TemplatePrimitive{
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
        def result 
        /*
            TODO: 
                - replace context as a map to being a TemplateContext class

                no more duck typing or implicit assumptions
        */
        def context = [
            step: name, 
            library: library,
            status: script.currentBuild.result
        ]
        try{
            //Hooks.invoke(BeforeStep, script.getBinding(), context)
            Utils.getLogger().println "[JTE] Executing step ${name} from the ${library} Library" 
            result = InvokerHelper.getMetaClass(impl).invokeMethod(impl, "call", args)
        } catch (Exception x) {
            script.currentBuild.result = "Failure"
            throw new InvokerInvocationException(x)
        } finally{
            context.status = script.currentBuild.result
            Hooks.invoke(AfterStep, script.getBinding(), context)
            Hooks.invoke(Notifier,  script.getBinding(), context)
        }
        return result
    }

    void throwPreLockException(){
        throw new TemplateException ("Library Step Collision. The step ${name} already defined via the ${library} library.")
    }
    void throwPostLockException(){
        throw new TemplateException ("Library Step Collision. The variable ${name} is reserved as a library step via the ${library} library.")
    }

}

