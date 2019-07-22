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

import org.apache.commons.lang.StringEscapeUtils
import org.codehaus.groovy.control.CompilerConfiguration
import org.kohsuke.groovy.sandbox.SandboxTransformer
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

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

  static String serialize(TemplateConfigObject configObj){
    Map config = new JsonSlurper().parseText(JsonOutput.toJson(configObj.getConfig()))

    // inserts merge keys into config Map for printing 
    configObj.merge.each{ merge -> 
      if (!merge){
        config.merge = true 
      }
      def last_token
      if (merge.tokenize('.')){
        last_token = merge.tokenize('.').last()
      }
      merge.tokenize('.').inject(config){ obj, prop ->
        if (prop.equals(last_token)){
          obj."${prop}".merge = true 
          return obj 
        }else{
          return obj."${prop}" 
        }
      } 
    }

    // inserts override keys into config Map for printing 
    configObj.override.each{ override -> 
      def last_token
      if (!override){
        config.override = true 
      }
      if (override.tokenize('.')){
        last_token = override.tokenize('.').last()
      }
      override.tokenize('.').inject(config){ obj, prop ->
        if (prop.equals(last_token)){
          obj."${prop}".override = true 
          return obj 
        }else{
          return obj."${prop}" 
        }
      }
    }  

    def depth = 0
    ArrayList file = [] 
    return printBlock(file, depth, config).join("\n")
  }

  static ArrayList printBlock(ArrayList file, depth, Map block){
    String tab = "    "
    block.each{ key, value -> 
      if(value instanceof Map){
        if (value == [:]){
          file += "${tab*depth}${key}"
        }else{
          file += "${tab*depth}${key}{"
          depth++
          file = printBlock(file, depth, value)
          depth-- 
          file += "${tab*depth}}"
        }
      }else{
        if (value instanceof String){
          file += "${tab*depth}${key} = '${StringEscapeUtils.escapeJava(value)}'"
        } else if (value instanceof ArrayList){
          file += "${tab*depth}${key} = ${value.inspect()}" 
        }else{
          file += "${tab*depth}${key} = ${value}" 
        }
      }
    }
    return file 
  }

}
