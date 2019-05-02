import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.codehaus.groovy.runtime.InvokerHelper

def call(CpsScript script, ArrayList<String> steps){
    for(def i = 0; i < steps.size(); i++){
        String step = steps.get(i)
        InvokerHelper.getMetaClass(script).invokeMethod(script, step, null)
    }
}