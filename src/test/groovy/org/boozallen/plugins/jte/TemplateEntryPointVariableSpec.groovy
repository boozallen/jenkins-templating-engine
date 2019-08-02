package org.boozallen.plugins.jte

import org.boozallen.plugins.jte.binding.TemplateBinding
import org.boozallen.plugins.jte.config.GovernanceTier
import org.boozallen.plugins.jte.config.PipelineConfig
import org.boozallen.plugins.jte.config.TemplateConfigDsl
import org.boozallen.plugins.jte.config.TemplateConfigObject
import org.boozallen.plugins.jte.utils.FileSystemWrapper
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

  def "aggregateTemplateConfigurations 1 tier, no local config"(){
    setup:
    PipelineConfig pipelineConfigMain =  Mock(PipelineConfig)

    TemplateConfigObject templateConfigObject = Mock(TemplateConfigObject)
    //1 * templateConfigObject.asBoolean() >> { return true }

    1 * pipelineConfigMain.join(templateConfigObject) >> { return }

    GovernanceTier tier = GroovyMock(GovernanceTier, global: true)
    tier.getConfig() >> { return templateConfigObject }

    1 * GovernanceTier.getHierarchy() >> { return [ tier ] }
    1 * GovernanceTier.getCONFIG_FILE() >> { return "pipeline_config.groovy" } // mocks GovernanceTier.CONFIG_FILE

    FileSystemWrapper fsw = GroovyMock(FileSystemWrapper, global: true)
    1 * FileSystemWrapper.createFromJob() >> { return fsw }
    1 * fsw.getFileContents(_, _, _) >> { return null }


    when:
    templateEntryPointVariable.aggregateTemplateConfigurations(pipelineConfigMain)

    then:
    true == true
  }

  def "aggregateTemplateConfigurations #tier_count tier, with local config"(){
    setup:
    PipelineConfig pipelineConfigMain =  Mock(PipelineConfig)

    TemplateConfigObject templateConfigObject = Mock(TemplateConfigObject)
    //1 * templateConfigObject.asBoolean() >> { return true }

    tier_count * pipelineConfigMain.join(templateConfigObject) >> { return }

    def tiers = []

    for( int t = 0; t < tier_count; t++ ) {
      GovernanceTier tier = GroovyMock(GovernanceTier)
      1 * tier.getConfig() >> { return templateConfigObject }
      tiers << tier
    }

    GroovyMock(GovernanceTier, global: true)
    1 * GovernanceTier.getHierarchy() >> { return tiers }
    1 * GovernanceTier.getCONFIG_FILE() >> { return "pipeline_config.groovy" }

    FileSystemWrapper fsw = GroovyMock(FileSystemWrapper, global: true)
    1 * FileSystemWrapper.createFromJob() >> { return fsw }

    TemplateConfigObject appTemplate = Mock(TemplateConfigObject)
    String appTemplateContent = "_"
    1 * fsw.getFileContents("pipeline_config.groovy", _, _) >> { return appTemplateContent }

    GroovyMock(TemplateConfigDsl, global: true)
    1 * TemplateConfigDsl.parse(appTemplateContent) >> { return appTemplate }
    1 * pipelineConfigMain.join(appTemplate) >> { return }


    when:
    templateEntryPointVariable.aggregateTemplateConfigurations(pipelineConfigMain)

    then:
    true == true

    where:
    tier_count << [0, 1, 5]

  }
}
