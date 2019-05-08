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

package org.boozallen.plugins.jte.hooks

import org.boozallen.plugins.jte.Utils
import org.boozallen.plugins.jte.utils.TemplateScriptEngine
import org.boozallen.plugins.jte.binding.* 
import java.lang.annotation.Annotation
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted 
import jenkins.model.Jenkins

class Hooks implements Serializable{

    @Whitelisted
    static List<AnnotatedMethod> discover(Class<? extends Annotation> a, TemplateBinding b){
        List<AnnotatedMethod> discovered = new ArrayList() 

        List<StepWrapper> stepWrappers = b.getVariables().collect{ it.value }.findAll{ it instanceof StepWrapper }

        stepWrappers.each{ step ->
            step.impl.class.methods.each{ method ->
                if (method.getAnnotation(a)){
                    AnnotatedMethod am = new AnnotatedMethod(a.getSimpleName(), method.name, step)
                    discovered.push(am) 
                }            
            }
        }

        return discovered
    }

    @Whitelisted
    static void invoke(Class<? extends Annotation> a, TemplateBinding b, Map context = [:]){
        /*
            any invocation of pipeline code from a plugin class of more than one
            executable script or closure requires to be parsed through the execution
            shell or the method returns prematurely after the first execution
        */
        String invoke =  Jenkins.instance
                                .pluginManager
                                .uberClassLoader
                                .loadClass("org.boozallen.plugins.jte.hooks.Hooks")
                                .getResource("Invoke.groovy")
                                .text
        TemplateScriptEngine.parse(invoke, b)(a, b, context)
    }
    
}