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
import org.boozallen.plugins.jte.utils.TemplateScriptEngine
import org.boozallen.plugins.jte.console.TemplateLogger
import org.jenkinsci.plugins.workflow.cps.CpsScript
import hudson.Extension 
import jenkins.model.Jenkins

/*
    represents a group of library steps to be called. 
*/
class Stage extends TemplatePrimitive {
    CpsScript script 
    String name
    ArrayList<String> steps

    Stage(){}

    Stage(CpsScript script, String name, ArrayList<String> steps){
        this.script = script
        this.name = name
        this.steps = steps 
    }

    void call(){
        /*
            any invocation of pipeline code from a plugin class of more than one
            executable script or closure requires to be parsed through the execution
            shell or the method returns prematurely after the first execution
        */
        String invoke =  Jenkins.instance
                                .pluginManager
                                .uberClassLoader
                                .loadClass("org.boozallen.plugins.jte.binding.Stage")
                                .getResource("StageImpl.groovy")
                                .text
        TemplateLogger.print "[Stage - ${name}]" 
        TemplateScriptEngine.parse(invoke, script.getBinding())(script, steps)
    }

    void throwPreLockException(){
        throw new TemplateException ("The Stage ${name} is already defined.")
    }

    void throwPostLockException(){
        throw new TemplateException ("The variable ${name} is reserved as a template Stage.")
    }

    @Extension static class Injector extends TemplatePrimitiveInjector {
        static void doInject(TemplateConfigObject config, CpsScript script){
            config.getConfig().stages.each{name, steps ->
                ArrayList<String> stepsList = new ArrayList()
                steps.collect(stepsList){ it.key }
                script.getBinding().setVariable(name, new Stage(script, name, stepsList))
            }
        }
    }

}