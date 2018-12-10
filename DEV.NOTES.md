## Introduction
This document is a running (as of 2018/Oct/16) note regarding different development aspects around/about SDP. For this document SDP and JTE are used interchangibly

## Considerations/TODO/Backlog
Consider renaming SDP to Jenkins Template Engine (JTE)
Consider how to connect steps such as how a 'Twistlock scan' step would get the containers after a 'build docker' step
* Synchronous Event listeners and Async Queue listeners as options
* Annotations for each step type (e.g 'Build', 'scan')
* Library developers will probably implement the entire pipeline
* (TODO) ask Steven what versions of : 
* * Java: 1.8 but more dependent of groovy version
* * Groovy: 2.5.2
* * Jenkins: working against the versions for https://jenkins.io/changelog-stable/ **not** https://jenkins.io/changelog/
* * we are using Groovy and most of the newer java changes are redundant with existing Groovy features e.g. Optional in java are already handled by the groovy ?.

## Business Notes
Primary Target user is the technical person in charge of Jenkins, more than likely a 'Platform Engineer' or 'DevOps engineer'
Business Case:
1. 'As-Is' Developer: Take the existing libraries and configure to organizations desire
2. Pipeline Author: people who create pipelines
3. Library Developer: people who develop new tool integrations/libraries
4. JTE/'Framework Core' Developer: Understanding the core framework

## Core Platform Development Notes
Jenkins Development: 
GlobalVariable is like a singleton and initialized on its first call.  
Jenkins' GlobalVariable represents 
1. a global function or 
2. a ‘namespace’/package for functions/methods. 

SDP/JTE overrides the script binding class and instance (with TemplateBinding);  
this SDP bind instance restricts/controls the usage of TemplatePrimitive sub-classes (TemplatePrimitive is a Java Abstract base class) i.e., jenkins configuration data that are managed by the JTE

## Platform Documentation 
Text/documentation is stored in .rst (reStructured Text) formatted files
currently files are under https://github.com/boozallen/sdp-pipeline-framework/blob/master/docs

## Validation Notes
Consider having libraries and pipeline authors implement validation objects;  Thinking these validators will check against final config (aggregated pipeline config and libraries)

Syntax/Key checking: are there keys that are unused by libraries.  
Validity: libraries check to see if their keys exists in the final config.  
Pipeline: where library and syntax keys are fine but key values are not as expected e.g., the openshift urls must have hostname = boozallencsn.com

Solutions being considered: return a data structure of regular expression and types;  allow pipeline authors and library devs to implement callbacks against validation step:  
Currently thinking validator.validate(Config config, String[] currentRemainingUsedKeys) (String[] remainingUnusedKeys, Issues[] issues);  Each step should remove the keys it uses from the currentRemainingUsedKeys;  and the validation chain should capture all 'Issues' where issues may be Warning (no exception just logging) or Errors/Failures/Exceptions which should stop the pipeline.

## Onboarding
import the project via gradle **not** via maven dependencies.
use the data from the included maven onboard/settings.xml to import the dependencies via gradle.  Copied that settings.xml to ~/.m2/settings.xml (backup your existing file if necessary).  
above step is only needed once ^^^
call `gradle clean jpi` to build project

