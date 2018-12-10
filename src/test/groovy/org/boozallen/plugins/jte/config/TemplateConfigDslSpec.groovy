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

package org.boozallen.plugins.jte.config

import spock.lang.*

import org.boozallen.plugins.jte.config.TemplateConfigObject
import org.boozallen.plugins.jte.config.TemplateConfigDsl

class TemplateConfigDslSpec extends Specification {

    def 'Empty Config File'(){
        when: 
            String config = "" 
            TemplateConfigObject configObject = TemplateConfigDsl.parse(config)

        then: 
            configObject.config == [:]
            configObject.merge.isEmpty()
            configObject.override.isEmpty()
    }

    def 'Flat Keys Configuration'(){
        when: 
            String config = """
            a = 3
            b = "hi" 
            c = true 
            """
            TemplateConfigObject configObject = TemplateConfigDsl.parse(config)
        then: 
            configObject.config == [
                a: 3, 
                b: "hi", 
                c: true
            ]
    }

    def 'Nested Keys Configuration'(){
        when: 
            String config = """
            random = "hi" 
            application_environments{
                dev{
                    field = true 
                }
                test{
                    field = false 
                }
            } 
            blah{
                another{
                    field = "hey" 
                }
            }
            """
            TemplateConfigObject configObject = TemplateConfigDsl.parse(config)
        then: 
            configObject.config == [
                random: "hi",
                application_environments: [
                    dev: [
                        field: true
                    ],
                    test: [
                        field: false 
                    ]
                ],
                blah: [
                    another: [
                        field: "hey" 
                    ]
                ]
            ]
    }

    def 'One Merge First Key'(){
        when: 
            String config = """
            application_environments{
                merge = true
            } 
            """
            TemplateConfigObject configObject = TemplateConfigDsl.parse(config)
        then: 
            configObject.merge == [ "application_environments" ] as Set
    }

    def 'One Merge Nested Key'(){
        when: 
            String config = """
            application_environments{
                dev{
                    merge = true
                }
            } 
            """
            TemplateConfigObject configObject = TemplateConfigDsl.parse(config)
        then: 
            configObject.merge == [ "application_environments.dev" ] as Set
    }

    def 'Multi-Merge'(){
        when: 
            String config = """
            application_environments{
                dev{
                    merge = true
                }
                test{
                    merge = true 
                }
            } 
            """
            TemplateConfigObject configObject = TemplateConfigDsl.parse(config)
        then: 
            configObject.merge == [ "application_environments.dev", "application_environments.test" ] as Set 
    }

    def 'One Override First Key'(){
        when: 
            String config = """
            application_environments{
                override = true
            } 
            """
            TemplateConfigObject configObject = TemplateConfigDsl.parse(config)
        then: 
            configObject.override == [ "application_environments" ] as Set 
    }

    def 'One Override Nested Key'(){
        when: 
            String config = """
            application_environments{
                dev{
                    override = true
                }
            } 
            """
            TemplateConfigObject configObject = TemplateConfigDsl.parse(config)
        then: 
            configObject.override == [ "application_environments.dev" ] as Set
    }

    def 'Multi-Override'(){
        when: 
            String config = """
            application_environments{
                dev{
                    override = true
                }
                test{
                    override = true 
                }
            } 
            """
            TemplateConfigObject configObject = TemplateConfigDsl.parse(config)
        then: 
            configObject.override == [ "application_environments.dev", "application_environments.test" ] as Set 
    }

    def 'File Access Throws Security Exception'(){
        when: 
            String config = """
            password = new File("/etc/passwd").text 
            """
            TemplateConfigObject configObject = TemplateConfigDsl.parse(config)
        then: 
            thrown(SecurityException)
    }

}