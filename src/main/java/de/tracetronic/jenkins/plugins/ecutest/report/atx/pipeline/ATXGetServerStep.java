/*
 * Copyright (c) 2015-2019 TraceTronic GmbH
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package de.tracetronic.jenkins.plugins.ecutest.report.atx.pipeline;

import de.tracetronic.jenkins.plugins.ecutest.report.atx.installation.ATXInstallation;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Collections;
import java.util.Set;

/**
 * Advanced pipeline step that returns a pre-configured {@link ATXServer} instance by name.
 *
 * @author Christian Pönisch <christian.poenisch@tracetronic.de>
 */
public class ATXGetServerStep extends Step {

    private final String atxName;

    /**
     * Instantiates a new {@link ATXGetServerStep}.
     *
     * @param atxName the ATX name
     */
    @DataBoundConstructor
    public ATXGetServerStep(final String atxName) {
        super();
        this.atxName = atxName;
    }

    /**
     * @return the ATX name
     */
    public String getAtxName() {
        return atxName;
    }

    @Override
    public StepExecution start(final StepContext context) throws Exception {
        return new Execution(this, context);
    }

    /**
     * Synchronous pipeline step execution that returns a pre-configured {@link ATXServer} instance by name.
     */
    private static class Execution extends SynchronousStepExecution<ATXServer> {

        private static final long serialVersionUID = 1L;

        private final transient ATXGetServerStep step;

        /**
         * Instantiates a new {@link Execution}.
         *
         * @param step    the step
         * @param context the context
         */
        Execution(final ATXGetServerStep step, final StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected ATXServer run() throws Exception {
            return new ATXServer(ATXInstallation.get(step.atxName));
        }
    }

    /**
     * DescriptorImpl for {@link ATXGetServerStep}.
     */
    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "getATXServer";
        }

        @Override
        public String getDisplayName() {
            return "Get TEST-GUIDE installation by name";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.emptySet();
        }
    }
}
