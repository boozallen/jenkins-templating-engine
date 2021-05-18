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
package org.boozallen.plugins.jte.util

import groovy.transform.InheritConstructors

/**
 * A generic exception to mark an exception as coming from JTE
 */
@InheritConstructors class AggregateException extends Exception {

    private List<Exception> exceptions = []

    void add(Exception e){
        if(e instanceof AggregateException){
            exceptions += e.getExceptions()
        } else{
            exceptions.push(e)
        }
    }

    int size(){
        return exceptions.size()
    }

    List<Exception> getExceptions(){
        return exceptions
    }

    String getMessage(){
        List<String> msg = ["The following errors occurred: "]
        exceptions.eachWithIndex{ e, i ->
            msg.push("${i + 1}: ${e.getMessage()}".toString())
        }
        return msg.join("\n")
    }

}
