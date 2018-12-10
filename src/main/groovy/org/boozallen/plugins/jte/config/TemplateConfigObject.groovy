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

class TemplateConfigObject implements Serializable{
    LinkedHashMap config = [:]
    Set<String> merge = []
    Set<String> override = []

    /*
    TODO:
        come back to this.  libraries should probably be mergeable by
        default recursively. so you dont have to specify the allowance
        of additional library configs for each library. 
        
    ArrayList getMerge(){
        if(config.libraries) merge << getNestedLibraryKeys(config.libraries)
        return merge 
    }

    def getNestedLibraryKeys(map, result = [], String keyPrefix = 'libraries.') {
        map.each { key, value ->
            if (value instanceof Map) {
                result << "${keyPrefix}${key}"
                getNestedLibraryKeys(value, result, keyPrefix += "$key.")
                keyPrefix = 'libraries.'
            }
        }
        return result
    }
    */
}