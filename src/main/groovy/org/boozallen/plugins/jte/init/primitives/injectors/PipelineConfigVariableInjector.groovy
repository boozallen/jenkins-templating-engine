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
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.primitives.PrimitiveNamespace
import org.boozallen.plugins.jte.init.primitives.TemplateBinding
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitive
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * injects the aggregated pipeline configuration as a variable called pipelineConfig into the
 * run's {@link org.boozallen.plugins.jte.init.primitives.TemplateBinding}
 */
@Extension class PipelineConfigVariableInjector extends TemplatePrimitiveInjector {

    static final String VARIABLE = "pipelineConfig"
    private static final String TYPE_DISPLAY_NAME = "Pipeline Config"
    private static final String NAMESPACE_KEY = VARIABLE

    static PrimitiveNamespace createNamespace(){
        return new Namespace(name: getNamespaceKey(), typeDisplayName: TYPE_DISPLAY_NAME)
    }

    static String getNamespaceKey(){
        return NAMESPACE_KEY
    }

    @SuppressWarnings('NoDef')
    @Override
    void injectPrimitives(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, TemplateBinding binding){
        Class keywordClass = KeywordInjector.getPrimitiveClass()

        // add the pipelineConfig to the binding
        def pipelineConfig = keywordClass.newInstance(
            name: VARIABLE,
            value: config.getConfig(),
            injector: this.getClass(),
            preLockException: "Variable ${VARIABLE} reserved for accessing the aggregated pipeline configuration",
            postLockException: "Variable ${VARIABLE} reserved for accessing the aggregated pipeline configuration"
        )

        binding.setVariable(VARIABLE, pipelineConfig)
    }

    static class Namespace extends PrimitiveNamespace {
        String name = VARIABLE
        LinkedHashMap pipelineConfig
        @Override void add(TemplatePrimitive primitive){
            pipelineConfig = primitive.getValue()
        }

        @Override
        Set<String> getVariables(){
            return [ VARIABLE ]
        }

        @Override
        void printAllPrimitives(TemplateLogger logger) {
            // pipelineConfig is not communicated to users as a primitive
        }

        Object getProperty(String name){
            return pipelineConfig[name]
        }
    }

}
