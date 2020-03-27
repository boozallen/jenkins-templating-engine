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

import spock.lang.*

class ConsolePipelineConfigurationProviderSpec extends Specification{

    def "pipelineConfig is null if providePipelineConfig is false"(){
        setup: 
        ConsolePipelineConfigurationProvider p = new ConsolePipelineConfigurationProvider(false, "config", false, "", [])

        expect: 
        p.getPipelineConfig() == null 
    }

    def "pipelineConfig is null if providePipelineConfig is true but text area is empty"(){
        setup: 
        ConsolePipelineConfigurationProvider p = new ConsolePipelineConfigurationProvider(true, "", false, "", [])

        expect: 
        p.getPipelineConfig() == null 
    }

    def "pipelineConfig is set if providePipelineConfig is true and text area populated"(){
        setup: 
        String config = "config"
        ConsolePipelineConfigurationProvider p = new ConsolePipelineConfigurationProvider(true, config, false, "", [])

        expect: 
        p.getPipelineConfig() == config
    }

    def "defaultTemplate is null if provideDefaultTemplate is false"(){
        setup: 
        ConsolePipelineConfigurationProvider p = new ConsolePipelineConfigurationProvider(false, "", false, "template", [])

        expect: 
        p.getDefaultTemplate() == null 
    }
    
    def "defaultTemplate is null if provideDefaultTemplate is true but text area is empty"(){
        setup: 
        ConsolePipelineConfigurationProvider p = new ConsolePipelineConfigurationProvider(false, "", true, "", [])

        expect: 
        p.getDefaultTemplate() == null 
    }
    
    def "defaultTemplate is set if provideDefaultTemplate is true and text area populated"(){
        setup: 
        String template = "template"
        ConsolePipelineConfigurationProvider p = new ConsolePipelineConfigurationProvider(false, "", true, template, [])

        expect: 
        p.getDefaultTemplate() == template
    }

    def "getConfig returns null if pipelineConfig not present"(){
        setup: 
            TemplateConfigObject confObj = Mock()
            GroovySpy(TemplateConfigDsl, global:true)
            TemplateConfigDsl.parse(_) >> confObj 
            ConsolePipelineConfigurationProvider p = new ConsolePipelineConfigurationProvider(false, "", false, "", [])

        expect: 
            p.getConfig() == null 
    }

    def "getConfig returns TemplateConfigObject if pipelineConfig present"(){
        setup: 
            TemplateConfigObject confObj = Mock()
            GroovySpy(TemplateConfigDsl, global:true)
            TemplateConfigDsl.parse(_) >> confObj 
            ConsolePipelineConfigurationProvider p = new ConsolePipelineConfigurationProvider(true, "config", false, "", [])

        expect: 
            p.getConfig() == confObj
    }

    def "getJenkinsfile returns the default template if present"(){
        setup: 
        String defaultTemplate = "default template" 
        ConsolePipelineConfigurationProvider p = new ConsolePipelineConfigurationProvider(false, "", true, defaultTemplate, [])

        expect: 
        p.getJenkinsfile() == defaultTemplate 
    }

    def "getJenkinsfile returns null if the default template not present"(){
        setup: 
        String defaultTemplate = "" 
        ConsolePipelineConfigurationProvider p = new ConsolePipelineConfigurationProvider(false, "", true, defaultTemplate, [])

        expect: 
        p.getJenkinsfile() == null 
    }
    
    def "getTemplate returns null if no templates defined"(){
        setup: 
        List<ConsolePipelineTemplate> pipelineCatalog = [] 
        ConsolePipelineConfigurationProvider p = new ConsolePipelineConfigurationProvider(false, "", false, "", pipelineCatalog)

        expect: 
        p.getTemplate("myTemplate") == null 
    }
    
    def "getTemplate returns null if template not present in list"(){
        setup: 
        ConsolePipelineTemplate a = new ConsolePipelineTemplate()
        a.setName("a")
        a.setTemplate("template a")
        ConsolePipelineTemplate b = new ConsolePipelineTemplate()
        a.setName("b")
        a.setTemplate("template b")
        List<ConsolePipelineTemplate> pipelineCatalog = [a, b]
        ConsolePipelineConfigurationProvider p = new ConsolePipelineConfigurationProvider(false, "", false, "", pipelineCatalog)

        expect: 
        p.getTemplate("myTemplate") == null 
    }
    
    def "getTemplate returns template if present"(){
        setup: 
        ConsolePipelineTemplate a = new ConsolePipelineTemplate()
        a.setName("a")
        a.setTemplate("template a")
        ConsolePipelineTemplate b = new ConsolePipelineTemplate()
        b.setName("b")
        b.setTemplate("template b")
        List<ConsolePipelineTemplate> pipelineCatalog = [a, b]
        ConsolePipelineConfigurationProvider p = new ConsolePipelineConfigurationProvider(false, "", false, "", pipelineCatalog)

        expect: 
        p.getTemplate("a") == "template a"
        p.getTemplate("b") == "template b" 
    } 

}