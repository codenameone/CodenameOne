/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.maven.help;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ToolingHelpTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private String previousOverride;

    @Before
    public void redirectFailureFile() throws Exception {
        previousOverride = System.getProperty("cn1.help.lastFailureFile");
        File file = new File(tmp.getRoot(), "last-failure.properties");
        System.setProperty("cn1.help.lastFailureFile", file.getAbsolutePath());
    }

    @After
    public void restore() {
        if (previousOverride == null) {
            System.clearProperty("cn1.help.lastFailureFile");
        } else {
            System.setProperty("cn1.help.lastFailureFile", previousOverride);
        }
    }

    @Test
    public void summaryOfUsesFirstLineOfMessage() {
        Throwable t = new RuntimeException("archetype:generate failed: JAVA_HOME points to a JRE\nmore detail here");
        assertEquals("archetype:generate failed: JAVA_HOME points to a JRE", ToolingHelp.summaryOf(t));
    }

    @Test
    public void summaryOfFallsBackToClassName() {
        assertEquals("IllegalStateException", ToolingHelp.summaryOf(new IllegalStateException()));
    }

    @Test
    public void detailOfContainsStackTrace() {
        String detail = ToolingHelp.detailOf(new RuntimeException("boom"));
        assertTrue(detail.contains("boom"));
        assertTrue(detail.contains("ToolingHelpTest"));
    }

    @Test
    public void persistAndLoadRoundTrips() {
        ToolingHelp.clearLastFailure();
        assertNull(ToolingHelp.loadLastFailure());

        ToolingHelp.offerAfterFailure(null, "maven-plugin", "create_project", "7.0.153",
                new RuntimeException("archetype:generate failed"));

        ToolingHelpReport.Builder loaded = ToolingHelp.loadLastFailure();
        assertTrue(loaded != null);
        ToolingHelpReport report = loaded.build();
        assertEquals("maven-plugin", report.getComponent());
        assertEquals("create_project", report.getStep());
        assertEquals("7.0.153", report.getToolingVersion());
        assertEquals("archetype:generate failed", report.getErrorSummary());
        assertTrue(report.getErrorDetail().contains("archetype:generate failed"));
        // Environment auto-captured.
        assertEquals(System.getProperty("os.name"), report.getOs());
    }

    @Test
    public void errorDetailIsSelfContainedReproduction() {
        ToolingHelp.offerAfterFailure(null, "maven-plugin", "build_submit",
                "mvn cn1:build -Dcodename1.platform=ios", "8.0",
                new RuntimeException("Android build failed"));

        String detail = ToolingHelp.loadLastFailure().build().getErrorDetail();
        // Environment/command header so support can reproduce without reading the whole log.
        assertTrue(detail.contains("Step: build_submit"));
        assertTrue(detail.contains("Command: mvn cn1:build -Dcodename1.platform=ios"));
        assertTrue(detail.contains("JAVA_HOME"));
        assertTrue(detail.contains("Java: " + System.getProperty("java.version")));
        // ...followed by the full stack trace.
        assertTrue(detail.contains("Android build failed"));
        assertTrue(detail.contains("at com.codename1.maven.help.ToolingHelpTest"));
    }

    @Test
    public void clearLastFailureRemovesFile() {
        ToolingHelp.offerAfterFailure(null, "maven-plugin", "install", "7.0",
                new RuntimeException("x"));
        assertTrue(ToolingHelp.loadLastFailure() != null);
        ToolingHelp.clearLastFailure();
        assertNull(ToolingHelp.loadLastFailure());
    }

    @Test
    public void offerAfterFailureNeverThrowsOnNullFailure() {
        // Must be robust: a null throwable must not blow up the caller's failure path.
        ToolingHelp.offerAfterFailure(null, "maven-plugin", "other", "7.0", null);
    }
}
