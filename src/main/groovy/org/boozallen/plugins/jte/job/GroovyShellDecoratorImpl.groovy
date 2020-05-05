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

package org.boozallen.plugins.jte.job

import org.boozallen.plugins.jte.binding.TemplateBinding
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import hudson.Extension
import org.jenkinsci.plugins.workflow.cps.GroovyShellDecorator
import javax.annotation.CheckForNull
import groovy.lang.GroovyShell
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import java.lang.reflect.Field


@Extension
public class GroovyShellDecoratorImpl extends GroovyShellDecorator {

    @Override
    void configureShell(@CheckForNull CpsFlowExecution context, GroovyShell shell) {
        FlowExecutionOwner owner = context.getOwner()
        WorkflowRun run = owner.run() 
        PipelineDecorator pipelineDecorator = run.getAction(PipelineDecorator)
        if(pipelineDecorator){
            TemplateBinding binding = pipelineDecorator.getBinding()
            Field shellBinding = GroovyShell.class.getDeclaredField("context")
            shellBinding.setAccessible(true)
            shellBinding.set(shell, binding)
        }
    }

}