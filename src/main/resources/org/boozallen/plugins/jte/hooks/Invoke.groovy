import org.boozallen.plugins.jte.hooks.Hooks
import org.boozallen.plugins.jte.binding.TemplateBinding
import org.boozallen.plugins.jte.hooks.AnnotatedMethod
import java.lang.annotation.Annotation

void call(Class<? extends Annotation> a, TemplateBinding b, Map context){
    List<AnnotatedMethod> discovered = Hooks.discover(a, b)
    List exceptions = []
    discovered.each{
        try {
            it.invoke(context)
        }catch( Exception e){
            exceptions << e.message
        }
    }

    if( exceptions ){
        throw new Exception("@${a.name}: " + exceptions.join(";\n"))
    }
}