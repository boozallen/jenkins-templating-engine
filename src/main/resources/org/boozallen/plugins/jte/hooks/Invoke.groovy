import org.boozallen.plugins.jte.hooks.Hooks
import org.boozallen.plugins.jte.binding.TemplateBinding
import org.boozallen.plugins.jte.hooks.AnnotatedMethod
import java.util.ArrayList
import java.lang.annotation.Annotation
    
void call(Class<? extends Annotation> a, TemplateBinding b, Map context){
    List<AnnotatedMethod> discovered = Hooks.discover(a, b)            
    discovered.each{ it.invoke(context) }
}