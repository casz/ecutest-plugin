/*
 * Copyright (c) 2015-2019 TraceTronic GmbH
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package de.tracetronic.jenkins.plugins.ecutest.log;

import hudson.model.TaskListener;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;

/**
 * A helper class that offers various types of logging.
 * Provides plain logging into console log and annotated logging with {@link TTConsoleAnnotator}.
 *
 * @author Christian Pönisch <christian.poenisch@tracetronic.de>
 */
public class TTConsoleLogger {

    private final TaskListener listener;
    private final TTConsoleAnnotator annotator;

    /**
     * Instantiates a new {@link TTConsoleLogger}.
     *
     * @param listener the listener
     */
    public TTConsoleLogger(final TaskListener listener) {
        this.listener = listener;
        annotator = new TTConsoleAnnotator(this.listener.getLogger());
    }

    /**
     * Gets the logger.
     *
     * @return the {@link PrintStream} logger
     */
    public PrintStream getLogger() {
        return listener.getLogger();
    }

    /**
     * Logs annotated message.
     *
     * @param message the message to log
     */
    public void logAnnot(final String message) {
        logAnnot("", message);
    }

    /**
     * Logs info message.
     *
     * @param message the message to log
     */
    public void logInfo(final String message) {
        logAnnot("[TT] INFO: ", message);
    }

    /**
     * Logs warning message.
     *
     * @param message the message to log
     */
    public void logWarn(final String message) {
        logAnnot("[TT] WARN: ", message);
    }

    /**
     * Logs error message.
     *
     * @param message the message to log
     */
    public void logError(final String message) {
        logAnnot("[TT] ERROR: ", message);
    }

    /**
     * Logs error message caused by COM exception.
     *
     * @param message the message to log
     */
    public void logComException(final String message) {
        logError(String
            .format("Caught ComException: %s%n"
                    + "For further information see FAQ: "
                    + "https://wiki.jenkins-ci.org/x/joLtB#TraceTronicECU-TESTPlugin-FAQ",
                message));
    }

    /**
     * Logs debug message. Can be enabled either by
     * providing -Decutest.debugLog=true to Jenkins master JVM or
     * setting system property ecutest.debugLog directly.
     *
     * @param message the message to log
     */
    public void logDebug(final String message) {
        if (Boolean.getBoolean("ecutest.debugLog")) {
            logAnnot("[TT] DEBUG: ", message);
        }
    }

    /**
     * Logs annotated message.
     *
     * @param prefix  the prefix
     * @param message message to be annotated
     */
    public void logAnnot(final String prefix, final String message) {
        final String log = prefix + message + "\n";
        final byte[] msg = log.getBytes(Charset.defaultCharset());
        try {
            annotator.eol(msg, msg.length);
        } catch (final IOException e) {
            listener.getLogger().println("Problem with writing into console log: " + e.getMessage());
        }
    }

    /**
     * Logs plain text messages directly into console.
     *
     * @param message message in plain text
     */
    public void log(final String message) {
        listener.getLogger().println(message);
    }
}
