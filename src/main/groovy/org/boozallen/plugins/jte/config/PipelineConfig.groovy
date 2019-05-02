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

import org.codehaus.groovy.runtime.InvokerHelper
import jenkins.model.Jenkins

/*
  stores the aggregated & immutable pipeline configuration. 
*/
class PipelineConfig implements Serializable{
    TemplateConfigObject currentConfigObject
    Boolean firstJoin = true  

    PipelineConfig(){
      String defaultTemplateConfig = Jenkins.instance
                                  .getPluginManager()
                                  .uberClassLoader
                                  .loadClass("org.boozallen.plugins.jte.config.PipelineConfig")
                                  .getResource(GovernanceTier.CONFIG_FILE).text
      
      currentConfigObject = TemplateConfigDsl.parse(defaultTemplateConfig)
    }

    TemplateConfigObject getConfig(){
      return currentConfigObject
    }

    /*
      
    */
    void join(TemplateConfigObject child){
      def pipeline_config
      if (firstJoin){
        pipeline_config = currentConfigObject.config + child.config 
        firstJoin = false 
      } else{
        pipeline_config = child.config + currentConfigObject.config
      }

      currentConfigObject.override.each{ key ->
        if (get_prop(child.config, key)){
          clear_prop(pipeline_config, key)
          get_prop(pipeline_config, key) << get_prop(child.config, key) 
        }
      }

      currentConfigObject.merge.each{ key ->
        if (get_prop(child.config, key)){
          get_prop(pipeline_config, key) << (get_prop(child.config, key) + get_prop(pipeline_config, key))
        }
      }

      child.setConfig(pipeline_config)
      currentConfigObject = child

    }

    static def get_prop(o, p){
      return p.tokenize('.').inject(o){ obj, prop ->       
        obj?."$prop"
      }   
    }

    static void clear_prop(o, p){
      def last_token
      if (p.tokenize('.')) last_token = p.tokenize('.').last()
      else if (InvokerHelper.getMetaClass(o).respondsTo(o, "clear", (Object[]) null)){
        o.clear()
      }
      p.tokenize('.').inject(o){ obj, prop ->    
        if (prop.equals(last_token) && InvokerHelper.getMetaClass(obj?."$prop").respondsTo(obj?."$prop", "clear", (Object[]) null)){
          obj?."$prop".clear()
        }
        obj?."$prop"
      }   
    }

}
