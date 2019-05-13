package org.boozallen.plugins.jte.console

import org.boozallen.plugins.jte.Utils
import hudson.Extension
import hudson.MarkupText
import hudson.Util
import hudson.console.ConsoleAnnotationDescriptor
import hudson.console.ConsoleAnnotator
import hudson.console.ConsoleNote
import hudson.model.TaskListener
import org.jenkinsci.plugins.workflow.job.WorkflowRun

enum LogLevel{
    INFO(tag: "info"),
    WARN(tag: "warn"),
    ERROR(tag: "error")
    String tag
}

public class TemplateLogger extends ConsoleNote<WorkflowRun> {

    private static final String CONSOLE_NOTE_PREFIX = "[JTE] "
    String messageID 
    LogLevel logType 
    Boolean firstLine
    Boolean multiLine
    Boolean initiallyHidden

    @Override
    ConsoleAnnotator<?> annotate(WorkflowRun context, MarkupText text, int charPos) {
        def tags = [
            "class='jte-${logType.tag}'",
            "jte-id='${messageID}'",
        ]
        if(multiLine){
            tags.push("first-line='${firstLine}''")
            tags.push("multi-line='${multiLine}'")
        }
        if(initiallyHidden){
            tags.push("initially-hidden='${initiallyHidden}'")
        }
        text.wrapBy("<span ${tags.join(" ")}>", '</span>')
        return null
    }

    static void print(String message, Map config){
        def argTypes = [
                "initiallyHidden": Boolean,
                "logType": LogLevel,
                "trimLines": Boolean
        ]

        Boolean throwError = false
        String errorMsg = "Improper TemplateLogger Configuration: \n"

        def improperConfigNames = config.keySet() - argTypes.keySet()
        if(improperConfigNames){
            throwError = true
            errorMsg +=  "The following keys are not identified: ${improperConfigNames} \n"
            improperConfigNames.each{ config.remove(it) }
        }
        config.each{ key, value ->
            Class expectedType = argTypes[key]
            if(!(value.getClass() == expectedType)){
                throwError = true
                errorMsg += "key ${key} should be of type ${argTypes[key]} \n"
            }
        }

        if (throwError){
            throw new Utils.JTEException(errorMsg)
        }

        // apply defaults if not set.
        config = [ initiallyHidden: false, trimLines: true, logType: LogLevel.INFO ] + config


        // do rest of the things..

        def alphabet = (["a".."z"] + [0..9]).flatten()
        String messageID = (1..10).collect{ alphabet[ new Random().nextInt(alphabet.size()) ] }.join()
        TaskListener listener = Utils.getListener()
        PrintStream logger = listener.getLogger()
        String trimmedMsg = message.trim()
        Boolean firstLine = true
        Boolean multiLine = (trimmedMsg.split("\n").size() > 1)
        trimmedMsg.trim().eachLine{ line ->
            synchronized (logger) {
                if( config.trimLines ) {
                    line = line.trim()
                }
                listener.annotate(new TemplateLogger(
                        logType: config.logType,
                        messageID: messageID,
                        firstLine: firstLine,
                        multiLine: multiLine,
                        initiallyHidden: config.initiallyHidden
                ))
                if (firstLine) firstLine = false
                logger.println(CONSOLE_NOTE_PREFIX + line)
            }
        }


    }

    static void printWarning(String message, Boolean initiallyHidden = false) {
        print(message, [initiallyHidden: initiallyHidden, logType:  LogLevel.WARN])
    }

    static void printError(String message, Boolean initiallyHidden = false ) {
        print(message, [initiallyHidden: initiallyHidden, logType:  LogLevel.ERROR])
    }

    @Extension public static final class DescriptorImpl extends ConsoleAnnotationDescriptor {}

}