package org.boozallen.plugins.jte.binding

import org.boozallen.plugins.jte.binding.injectors.LibraryLoader
import org.boozallen.plugins.jte.utils.TemplateScriptEngine
import spock.lang.*
import org.boozallen.plugins.jte.binding.injectors.StepWrapper
import org.boozallen.plugins.jte.config.GovernanceTier
import org.boozallen.plugins.jte.config.TemplateConfigException
import org.boozallen.plugins.jte.config.TemplateConfigObject
import org.boozallen.plugins.jte.config.libraries.LibraryProvider
import org.boozallen.plugins.jte.config.libraries.LibraryConfiguration
import org.boozallen.plugins.jte.console.TemplateLogger
import org.boozallen.plugins.jte.utils.RunUtils
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.WithoutJenkins

class LibraryLoaderSpec extends Specification {
    
    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()
    CpsScript script = Mock()
    PrintStream logger = Mock()

    def setup(){

        GroovySpy(TemplateLogger.class, global:true)
        TemplateLogger.print(_,_) >> {return }

        GroovySpy(RunUtils, global:true)
        _ * RunUtils.getJob() >> jenkins.createProject(WorkflowJob)
        _ * RunUtils.getLogger() >> logger

        GroovySpy(TemplateLogger, global:true)
        _ * TemplateLogger.printError(_) >>{ return }
    }

    class MockLibraryProvider extends LibraryProvider{
        Boolean hasLibrary(String libName){
            println "should always be mocked" 
        }
        List loadLibrary(CpsScript script, String libName, Map libConfig){
            println "should always be mocked"
        }
    }
    
    @WithoutJenkins
    def "when library source has library, loadLibrary is called"(){
        setup: 
            MockLibraryProvider s = Mock{
                hasLibrary("test_library") >> true
            }
            LibraryConfiguration c = Mock{
                getLibraryProvider() >> s
            }
            GovernanceTier tier = GroovyMock(global: true){
                getLibraries() >> [ c ] 
            }
            GovernanceTier.getHierarchy() >>  [ tier ]

            // mock libraries to load 
            TemplateConfigObject config = new TemplateConfigObject(config: [
                libraries: [
                    test_library: [:]
                ]
            ])

            GroovySpy(TemplateLogger.class, global:true)
            TemplateLogger.print(_,_) >> {return }

        when: 
            LibraryLoader.doInject(config, script)
        then: 
            1 * s.loadLibrary(script, "test_library", [:])
    }

    @WithoutJenkins
    def "Libraries can be loaded across library sources in a governance tier"(){
        setup: 
            MockLibraryProvider s1 = Mock{
                hasLibrary("libA") >> true 
            }
            MockLibraryProvider s2 = Mock{
                hasLibrary("libB") >> true
            }

            LibraryConfiguration c1 = Mock{
                getLibraryProvider() >> s1
            }

            LibraryConfiguration c2 = Mock{
                getLibraryProvider() >> s2
            }
            
            GovernanceTier tier = GroovyMock(global: true){
                getLibraries() >> [ c1, c2 ]
            }
            GovernanceTier.getHierarchy() >> [ tier ]

            // mock libraries to load 
            TemplateConfigObject config = new TemplateConfigObject(config: [
                libraries: [
                    libA: [:],
                    libB: [:]
                ]
            ])

        when: 
            LibraryLoader.doInject(config, script)
        then: 
            1 * s1.loadLibrary(script, "libA", [:])
            0 * s1.loadLibrary(script, "libB", [:])
            1 * s2.loadLibrary(script, "libB", [:])
            0 * s2.loadLibrary(script, "libA", [:])
    }

    @WithoutJenkins
    def "Libraries can be loaded across library sources in different governance tiers"(){
        setup: 
            MockLibraryProvider s1 = Mock{
                hasLibrary("libA") >> true 
            }
            MockLibraryProvider s2 = Mock{
                hasLibrary("libB") >> true 
            }

            LibraryConfiguration c1 = Mock{
                getLibraryProvider() >> s1
            }

            LibraryConfiguration c2 = Mock{
                getLibraryProvider() >> s2
            }
            
            GovernanceTier tier1 = Mock{
                getLibraries() >> [ c1 ]
            }
            GovernanceTier tier2 = GroovyMock(global:true){
                getLibraries() >> [ c2 ]
            }
            GovernanceTier.getHierarchy() >> [ tier1, tier2 ]

            // mock libraries to load 
            TemplateConfigObject config = new TemplateConfigObject(config: [
                libraries: [
                    libA: [:],
                    libB: [:]
                ]
            ])
        when: 
            LibraryLoader.doInject(config, script)
        then: 
            1 * s1.loadLibrary(script, "libA", [:])
            0 * s1.loadLibrary(script, "libB", [:])
            1 * s2.loadLibrary(script, "libB", [:])
            0 * s2.loadLibrary(script, "libA", [:])
    }

    @WithoutJenkins
    def "library on more granular governance tier gets loaded"(){
        setup: 
             MockLibraryProvider s1 = Mock{
                hasLibrary("libA") >> true 
            }
            MockLibraryProvider s2 = Mock{
                hasLibrary("libA") >> true 
            }

            LibraryConfiguration c1 = Mock{
                getLibraryProvider() >> s1 
            }
            LibraryConfiguration c2 = Mock{
                getLibraryProvider() >> s2 
            }

            GovernanceTier tier1 = Mock{
                getLibraries() >> [ c1 ]
            }
            GovernanceTier tier2 = GroovyMock(global:true){
                getLibraries() >> [ c2 ]
            }
            GovernanceTier.getHierarchy() >> [ tier1, tier2 ]

            // mock libraries to load 
            TemplateConfigObject config = new TemplateConfigObject(config: [
                libraries: [
                    libA: [:]
                ]
            ])

        when: 
            LibraryLoader.doInject(config, script)
        then: 
            1 * s1.loadLibrary(script, "libA", [:])
            0 * s2.loadLibrary(script, "libA", [:])        
    }

    @WithoutJenkins
    def "library loader correctly passes step config"(){
        setup: 
            MockLibraryProvider s = Mock{
                hasLibrary("libA") >> true 
                hasLibrary("libB") >> true 
            }

            LibraryConfiguration c = Mock{
                getLibraryProvider() >> s 
            }

            GovernanceTier tier = GroovyMock(global:true){
                getLibraries() >> [ c ]
            }
            GovernanceTier.getHierarchy() >> [ tier ]

            // mock libraries to load 
            TemplateConfigObject config = new TemplateConfigObject(config: [
                libraries: [
                    libA: [
                        fieldA: "A" 
                    ],
                    libB: [
                        fieldB: "B"
                    ]
                ]
            ])

        when: 
            LibraryLoader.doInject(config, script)
        then: 
            1 * s.loadLibrary(script, "libA", [fieldA: "A"])
            1 * s.loadLibrary(script, "libB", [fieldB: "B"])
    }

    @WithoutJenkins
    def "steps configured via configuration file get loaded as default step implementation"(){
        setup:
            TemplateBinding binding = Mock()
            script.getBinding() >> binding 
            def s = GroovyMock(StepWrapper, global: true)
            s.createDefaultStep(script, "test_step", [:]) >> s

            GroovySpy(LibraryLoader.class, global: true)
            LibraryLoader.getPrimitiveClass() >> { return s }

            GroovyMock(TemplateLogger, global:true)
            1 * TemplateLogger.print("Creating step test_step from the default step implementation." )

            TemplateConfigObject config = new TemplateConfigObject(config: [
                steps: [
                    test_step: [:]
                ]
            ])
        when: 
            LibraryLoader.doInject(config, script)
            LibraryLoader.doPostInject(config, script) 
        then: 
            1 * binding.setVariable("test_step", s)
    }

    @WithoutJenkins
    def "warning issued when configured steps conflict with loaded library"(){
        setup:
            TemplateBinding binding = Mock{
                hasStep("test_step") >> true 
                getStep("test_step") >> new StepWrapper(library: "libA")
            }
            script.getBinding() >> binding

            GroovyMock(TemplateLogger, global:true)
            1 * TemplateLogger.printWarning("""Configured step test_step ignored.
                                               -- Loaded by the libA Library.""" )

            TemplateConfigObject config = new TemplateConfigObject(config: [
                steps: [
                    test_step: [:]
                ]
            ])
        when: 
            LibraryLoader.doInject(config, script)
            LibraryLoader.doPostInject(config, script) 
        then: 
            0 * binding.setVariable(_ , _)
    }

    @WithoutJenkins
    def "template methods not implemented are Null Step"(){
        setup:
            Set<String> registry = new ArrayList() 
            TemplateBinding binding = Mock(){
                setVariable(_, _) >> { args ->
                    registry << args[0]
                }
                hasStep(_) >> { String stepName -> 
                    stepName in registry
                }
                getProperty("registry") >> registry 
            }
            script.getBinding() >> binding 
            def s = GroovyMock(Object)
            def s2 = GroovyMock(Object)
            s.createDefaultStep(script, "test_step1", _) >> s
            s.createNullStep("test_step2", script) >> s2

            GroovySpy(LibraryLoader.class, global: true)
            LibraryLoader.getPrimitiveClass() >> { return s }

            GroovyMock(TemplateLogger, global:true)
            1 * TemplateLogger.print(_)


            TemplateConfigObject config = new TemplateConfigObject(config: [
                steps: [
                    test_step1: [:]
                ],
                template_methods: [
                    test_step1: [:],
                    test_step2: [:]
                ]
            ])
        when: 
            LibraryLoader.doInject(config, script)
            LibraryLoader.doPostInject(config, script) 
        then:
            1 * s.createDefaultStep(script, "test_step1", [:])
            1 * s.createNullStep("test_step2", script)
            0 * s.createNullStep("test_step1", script)
    }

    @WithoutJenkins
    def "Missing library throws exception"(){
      // now, when a library isn't found, we push a message onto the `libConfigErrors` array
      // and throw the exception later after validating all the libraries.
      // so this test represents making sure that an exception is thrown if a library does not exist.
        setup:
        MockLibraryProvider s = Mock{
            hasLibrary("libA") >> true
            hasLibrary("libB") >> false
        }

        LibraryConfiguration c = Mock{
            getLibraryProvider() >> s
        }

        GovernanceTier tier = GroovyMock(global:true){
            getLibraries() >> [ c ]
        }

        GovernanceTier.getHierarchy() >> [ tier ]

        // mock libraries to load
        TemplateConfigObject config = new TemplateConfigObject(config: [
                libraries: [
                        libA: [
                                fieldA: "A"
                        ],
                        libB: [
                                fieldB: "B"
                        ]
                ]
        ])

        when:
        LibraryLoader.doInject(config, script)
        then:
        thrown(TemplateConfigException)
        1 * s.loadLibrary(script, "libA", [fieldA: "A"])
        0 * s.loadLibrary(script, "libB", [fieldB: "B"])

        1 * TemplateLogger.printError("Library libB Not Found.") >> { return }
        4 * TemplateLogger.printError(_) >>{ return }
    }


    @WithoutJenkins
    def "single library configuration errors gets TemplateLogger'd and throws exception"(){

        setup:
        String err = "Field 'fieldB' is not used."
        MockLibraryProvider s1 = Mock{
            hasLibrary("libA") >> true
            hasLibrary("libB") >> false
        }
        MockLibraryProvider s2 = Mock{
            hasLibrary("libA") >> false
            hasLibrary("libB") >> true
        }

        LibraryConfiguration c1 = Mock{
            getLibraryProvider() >> s1 
        }
        LibraryConfiguration c2 = Mock{
            getLibraryProvider() >> s2 
        }

        GovernanceTier tier = GroovyMock(global:true){
            getLibraries() >> [ c1, c2 ]
        }

        GovernanceTier.getHierarchy() >> [ tier ]

        // mock libraries to load
        TemplateConfigObject config = new TemplateConfigObject(config: [
                libraries: [
                        libA: [
                                fieldA: "A"
                        ],
                        libB: [
                                fieldB: "B"
                        ]
                ]
        ])

        when:
        LibraryLoader.doInject(config, script)
        then:
        thrown(TemplateConfigException)
        1 * s1.loadLibrary(script, "libA", [fieldA: "A"])
        0 * s1.loadLibrary(script, "libB", [fieldB: "B"])

        0 * s2.loadLibrary(script, "libA", [fieldA: "A"])
        1 * s2.loadLibrary(script, "libB", [fieldB: "B"]) >> { return [err]  }

        1 * TemplateLogger.printError(err) >> { return }
        4 * TemplateLogger.printError(_) >>{ return }

    }

    @WithoutJenkins
    def "multiple library configuration errors gets TemplateLogger'd and throws exception"(){
        setup:
        String err = "Field 'fieldA' is not used."
        String err2 = "Field 'fieldB' is not used."
        MockLibraryProvider s1 = Mock{
            hasLibrary("libA") >> true
            hasLibrary("libB") >> false
        }
        MockLibraryProvider s2 = Mock{
            hasLibrary("libA") >> false
            hasLibrary("libB") >> true
        }

        LibraryConfiguration c1 = Mock{
            getLibraryProvider() >> s1 
        }
        LibraryConfiguration c2 = Mock{
            getLibraryProvider() >> s2 
        }

        GovernanceTier tier = GroovyMock(global:true){
            getLibraries() >> [ c1, c2 ]
        }

        GovernanceTier.getHierarchy() >> [ tier ]

        // mock libraries to load
        TemplateConfigObject config = new TemplateConfigObject(config: [
                libraries: [
                        libA: [
                                fieldA: "A"
                        ],
                        libB: [
                                fieldB: "B"
                        ]
                ]
        ])

        when:
        LibraryLoader.doInject(config, script)
        then:
        thrown(TemplateConfigException)
        1 * s1.loadLibrary(script, "libA", [fieldA: "A"]) >> { return [err]  }
        0 * s1.loadLibrary(script, "libB", [fieldB: "B"])

        0 * s2.loadLibrary(script, "libA", [fieldA: "A"])
        1 * s2.loadLibrary(script, "libB", [fieldB: "B"]) >> { return [err2]  }

        1 * TemplateLogger.printError(err) >> { return }
        1 * TemplateLogger.printError(err2) >> { return }
        4 * TemplateLogger.printError(_) >>{ return }
    }


}
