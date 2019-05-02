package org.boozallen.plugins.jte.console

import org.boozallen.plugins.jte.Utils
import hudson.Extension
import hudson.MarkupText
import hudson.Util
import hudson.console.ConsoleAnnotationDescriptor
import hudson.console.ConsoleAnnotator
import hudson.console.ConsoleNote
import hudson.model.TaskListener
import java.util.logging.Level
import java.util.logging.Logger

import org.jenkinsci.plugins.workflow.job.WorkflowRun

public class TemplateConsoleNote extends ConsoleNote<WorkflowRun> {

    private static final Logger LOGGER = Logger.getLogger(TemplateConsoleNote.class.getName())
    private static final String CONSOLE_NOTE_PREFIX = "[JTE] "

    private TemplateConsoleNote() { }

    @Override
    ConsoleAnnotator<?> annotate(WorkflowRun context, MarkupText text, int charPos) {
        text.addMarkup(0, text.length(), '<span class="jte">', '</span>')
        return null
    }

    static void print(String message) {
        TaskListener listener = Utils.getListener()
        PrintStream logger = listener.getLogger()
        message.eachLine{ line -> 
            synchronized (logger) {
                try {
                    listener.annotate(new TemplateConsoleNote())
                } catch (IOException x) {
                    LOGGER.log(Level.WARNING, null, x)
                }
                logger.println(CONSOLE_NOTE_PREFIX + line)
            }
        }
    }

    static void printCollapsable(){

    }

    static void printError(){

    }

    @Extension public static final class DescriptorImpl extends ConsoleAnnotationDescriptor {}

}