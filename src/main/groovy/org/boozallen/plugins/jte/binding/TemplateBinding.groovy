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

import groovy.lang.Binding
import org.jenkinsci.plugins.workflow.cps.CpsThread
import org.jenkinsci.plugins.workflow.cps.DSL
import org.codehaus.groovy.runtime.InvokerHelper

class TemplateBinding extends Binding implements Serializable{
    public final String STEPS_VAR = "steps"
    public Set<String> registry = new ArrayList() 
    private Boolean locked = false 

    TemplateBinding(){
        CpsThread c = CpsThread.current()
        if (c) setVariable(STEPS_VAR, new DSL(c.getExecution().getOwner()))
    }
    
    public void lock(){ locked = true }

    @Override
    public void setVariable(String name, Object value) {
        if (name in registry){
            if (locked) variables.get(name).throwPostLockException()
            else variables.get(name).throwPreLockException() 
        }
        if (value in TemplatePrimitive) registry << name
        super.setVariable(name, value)
    }

    @Override
    public Object getVariable(String name){
        if (!variables)
            throw new MissingPropertyException(name, this.getClass());

        Object result = variables.get(name)

        if (!result && !variables.containsKey(name))
            throw new MissingPropertyException(name, this.getClass());

        if (result in TemplatePrimitive && InvokerHelper.getMetaClass(result).respondsTo(result, "getValue", (Object[]) null))
            result = result.getValue()

        return result
    }

}