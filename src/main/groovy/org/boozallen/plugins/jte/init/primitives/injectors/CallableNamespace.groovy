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
package org.boozallen.plugins.jte.init.primitives.injectors

import org.boozallen.plugins.jte.init.primitives.PrimitiveNamespace

/**
 * library step namespace under libraries namespace
 */
class CallableNamespace extends PrimitiveNamespace{

    String getName(){
        return super.name ?: "steps"
    }

    /**
     * should probably only override one
     * implemented because call(){ jte.libraries.$library.$step(...)} did not work
     * @param name
     * @param args
     * @return
     */
    Object methodMissing(String name, Object args) {
        Object step = primitives[name]
        if(step){
            return step(args)
        }

        throw new MissingMethodException(name, this.getClass(), args)
        //return null // super.methodMissing(name, args)
    }

}
