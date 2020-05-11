ruleset {
    ruleset('rulesets/basic.xml')

    ruleset('rulesets/braces.xml')

    ruleset('rulesets/concurrency.xml')

    ruleset('rulesets/convention.xml')

    ruleset('rulesets/design.xml') 

    ruleset('rulesets/dry.xml')

    ruleset('rulesets/exceptions.xml')

    ruleset('rulesets/formatting.xml') {
        // enforce at least one space after map entry colon
        SpaceAroundMapEntryColon {
            characterAfterColonRegex = /\s/
            characterBeforeColonRegex = /./
        }
    }

    ruleset('rulesets/generic.xml')

    ruleset('rulesets/groovyism.xml')

    ruleset('rulesets/imports.xml') {

    }

    ruleset('rulesets/logging.xml')

    ruleset('rulesets/naming.xml') {
        // Gradle encourages violations of this rule
        exclude 'ConfusingMethodName'
    }

    ruleset('rulesets/security.xml') {
        // we don't care for the Enterprise Java Bean specification here
        exclude 'JavaIoPackageAccess'
    }

    ruleset('rulesets/serialization.xml')

    ruleset('rulesets/size.xml') {
        NestedBlockDepth {
            maxNestedBlockDepth = 6
        }

        // we have no Cobertura coverage file yet
        exclude 'CrapMetric'
    }

    ruleset('rulesets/unnecessary.xml')

    ruleset('rulesets/unused.xml')
}