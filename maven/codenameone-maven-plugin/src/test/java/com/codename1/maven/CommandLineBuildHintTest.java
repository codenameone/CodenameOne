/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.maven;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A build hint given on the command line has to reach the build.
 *
 * <p>Hints were read only out of {@code codenameone_settings.properties}, so
 * {@code -Dcodename1.arg.ios.onDeviceDebug=true} was accepted by Maven, echoed
 * in its own property dump, and then ignored — the builder asked
 * {@code request.getArg(...)} and got the file's value. That produced a build
 * with no symbol table and no debug listener while appearing to have asked for
 * both, which is the complaint that opened issue #5333.</p>
 */
class CommandLineBuildHintTest {

    @Test
    void aCommandLineHintOverridesTheSettingsFile() throws Exception {
        Properties settings = new Properties();
        settings.setProperty("codename1.arg.ios.onDeviceDebug", "false");

        Properties fromCommandLine = new Properties();
        fromCommandLine.setProperty("codename1.arg.ios.onDeviceDebug", "true");

        overlay(settings, fromCommandLine);

        assertEquals("true", settings.getProperty("codename1.arg.ios.onDeviceDebug"));
    }

    @Test
    void aHintAbsentFromTheFileIsStillApplied() throws Exception {
        Properties settings = new Properties();
        Properties fromCommandLine = new Properties();
        fromCommandLine.setProperty("codename1.arg.ios.onDeviceDebug", "true");

        overlay(settings, fromCommandLine);

        assertEquals("true", settings.getProperty("codename1.arg.ios.onDeviceDebug"));
    }

    /**
     * Only Codename One properties are overlaid. The user properties carry
     * everything passed with -D, and turning an unrelated one into a build hint
     * would put arbitrary values into the build request.
     */
    @Test
    void unrelatedCommandLinePropertiesAreNotBuildHints() throws Exception {
        Properties settings = new Properties();
        Properties fromCommandLine = new Properties();
        fromCommandLine.setProperty("maven.test.skip", "true");
        fromCommandLine.setProperty("java.awt.headless", "true");

        overlay(settings, fromCommandLine);

        assertNull(settings.getProperty("maven.test.skip"));
        assertNull(settings.getProperty("java.awt.headless"));
    }

    /** Settings the command line does not mention are left alone. */
    @Test
    void hintsNotGivenOnTheCommandLineAreUntouched() throws Exception {
        Properties settings = new Properties();
        settings.setProperty("codename1.arg.ios.uiscene", "true");
        settings.setProperty("codename1.mainName", "MyApp");

        overlay(settings, new Properties());

        assertEquals("true", settings.getProperty("codename1.arg.ios.uiscene"));
        assertEquals("MyApp", settings.getProperty("codename1.mainName"));
    }

    /**
     * Drives the mojo's own overlay against a stand-in session, so the rule
     * under test is the shipped one rather than a restatement of it.
     */
    private void overlay(Properties settings, Properties userProperties) throws Exception {
        CN1BuildMojo mojo = new CN1BuildMojo();

        Field sessionField = findField(mojo.getClass(), "session");
        sessionField.setAccessible(true);
        sessionField.set(mojo, new StubSession(userProperties));

        Method overlay = findMethod(mojo.getClass(), "overlayCommandLineBuildHints", Properties.class);
        overlay.setAccessible(true);
        overlay.invoke(mojo, settings);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException keepLooking) {
                // up the chain
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... args)
            throws NoSuchMethodException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredMethod(name, args);
            } catch (NoSuchMethodException keepLooking) {
                // up the chain
            }
        }
        throw new NoSuchMethodException(name);
    }

    /** A MavenSession that carries nothing but the -D values. */
    private static final class StubSession extends org.apache.maven.execution.MavenSession {
        private final Properties userProperties;

        StubSession(Properties userProperties) {
            super(null, null, new org.apache.maven.execution.DefaultMavenExecutionRequest(),
                    new org.apache.maven.execution.DefaultMavenExecutionResult());
            this.userProperties = userProperties;
        }

        @Override
        public Properties getUserProperties() {
            return userProperties;
        }
    }
}
