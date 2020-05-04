package org.boozallen.plugins.jte.console

import org.boozallen.plugins.jte.utils.JTEException
import org.boozallen.plugins.jte.utils.RunUtils
import hudson.Extension
import hudson.MarkupText
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

public class TemplateLogger {

    private static final String CONSOLE_NOTE_PREFIX = "[JTE] "
    TaskListener listener
    PrintStream logger

    TemplateLogger(TaskListener listener){
        this.listener = listener
        this.logger = listener.getLogger()
    }

    void print(String message, LogLevel logType = LogLevel.INFO){
        message = message.trim()
        Boolean isMultiline = message.contains("\n")
        String messageId = generateMessageId()
        message.eachLine{ line, i ->
            synchronized (logger) {
                listener.annotate(new Annotator(
                    logType: logType,
                    messageId: messageId,
                    firstLine: !i,
                    multiLine: isMultiline
                ))
                logger.println(CONSOLE_NOTE_PREFIX + line)
            }
        }
    }

    void printWarning(String message) {
        print(message, LogLevel.WARN)
    }

    void printError(String message) {
        print(message, LogLevel.ERROR)
    }

    private String generateMessageId(){
        def alphabet = (["a".."z"] + [0..9]).flatten()
        String messageId = (1..10).collect{ alphabet[ new Random().nextInt(alphabet.size()) ] }.join()
        return messageId
    }

    static class Annotator extends ConsoleNote<WorkflowRun>{
        String messageId
        LogLevel logType
        Boolean firstLine
        Boolean multiLine
        /*
            TODO: 
            * see if we're actually using this configuration option anywhere
            * remove it if we arent
        */
        Boolean initiallyHidden = true 

        @Override
        ConsoleAnnotator<?> annotate(WorkflowRun context, MarkupText text, int charPos) {
            def tags = [
                "class='jte-${logType.tag}'",
                "jte-id='${messageId}'",
            ]
            if(multiLine){
                tags << "first-line='${firstLine}''"
                tags << "multi-line='${multiLine}'"
            }
            if(initiallyHidden){
                tags << "initially-hidden='${initiallyHidden}'"
            }
            text.wrapBy("<span ${tags.join(" ")}>", '</span>')
            return null
        }

        @Extension public static final class DescriptorImpl extends ConsoleAnnotationDescriptor {}
    }
}