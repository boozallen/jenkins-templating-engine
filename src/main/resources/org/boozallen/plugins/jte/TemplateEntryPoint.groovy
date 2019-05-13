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


import org.boozallen.plugins.jte.binding.*
import org.boozallen.plugins.jte.config.*
import org.boozallen.plugins.jte.hooks.*
import org.boozallen.plugins.jte.Utils 
import com.cloudbees.groovy.cps.impl.CpsClosure 

def call(CpsClosure body = null){
    // otherwise currentBuild.result defaults to null 
    currentBuild.result = "SUCCESS"

    // TODO: find a cleaner way.. 
    createWorkspaceStash()
    archiveConfig()

    Map context = [
        step: null, 
        library: null, 
        status: currentBuild.result 
    ]
    try{
        Hooks.invoke(Init, getBinding(), context)
        if (body){
            body()
        } else{
            Utils.findAndRunTemplate(pipelineConfig, getBinding()) 
        }
    }catch(any){
        currentBuild.result = "FAILURE" 
        context.status = currentBuild.result 
        throw any 
    }finally{
        Hooks.invoke(CleanUp, getBinding(), context)
        Hooks.invoke(Notify, getBinding(), context)
    }

}

void createWorkspaceStash(){
  try{
      if (scm){
          node{
          	  cleanWs()
              checkout scm 
              stash "workspace" 
          }
      }
  }catch(any){}
}

void archiveConfig(){
    node{
        writeFile text: TemplateConfigDsl.serialize(templateConfigObject), file: "pipeline_config.groovy"
        archiveArtifacts "pipeline_config.groovy"
    }
}



































