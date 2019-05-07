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
    static final Integer ID = 0 
    Integer messageID 
    LogLevel logType 
    Boolean firstLine
    Boolean multiLine

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
        text.wrapBy("<span ${tags.join(" ")}>", '</span>')
        return null
    }

    static void print(LogLevel logType = LogLevel.INFO, String message) {
        Integer messageID = ID++ 
        TaskListener listener = Utils.getListener()
        PrintStream logger = listener.getLogger()
        String trimmedMsg = message.trim() 
        Boolean firstLine = true 
        Boolean multiLine = (trimmedMsg.split("\n").size() > 1)
        trimmedMsg.trim().eachLine{ line -> 
            synchronized (logger) {
                line = line.trim()
                listener.annotate(new TemplateLogger(
                    logType: logType, 
                    messageID: messageID, 
                    firstLine: firstLine, 
                    multiLine: multiLine
                ))
                if (firstLine) firstLine=false
                logger.println(CONSOLE_NOTE_PREFIX + line)
            }
        }
    }

    static void printWarning(String message) {
        print(LogLevel.WARN, message)
    }

    static void printError(String message) {
        print(LogLevel.ERROR, message)
    }

    @Extension public static final class DescriptorImpl extends ConsoleAnnotationDescriptor {}

}