package org.boozallen.plugins.jte.binding

import hudson.scm.SCM
import jenkins.plugins.git.GitSampleRepoRule
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.ClassRule
import org.jvnet.hudson.test.GroovyJenkinsRule
import spock.lang.Shared
import spock.lang.Specification

class LibraryLoaderSpec extends Specification {
    @ClassRule
    @SuppressWarnings('JUnitPublicField')
    public GroovyJenkinsRule groovyJenkinsRule = new GroovyJenkinsRule()

    @Shared
    @ClassRule GitSampleRepoRule sampleRepo = new GitSampleRepoRule()

    @Shared
    SCM scm = null

}
