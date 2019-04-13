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

package org.boozallen.plugins.jte.binding

import spock.lang.* 
import org.junit.*
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.boozallen.plugins.jte.config.TemplateConfigObject
import org.boozallen.plugins.jte.config.TemplateConfigException

class ApplicationEnvironmentSpec extends Specification{

    TemplateBinding binding = new TemplateBinding() 
    CpsScript script = GroovyMock(CpsScript)

    def setup(){
        _ * script.getBinding() >> {
            return binding 
        }
    }

    void injectKeywords(Map keywords){
        TemplateConfigObject config = new TemplateConfigObject(config: [
            keywords: keywords
        ])
        Keyword.Injector.doInject(config, script)
    }

    def "injector inserts keyword into binding"(){
        when: 
            injectKeywords(["a": 1])
        then: 
            assert binding.hasVariable("a")
            assert binding.getVariable("a") instanceof Keyword 
    }

}