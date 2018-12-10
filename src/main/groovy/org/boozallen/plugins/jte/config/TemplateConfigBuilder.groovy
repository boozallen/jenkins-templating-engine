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

import java.util.ArrayList
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted

/*
    Base class during Config File DSL execution.
    Basically just turns the nested closure syntax
    into a nested hash map while recognizing the keys
    "merge" and "override" to put onto the TemplateConfigObject

    the templateConfig variable here comes from the instance
    being created and is instantiated in TemplateConfigDsl
*/
abstract class TemplateConfigBuilder extends Script{
    ArrayList object_stack = []
    ArrayList node_stack = []

    @Whitelisted
    Object methodMissing(String name, args){
        object_stack.push([:])
        node_stack.push(name)

        args[0]()

        def node_config = object_stack.pop()
        def node_name = node_stack.pop()

        if (object_stack.size()) 
            object_stack.last() << [ (node_name): node_config ]
        else 
            templateConfig.config << [ (name): node_config]
    }

    @Whitelisted
    Object getVariable(String name){
        return super.getVariable(name)
    }

    @Whitelisted
    void setProperty(String name, value){
        if (name.equals("merge") && value.equals(true))
            templateConfig.merge << node_stack.join(".")
        else if (name.equals("override") && value.equals(true)) 
            templateConfig.override << node_stack.join(".")
        else if (object_stack.size()) 
            object_stack.last()[name] = value
        else 
            templateConfig.config[name] = value
    }

    @Whitelisted
    void propertyMissing(String name){
        if (object_stack.size()) object_stack.last()[name] = [:]
        else templateConfig.config[name] = [:]
    }

}