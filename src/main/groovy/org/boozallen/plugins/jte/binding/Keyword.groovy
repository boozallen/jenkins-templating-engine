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
import org.jenkinsci.plugins.workflow.cps.CpsScript
import hudson.Extension 

/*
    represents a protected variable in the jenkinsfile
*/
class Keyword extends TemplatePrimitive{
    String var_name
    Object value

    Keyword(){}

    Object getValue(){
        return value
    }

    Keyword(String var_name, Object value){ 
        this.var_name = var_name 
        this.value = value
    }

    void throwPreLockException(){
        throw new TemplateException ("Keyword ${var_name} already defined.")
    }

    void throwPostLockException(){
        throw new TemplateException ("Variable ${var_name} is reserved as a template Keyword.")
    }

    @Extension static class Injector extends TemplatePrimitiveInjector {
        static void doInject(TemplateConfigObject config, CpsScript script){
            config.getConfig().keywords.each{ key, value ->
                script.getBinding().setVariable(key, new Keyword(key, value))
            }
        }
    }


}