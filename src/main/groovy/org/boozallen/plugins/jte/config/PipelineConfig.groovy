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

        printJoin(child, argCopy, prevCopy)

//        TemplateLogger.print(
//                TemplateConfigDsl.printBlock([], 0,
//                        configResult([:], getNestedKeys(child.config), argCopy, currentConfigObject) ).join("\n"),
//                    [ initiallyHidden: true, trimLines: false ]
//        )


//        child.setConfig(pipeline_config)
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
        def data = [outcome:[c:outcome, nestedKeys:[]],
                    incoming:[c:incoming, nestedKeys:[]],
                    prev:[c:prev, nestedKeys:[]]]

        def output = ['JTE Pipeline Config']

        // get the nested keys and data
        data.each { k, v ->
            v['nested'] = getNested(v.c.config, v['nestedKeys'])
        }


        // get the added keys
        def keys = (data.incoming.nestedKeys - data.prev.nestedKeys).intersect(data.outcome.nestedKeys)
        output << ["Configurations Added:${keys.empty? ' None': '' }"]
        keys.each{ k ->
            output << "-${k} set to ${data.outcome.nested[k]}"
        }


        keys = data.incoming.nestedKeys.intersect(data.prev.nestedKeys)
        output << ["Configurations Changed:${keys.empty? ' None': '' }"]

        keys.each{ k ->
            output << "-${k} changed from ${data.prev.nested[k]} to ${data.outcome.nested[k]}"
        }

        keys = (data.incoming.nestedKeys - data.outcome.nestedKeys)
        output << ["Configurations Ignored:${keys.empty? ' None': '' }"]
        keys.each{ k ->
            output << "-${k}"
        }

        output << ["Subsequent may merge:${data.outcome.c.merge.empty? ' None': '' }"]
        data.outcome.c.merge.each{ k ->
            output << "-${k}"
        }

        output << ["Subsequent may override:${data.outcome.c.override.empty? ' None': '' }"]
        data.outcome.c.override.each{ k ->
            output << "-${k}"
        }

        TemplateLogger.print( output.join("\n"), [ initiallyHidden: true, trimLines: false ])
    }

    static Map configResult(Map output, List resultKeys, TemplateConfigObject... changes){
        if( null == output )
            output = [:]

        def nestedKeys = [[],[]]
        def nestedData = [[:], [:]]
        def prevConfig = changes[1]

        String incoming_and_prev = 'incoming \u2229 prev'

        changes.eachWithIndex { c, j ->
            String param = j == 0 ? 'incoming' : 'previous'

            output[param] = [config:c.config, merge:c.merge, override:c.override]

            output[param]['nested'] = nestedData[j] = getNested(c.config, nestedKeys[j])
            output[param]['nestedKeys'] = ['these' : nestedKeys[j],
                                           'result ∩ this':resultKeys.intersect(nestedKeys[j]),
                                           'result - this':resultKeys - nestedKeys[j],
                                           'this - result':nestedKeys[j] - resultKeys ]
        }

        output[incoming_and_prev] = [nestedKeys: nestedKeys[0].intersect(nestedKeys[1]),
                                data:[:] ]
        // show the difference between the join incomingument and the previous config
        output[incoming_and_prev]['nestedKeys'].each { k ->
            output[incoming_and_prev]['data'][k] = [override: prevConfig.override.contains(k),
                                               changed: nestedData[0][k] != nestedData[1][k],
                                               incoming:nestedData[0][k],  prev: nestedData[1][k]]
        }


        output['incoming - prev'] = [nestedKeys:nestedKeys[0] - nestedKeys[1], data:[:] ]
        // show what was added from the join incomingument
        output['incoming - prev']['nestedKeys'].each { k ->
            // top level keys are merge by default
            output['incoming - prev']['data'][k] = [
                    merge: prevConfig.merge.contains(k) || !k.contains("."),
                    data:nestedData[0][k]
            ]
        }


        output['prev - incoming'] = [nestedKeys: nestedKeys[1] - nestedKeys[0], data:[:] ]

        // show what was/is in the prev config
        output['prev - incoming']['nestedKeys'].each { k ->
            output['prev - incoming']['data'][k] = nestedData[1][k]
        }


        return output
    }

}
