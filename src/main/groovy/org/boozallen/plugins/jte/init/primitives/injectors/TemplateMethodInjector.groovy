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
import org.boozallen.plugins.jte.init.primitives.RunAfter
import org.boozallen.plugins.jte.init.primitives.TemplateBinding
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * Loads libraries from the pipeline configuration and injects StepWrapper's into the
 * run's {@link org.boozallen.plugins.jte.init.primitives.TemplateBinding}
 */
@Extension class TemplateMethodInjector extends TemplatePrimitiveInjector {

    private static final String KEY = "template_methods"
    private static final String TYPE_DISPLAY_NAME = "Template Method"
    private static final String NAMESPACE_KEY = KEY

    static PrimitiveNamespace createNamespace(){
        return new CallableNamespace(name: getNamespaceKey(), typeDisplayName: TYPE_DISPLAY_NAME)
    }

    static String getNamespaceKey(){
        return NAMESPACE_KEY
    }

    @SuppressWarnings("ParameterName")
    @Override
    @RunAfter([LibraryStepInjector, DefaultStepInjector])
    void injectPrimitives(FlowExecutionOwner flowOwner, PipelineConfigurationObject config, TemplateBinding binding){
        LinkedHashMap aggregatedConfig = config.getConfig()
        StepWrapperFactory stepFactory = new StepWrapperFactory(flowOwner)
        aggregatedConfig[KEY].each{ step, _ ->
            if(!binding.hasStep(step)){
                binding.setVariable(step, stepFactory.createNullStep(step, binding))
            }
        }
    }

}
