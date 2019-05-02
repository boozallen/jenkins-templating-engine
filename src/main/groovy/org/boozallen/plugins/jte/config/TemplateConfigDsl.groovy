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

import org.codehaus.groovy.control.CompilerConfiguration
import org.kohsuke.groovy.sandbox.SandboxTransformer

/*
  Parses an Template Config File and returns a TemplateConfigObject

  example usage:

    TemplateConfigObject my_config = TemplateConfigDsl.parse("""
      libraries{
        owasp_zap{
          target = "example.com"
        }
      }
    """)
*/
class TemplateConfigDsl implements Serializable{
  
  static TemplateConfigObject parse(String script_text){

    TemplateConfigObject templateConfig = new TemplateConfigObject()
    
    Binding our_binding = new Binding(templateConfig: templateConfig)
    
    CompilerConfiguration cc = new CompilerConfiguration()
    cc.addCompilationCustomizers(new SandboxTransformer())
    cc.scriptBaseClass = TemplateConfigBuilder.class.name
    
    GroovyShell sh = new GroovyShell(TemplateConfigDsl.classLoader, our_binding, cc);
    
    TemplateConfigDslSandbox sandbox = new TemplateConfigDslSandbox()
    sandbox.register();
    try {
      sh.evaluate script_text
    }finally {
      sandbox.unregister();
    }
    
    return templateConfig
  }

}
