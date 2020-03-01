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


import org.boozallen.plugins.jte.console.TemplateLogger
import org.boozallen.plugins.jte.utils.RunUtils
import org.codehaus.groovy.runtime.InvokerHelper
import jenkins.model.Jenkins

/*
  stores the aggregated & immutable pipeline configuration. 
*/
class PipelineConfig implements Serializable{
    TemplateConfigObject currentConfigObject
    Boolean firstJoin = true  

    PipelineConfig(){
      String defaultTemplateConfig = RunUtils.classLoader
                                  .loadClass("org.boozallen.plugins.jte.config.PipelineConfig")
                                  .getResource(GovernanceTier.CONFIG_FILE).text
      
      currentConfigObject = TemplateConfigDsl.parse(defaultTemplateConfig)
    }

    // for testing using any base config object
    PipelineConfig(TemplateConfigObject tco){
        this.currentConfigObject = tco
    }


    // for testing using the base pipeline config file contents
    static String baseConfigContentsFromLoader(ClassLoader ldr){
        ldr.loadClass("org.boozallen.plugins.jte.config.PipelineConfig")
                .getResource(GovernanceTier.CONFIG_FILE).text
    }

    TemplateConfigObject getConfig(){
      return currentConfigObject
    }

    /*
      
    */
    void join(TemplateConfigObject child){
      def pipeline_config
      def argCopy = TemplateConfigDsl.parse(TemplateConfigDsl.serialize(child))
      def prevCopy = TemplateConfigDsl.parse(TemplateConfigDsl.serialize(currentConfigObject))

      /*
        for first join, you always override the default JTE config
        for subsequent joins, the current configuration always wins out
        unless a merge/override take over
      */
      if (firstJoin){ 
        pipeline_config = currentConfigObject.config + child.config 
        firstJoin = false 
      } else{
        pipeline_config = child.config + currentConfigObject.config
      }

      currentConfigObject.override.each{ key ->
        if (get_prop(child.config, key) != null){
          clear_prop(pipeline_config, key)
          if(get_prop(pipeline_config, key) instanceof Map){
            get_prop(pipeline_config, key) << get_prop(child.config, key)
          } else { 
            def newValue = get_prop(child.config, key)
            set_prop(pipeline_config, key, newValue)
          }
        }
      }

      currentConfigObject.merge.each{ key ->
        if (get_prop(child.config, key) != null){
          get_prop(pipeline_config, key) << (get_prop(child.config, key) + get_prop(pipeline_config, key))
        }
      }

      // trim merge and overrides that don't apply 
      def r = getNested(pipeline_config)
      child.merge = child.merge.findAll{ m -> 
        r.keySet().collect{ it.startsWith(m) }.contains(true)
      }
      child.override = child.override.findAll{ o -> 
        r.keySet().collect{ it.startsWith(o) }.contains(true)
      }


      child.setConfig(pipeline_config)
      printJoin(child, argCopy, prevCopy)
      currentConfigObject = child

    }

    static def get_prop(o, p){
      return p.tokenize('.').inject(o){ obj, prop ->       
        obj?."$prop"
      }   
    }

    static void clear_prop(o, p){
      def last_token
      if (p.tokenize('.')){
        last_token = p.tokenize('.').last()
      } else if (InvokerHelper.getMetaClass(o).respondsTo(o, "clear", (Object[]) null)){
        o.clear()
      }
      p.tokenize('.').inject(o){ obj, prop ->    
        if (prop.equals(last_token) && InvokerHelper.getMetaClass(obj?."$prop").respondsTo(obj?."$prop", "clear", (Object[]) null)){
          obj?."$prop".clear()
        }
        obj?."$prop"
      }   
    }

    static void set_prop(o, p, n){
      def last_token
      if (p.tokenize('.')){
        last_token = p.tokenize('.').last()
      } else if (InvokerHelper.getMetaClass(o).respondsTo(o, "clear", (Object[]) null)){
        o = n 
      }
      p.tokenize('.').inject(o){ obj, prop ->    
        if (prop.equals(last_token)){
          obj?."$prop" = n 
        }
        obj?."$prop"
      }   
    }

    static def getNestedKeys(map, result = [], String keyPrefix = '') {
      map.each { key, value ->
        if (value instanceof Map) {
            getNestedKeys(value, result, "${keyPrefix}${key}.")
        } else {
            result << "${keyPrefix}${key}"
        }
      }
      return result
    }

    static def getNested(map, resultKeys = [], String keyPrefix = '') {
        def ret = [:]
        map.each { key, value ->
          def pathKey = "${keyPrefix}${key}"

          if (value instanceof Map) {
            def nestedMap = getNested(value, resultKeys, "${pathKey}.")
            if( nestedMap.isEmpty()){// we are a leaf node and empty
              ret[pathKey] = value
              resultKeys << pathKey
            } else {// we are another map so add to existing map
                ret = ret + nestedMap
            }
          } else {
            ret[pathKey] = value
            resultKeys << pathKey
          }
        }

        return ret
    }

    static printJoin(TemplateConfigObject outcome, TemplateConfigObject incoming, TemplateConfigObject prev){

        // flatten each configuration for ease of delta analysis 
        def fOutcome = getNested(outcome.getConfig())
        def fIncoming = getNested(incoming.getConfig())
        def fPrevious = getNested(prev.getConfig())


        def output = ['Pipeline Configuration Modifications']

        // Determine Configurations Added
        def configurationsAdded = fOutcome.keySet() - fPrevious.keySet()
        if (configurationsAdded){
          output << "Configurations Added:" 
          configurationsAdded.each{ key -> 
            output << "- ${key} set to ${fOutcome[key]}"
          }
        }else{
          output << "Configurations Added: None" 
        }

        // Determine Configurations Deleted
        def configurationsDeleted = (fPrevious - fOutcome).keySet().findAll{ !(it in fOutcome.keySet()) }
        if (configurationsDeleted){
          output << "Configurations Deleted:" 
          configurationsDeleted.each{ key -> 
            output << "- ${key}"
          }
        }else{
          output << "Configurations Deleted: None" 
        }
        // Determine Configurations Changed 
        def configurationsChanged = (fOutcome - fPrevious).findAll{ it.getKey() in fPrevious.keySet() }
        if (configurationsChanged){
          output << "Configurations Changed:" 
          configurationsChanged.keySet().each{ key  -> 
            output << "- ${key} changed from ${fPrevious[key]} to ${fOutcome[key]}"
          }
        }else{
          output << "Configurations Changed: None" 
        }

        // Determine Configurations Duplicated
        def configurationsDuplicated = fPrevious.intersect(fIncoming)
        if (configurationsDuplicated){
          output << "Configurations Duplicated:" 
          configurationsDuplicated.keySet().each{ key -> 
            output << "- ${key}"
          }
        }else{
          output << "Configurations Duplicated: None" 
        }

        // Determine Configurations Ignored 
        def configurationsIgnored = (fIncoming - fOutcome).keySet()
        if (configurationsIgnored){
          output << "Configurations Ignored:" 
          configurationsIgnored.each{ key -> 
            output << "- ${key}"
          }
        }else{
          output << "Configurations Ignored: None" 
        }
        
        // Print Subsequent May Merge
        def subsequentMayMerge = outcome.merge 
        if(subsequentMayMerge){
          output << "Subsequent May Merge:"
          subsequentMayMerge.each{ key -> 
            output << "- ${key}" 
          }
        }else{
          output << "Subsequent May Merge: None" 
        }

        // Print Subsequent May Override 
        def subsequentMayOverride = outcome.override 
        if(subsequentMayOverride){
          output << "Subsequent May Override:"
          subsequentMayOverride.each{ key -> 
            output << "- ${key}" 
          }
        }else{
          output << "Subsequent May Override: None" 
        }


        TemplateLogger.print( output.join("\n"), [ initiallyHidden: true, trimLines: false ])
    }

}
