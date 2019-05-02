import org.boozallen.plugins.jte.config.*
import org.boozallen.plugins.jte.hooks.Hooks
import org.boozallen.plugins.jte.hooks.BeforeStep
import org.boozallen.plugins.jte.hooks.AfterStep
import org.boozallen.plugins.jte.hooks.Notify
import org.boozallen.plugins.jte.Utils
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.InvokerInvocationException

def call(String name, String library, CpsScript script, Object impl, String methodName, Object... args){
    def result
    def context = [
        step: name, 
        library: library,
        status: script.currentBuild.result
    ]
    try{
        Hooks.invoke(BeforeStep, script.getBinding(), context)
        Utils.getLogger().println "[JTE][Step - ${library}/${name}.${methodName}(${args.collect{ it.getClass().simpleName }.join(", ")})]" 
        result = InvokerHelper.getMetaClass(impl).invokeMethod(impl, methodName, args)
    } catch (Exception x) {
        script.currentBuild.result = "Failure"
        throw new InvokerInvocationException(x)
    } finally{
        context.status = script.currentBuild.result
        Hooks.invoke(AfterStep, script.getBinding(), context)
        Hooks.invoke(Notify, script.getBinding(), context)
    }
    return result 
}