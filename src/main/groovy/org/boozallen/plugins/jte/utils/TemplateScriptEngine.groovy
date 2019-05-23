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

package org.boozallen.plugins.jte.utils

import java.lang.reflect.Field
import org.boozallen.plugins.jte.binding.TemplateBinding
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.jenkinsci.plugins.workflow.cps.CpsThreadGroup

/*
    We do a lot of executing the code inside files and JTE is
    dependent on the binding being preserved.

    The default CpsGroovyShell leveraged in CpsScript's evaluate method
    is insufficient for our needs because it instantiates each shell with
    a new Binding() instead of using getBinding().

    </rant>
*/
class TemplateScriptEngine implements Serializable{
    static Script parse(String scriptText, Binding b){
        // get current CpsGroovyShell
        GroovyShell shell = CpsThreadGroup.current().getExecution().getTrustedShell()

        // define auto importing of JTE hook annotations
        ImportCustomizer ic = new ImportCustomizer()
        ic.addStarImports("org.boozallen.plugins.jte.hooks")

        // get jenkins compiler configuration
        Field configF = GroovyShell.class.getDeclaredField("config")
        configF.setAccessible(true)
        CompilerConfiguration cc = configF.get(shell)

        // add import customizers
        cc.addCompilationCustomizers(ic)
        configF.set(shell, cc)

        // parse the script 
        Script script = shell.getClassLoader().parseClass(scriptText).newInstance()

        // set the script binding to our TemplateBinding
        script.setBinding(b)

        return script 
    }
}