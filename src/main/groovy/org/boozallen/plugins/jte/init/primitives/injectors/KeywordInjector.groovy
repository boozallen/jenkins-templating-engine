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
package org.boozallen.plugins.jte.init.primitives.injectors

import hudson.Extension
import jenkins.model.Jenkins
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.primitives.PrimitiveNamespace
import org.boozallen.plugins.jte.init.primitives.TemplateBinding
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * creates Keywords and populates the run's {@link org.boozallen.plugins.jte.init.primitives.TemplateBinding}
 */
@Extension class KeywordInjector extends TemplatePrimitiveInjector {

    static Class getPrimitiveClass(){
        ClassLoader uberClassLoader = Jenkins.get().pluginManager.uberClassLoader
        String self = this.getMetaClass().getTheClass().getName()
        String classText = uberClassLoader.loadClass(self).getResource("Keyword.groovy").text
        return parseClass(classText)
    }

    private static final String KEY = "keywords"
    private static final String TYPE_DISPLAY_NAME = "Keyword"
    private static final String NAMESPACE_KEY = KEY

    static PrimitiveNamespace createNamespace(){
        return new PrimitiveNamespace(name: getNamespaceKey(), typeDisplayName: TYPE_DISPLAY_NAME)
    }

    static String getNamespaceKey(){
        return NAMESPACE_KEY
    }

    @Override
    void injectPrimitives(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, TemplateBinding binding){
        Class keywordClass = getPrimitiveClass()
        LinkedHashMap aggregatedConfig = config.getConfig()
        aggregatedConfig[KEY].each{ key, value ->
            binding.setVariable(key, keywordClass.newInstance(
                name: key,
                value: value,
                injector: this.getClass()
            ))
        }
    }

}
