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
package org.boozallen.plugins.jte.init.dsl

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

class PipelineConfigurationObject implements Serializable{
    LinkedHashMap config = [:]
    Set<String> merge = []
    Set<String> override = []

    static PipelineConfigurationObject createFromMap(Map config){
        LinkedHashMap c = new JsonSlurper().parseText(JsonOutput.toJson(config))
        Set<String> merge = []
        Set<String> override = []

        (merge, override) = findMergeAndOverride([], [], [], c)
        return new PipelineConfigurationObject(config: c, merge: merge, override: override)
    }

    static def findMergeAndOverride(ArrayList merge, ArrayList override, ArrayList breadcrumbs, Map config){
        Boolean removeMerge = false
        Boolean removeOverride = false
        config.each{ key, value ->
            if (key.equals("merge") && value){
                merge += breadcrumbs.join(".")
                removeMerge = true
            }
            if (key.equals("override") && value){
                removeOverride = true
                override += breadcrumbs.join(".")
                config.remove("override")
            }
            if (value instanceof Map){
                breadcrumbs += key
                (merge, override) = findMergeAndOverride(merge,override,breadcrumbs, value)
                breadcrumbs -= key
            }
        }

        // done after the fact to avoid ConcurrentModificationException
        if (removeMerge){
            config.remove("merge")
        }
        if (removeOverride){
            config.remove("override")
        }
        return [ merge, override ]
    }
}
