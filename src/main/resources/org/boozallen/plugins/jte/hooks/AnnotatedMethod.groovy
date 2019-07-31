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

import org.boozallen.plugins.jte.console.TemplateLogger
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.InvokerInvocationException
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted 
import org.boozallen.plugins.jte.binding.* 

class AnnotatedMethod implements Serializable{
    String annotationName 
    def stepWrapper 
    String methodName

    AnnotatedMethod(String annotationName, String methodName, def stepWrapper){
        this.annotationName = annotationName
        this.methodName = methodName
        this.stepWrapper = stepWrapper 
    } 

    void invoke(Map context){
        try{
            TemplateLogger.print "[@${annotationName} - ${stepWrapper.library}/${stepWrapper.name}.${methodName}]"
            InvokerHelper.getMetaClass(stepWrapper.impl).invokeMethod(stepWrapper.impl, methodName, context);
        } catch (Exception x) {
            throw new InvokerInvocationException(x);
        }
    }
}   