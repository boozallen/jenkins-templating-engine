package org.boozallen.plugins.jte

import org.boozallen.plugins.jte.binding.TemplateBinding
import org.boozallen.plugins.jte.config.PipelineConfig
import org.boozallen.plugins.jte.config.TemplateConfigObject
import org.boozallen.plugins.jte.utils.RunUtils
import org.boozallen.plugins.jte.utils.TemplateScriptEngine
import org.jenkinsci.plugins.workflow.cps.CpsScript
import spock.lang.*
import hudson.model.*
import org.junit.*;
import org.jvnet.hudson.test.*;

class TemplateEntryPointVariableSpec extends Specification {
  @Shared
  @ClassRule
  @SuppressWarnings('JUnitPublicField')
  public GroovyJenkinsRule groovyJenkinsRule = new GroovyJenkinsRule()

  @Shared
  public ClassLoader classLoader;

  TemplateEntryPointVariable templateEntryPointVariable = new TemplateEntryPointVariable()

  def setupSpec(){
    classLoader = groovyJenkinsRule.jenkins.getPluginManager().uberClassLoader
  }

  def "getName returns 'template'"(){
    setup:
    String name = null

    when:
    name = templateEntryPointVariable.getName()

    then:
    name == TemplateEntryPointVariable.NAME
  }

  def "getValue returns the existing value from script.binding" (){
    setup:
    CpsScript script = Mock(CpsScript)
    TemplateBinding binding = Mock(TemplateBinding)
    Script template = Mock(Script)

    2 * script.getBinding() >> { return binding }
    1 * binding.getVariable(TemplateEntryPointVariable.NAME) >> { return template }
    1 * binding.hasVariable(TemplateEntryPointVariable.NAME) >> { return true }

    Object value;
    when:
    value = templateEntryPointVariable.getValue(script)

    then:
    value == template

  }

  def "getValue returns template and sets in binding; all subroutines mocked" (){
    setup:
    CpsScript script = Mock(CpsScript)
    groovy.lang.Binding bindingStart = Mock(groovy.lang.Binding)
    TemplateBinding binding = Mock(TemplateBinding)
    Script template = Mock(Script)

    1 * script.getBinding() >> { return bindingStart }
    0 * bindingStart.getVariable(TemplateEntryPointVariable.NAME)
    1 * bindingStart.hasVariable(TemplateEntryPointVariable.NAME) >> { return false }

    TemplateEntryPointVariable t = templateEntryPointVariable = Spy(TemplateEntryPointVariable)
    1 * t.newTemplateBinding() >> { return binding }
    1 * script.setBinding(binding)

    PipelineConfig pipelineConfig = Mock(PipelineConfig)
    1 * t.newPipelineConfig() >> { return pipelineConfig }
    1 * t.aggregateTemplateConfigurations(_) >> { return }

    TemplateConfigObject templateConfigObject = Mock(TemplateConfigObject)
    2 * pipelineConfig.getConfig() >> { return templateConfigObject }

    LinkedHashMap configMap = [:] as LinkedHashMap
    1 * templateConfigObject.getConfig() >> { return configMap }

    1 * binding.setVariable("pipelineConfig", configMap)
    1 * binding.setVariable("templateConfigObject", templateConfigObject)

    1 * t.initializeBinding( pipelineConfig, script) >> { return }
    GroovySpy(RunUtils, global:true)
    RunUtils.getClassLoader() >> {return classLoader }

    GroovySpy(TemplateScriptEngine, global: true)
    Script parseResult = Mock(Script)

    1 * TemplateScriptEngine.parse(_, binding) >> {return parseResult}
    1 * binding.setVariable(t.getName(), parseResult)

    Object value;
    when:
    value = templateEntryPointVariable.getValue(script)

    then:
    value == parseResult

  }
}
