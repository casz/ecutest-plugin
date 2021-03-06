/*
 * Copyright (c) 2015-2019 TraceTronic GmbH
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package de.tracetronic.jenkins.plugins.ecutest.test.client;

import de.tracetronic.jenkins.plugins.ecutest.log.TTConsoleLogger;
import de.tracetronic.jenkins.plugins.ecutest.test.config.ExecutionConfig;
import de.tracetronic.jenkins.plugins.ecutest.test.config.GlobalConstant;
import de.tracetronic.jenkins.plugins.ecutest.test.config.TestConfig;
import de.tracetronic.jenkins.plugins.ecutest.wrapper.com.ETComClient;
import de.tracetronic.jenkins.plugins.ecutest.wrapper.com.ETComException;
import de.tracetronic.jenkins.plugins.ecutest.wrapper.com.ETComProperty;
import de.tracetronic.jenkins.plugins.ecutest.wrapper.com.TestConfiguration;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Common base class for {@link PackageClient} and {@link ProjectClient}.
 *
 * @author Christian Pönisch <christian.poenisch@tracetronic.de>
 */
public abstract class AbstractTestClient implements TestClient {

    private final String testFile;
    private final TestConfig testConfig;
    private final ExecutionConfig executionConfig;
    private String testName;
    private String testDescription;
    private String testReportDir;
    private String testResult;
    private boolean isAborted;

    /**
     * Instantiates a new {@link AbstractTestClient}.
     *
     * @param testFile        the test file
     * @param testConfig      the test configuration
     * @param executionConfig the execution configuration
     */
    public AbstractTestClient(final String testFile, final TestConfig testConfig,
                              final ExecutionConfig executionConfig) {
        this.testFile = StringUtils.trimToEmpty(testFile);
        this.testConfig = testConfig;
        this.executionConfig = executionConfig;
        testName = "";
        testDescription = "";
        testReportDir = "";
        testResult = "";
        isAborted = false;
    }

    /**
     * @return the test file path
     */
    public String getTestFile() {
        return testFile;
    }

    /**
     * @return the test configuration
     */
    public TestConfig getTestConfig() {
        return testConfig;
    }

    /**
     * @return the execution configuration
     */
    public ExecutionConfig getExecutionConfig() {
        return executionConfig;
    }

    /**
     * @return the test name
     */
    public String getTestName() {
        return testName;
    }

    /**
     * @param testName the test name to set
     */
    public void setTestName(final String testName) {
        this.testName = testName;
    }

    /**
     * @return the test description
     */
    public String getTestDescription() {
        return testDescription;
    }

    /**
     * @param testDescription the test description to set
     */
    public void setTestDescription(final String testDescription) {
        this.testDescription = testDescription;
    }

    /**
     * @return the test report directory
     */
    public String getTestReportDir() {
        return testReportDir;
    }

    /**
     * @param testReportDir the test report directory to set
     */
    public void setTestReportDir(final String testReportDir) {
        this.testReportDir = testReportDir;
    }

    /**
     * @return the test result
     */
    public String getTestResult() {
        return testResult;
    }

    /**
     * @param testResult the test result to set
     */
    public void setTestResult(final String testResult) {
        this.testResult = testResult;
    }

    /**
     * @return specifies whether the test execution is aborted
     */
    public boolean isAborted() {
        return isAborted;
    }

    /**
     * @param isAborted the test abort status
     */
    public void setAborted(final boolean isAborted) {
        this.isAborted = isAborted;
    }

    /**
     * {@link Callable} providing remote access to load configurations via COM.
     */
    protected static final class LoadConfigCallable extends MasterToSlaveCallable<Boolean, IOException> {

        private static final long serialVersionUID = 1L;

        private final TestConfig testConfig;
        private final TaskListener listener;

        /**
         * Instantiates a new {@link LoadConfigCallable}.
         *
         * @param testConfig the test configuration
         * @param listener   the listener
         */
        public LoadConfigCallable(final TestConfig testConfig, final TaskListener listener) {
            this.testConfig = testConfig;
            this.listener = listener;
        }

        @Override
        public Boolean call() throws IOException {
            final String tbcFile = testConfig.getTbcFile();
            final String tcfFile = testConfig.getTcfFile();
            final List<GlobalConstant> constants = testConfig.getConstants();
            final TTConsoleLogger logger = new TTConsoleLogger(listener);
            boolean isLoaded = false;

            final String progId = ETComProperty.getInstance().getProgId();
            try (ETComClient comClient = new ETComClient(progId)) {
                final String tbcName = getConfigName(tbcFile);
                final String tcfName = getConfigName(tcfFile);
                logger.logInfo(String.format("- Loading configurations: TBC=%s TCF=%s", tbcName, tcfName));
                if (testConfig.isForceReload()) {
                    logger.logInfo("-> Forcing reload configurations...");
                    comClient.stop();
                }
                if (comClient.openTestConfiguration(StringUtils.defaultIfBlank(tcfFile, null))) {
                    if (tcfFile != null && !constants.isEmpty()) {
                        final Map<String, String> constantMap = getGlobalConstantMap();
                        logger.logInfo("-> With global constants: " + constantMap.toString());
                        setGlobalConstants(comClient, constantMap);
                    }
                    logger.logInfo("-> Test configuration loaded successfully.");
                } else {
                    logger.logError(String.format("-> Loading TCF=%s failed!", tcfName));
                }
                if (comClient.openTestbenchConfiguration(StringUtils.defaultIfBlank(tbcFile, null))) {
                    logger.logInfo("-> Test bench configuration loaded successfully.");
                    isLoaded = true;
                } else {
                    logger.logError(String.format("-> Loading TBC=%s failed!", tbcName));
                }
                if (isLoaded) {
                    if (testConfig.isLoadOnly()) {
                        logger.logInfo("-> Starting configurations will be skipped.");
                    } else {
                        logger.logInfo("- Starting configurations...");
                        comClient.start();
                        logger.logInfo("-> Configurations started successfully.");
                    }
                }
            } catch (final ETComException e) {
                logger.logComException(e.getMessage());
                isLoaded = false;
            }
            return isLoaded;
        }

        /**
         * Gets the name of the given configuration file.
         *
         * @param configFile the configuration file
         * @return the configuration name
         */
        private String getConfigName(final String configFile) {
            String configName;
            if (StringUtils.isBlank(configFile)) {
                configName = "None";
            } else {
                configName = new File(configFile).getName();
            }
            return configName;
        }

        /**
         * Sets the new global constants for the currently loaded test configuration.
         * This requires to start the configuration, add the constants and reload the configuration.
         *
         * @param comClient   the COM client
         * @param constantMap the constants to set
         * @throws ETComException in case of a COM exception
         */
        private void setGlobalConstants(final ETComClient comClient, final Map<String, String> constantMap)
            throws ETComException {
            comClient.start();
            final TestConfiguration testConfig = (TestConfiguration) comClient.getCurrentTestConfiguration();
            for (final Entry<String, String> newConstant : constantMap.entrySet()) {
                testConfig.setGlobalConstant(newConstant.getKey(), newConstant.getValue());
            }
            comClient.stop();
        }

        /**
         * Converts the global constant list to a map.
         *
         * @return the global constant map
         * @throws ETComException in case of a COM exception
         */
        private Map<String, String> getGlobalConstantMap() throws ETComException {
            final Map<String, String> constantMap = new LinkedHashMap<>();
            for (final GlobalConstant constant : testConfig.getConstants()) {
                constantMap.put(constant.getName(), constant.getValue());
            }
            return constantMap;
        }
    }

    /**
     * Helper class storing information about the test result and the test report directory.
     */
    protected static final class TestInfoHolder implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String testResult;
        private final String testReportDir;
        private final boolean isAborted;

        /**
         * Instantiates a new {@link TestInfoHolder}.
         *
         * @param testResult    the test result
         * @param testReportDir the test report directory
         * @param isAborted     specifies whether test execution is aborted
         */
        public TestInfoHolder(final String testResult, final String testReportDir, final boolean isAborted) {
            this.testResult = testResult;
            this.testReportDir = testReportDir;
            this.isAborted = isAborted;
        }

        /**
         * @return the test result
         */
        public String getTestResult() {
            return testResult;
        }

        /**
         * @return the test report directory
         */
        public String getTestReportDir() {
            return testReportDir;
        }

        /**
         * @return specifies whether test execution is aborted
         */
        public boolean isAborted() {
            return isAborted;
        }
    }

    /**
     * Helper class storing information about the errors returned by checking packages and projects.
     */
    public static final class CheckInfoHolder {

        private final String filePath;
        private final Seriousness seriousness;
        private final String errorMessage;
        private final String lineNumber;

        /**
         * Instantiates a new {@link CheckInfoHolder}.
         *
         * @param filePath     the file path
         * @param seriousness  the seriousness
         * @param errorMessage the error message
         * @param lineNumber   the line number
         */
        public CheckInfoHolder(final String filePath, final Seriousness seriousness, final String errorMessage,
                               final String lineNumber) {
            super();
            this.filePath = filePath;
            this.seriousness = seriousness;
            this.errorMessage = errorMessage;
            this.lineNumber = lineNumber;
        }

        /**
         * @return the file path
         */
        public String getFilePath() {
            return filePath;
        }

        /**
         * @return the seriousness
         */
        public Seriousness getSeriousness() {
            return seriousness;
        }

        /**
         * @return the error message
         */
        public String getErrorMessage() {
            return errorMessage;
        }

        /**
         * @return the line number
         */
        public String getLineNumber() {
            return lineNumber;
        }

        /**
         * Defines the seriousness types for checks.
         */
        public enum Seriousness {
            /**
             * Seriousness indicating the check is informational only.
             */
            NOTE,

            /**
             * Seriousness indicating the check represents a warning.
             */
            WARNING,

            /**
             * Seriousness indicating the check represents an error.
             */
            ERROR
        }
    }
}
