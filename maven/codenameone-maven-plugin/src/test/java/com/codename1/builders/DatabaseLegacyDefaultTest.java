/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An Ant project predates the portable database contract, so rebuilding one has to keep the
 * behaviour it was written against unless its author asks for the new contract. A Maven project
 * gets the portable contract. Getting this backwards would silently change how a shipped
 * application's queries behave, which is the whole reason the compatibility switch exists, so the
 * decision is pinned here rather than left to a device run to discover.
 */
class DatabaseLegacyDefaultTest {

    /** Executor is abstract; none of these hooks are exercised here. */
    private static final class TestExecutor extends Executor {
        @Override
        protected String getDeviceIdCode() {
            return "";
        }

        @Override
        protected String generatePeerComponentCreationCode(String methodCallString) {
            return "";
        }

        @Override
        protected String convertPeerComponentToNative(String param) {
            return "";
        }

        @Override
        public boolean build(File sourceZip, BuildRequest request) {
            return false;
        }
    }

    private static BuildRequest antRequest() {
        return new BuildRequest();
    }

    private static BuildRequest mavenRequest() {
        BuildRequest r = new BuildRequest();
        r.putArgument("maven.codenameone-maven-plugin", "8.0");
        r.putArgument("maven.codenameone-core.version", "8.0");
        return r;
    }

    private final TestExecutor executor = new TestExecutor();

    @Test
    void detectsTheProjectTypeFromTheMavenPluginStamp() {
        assertTrue(executor.isMavenBuild(mavenRequest()));
        assertFalse(executor.isMavenBuild(antRequest()));
    }

    @Test
    void anAntProjectUsingTheDatabaseDefaultsToCompatibilityMode() {
        assertTrue(executor.isDatabaseLegacyMode(antRequest(), true));
    }

    @Test
    void aMavenProjectGetsThePortableContract() {
        assertFalse(executor.isDatabaseLegacyMode(mavenRequest(), true));
    }

    @Test
    void anAntProjectWithNoDatabaseIsLeftAlone() {
        // Harmless either way, but switching it on would put the explanation in the build log of
        // every Ant project ever built, most of which have no database at all.
        assertFalse(executor.isDatabaseLegacyMode(antRequest(), false));
    }

    @Test
    void anExplicitHintWinsInBothDirections() {
        BuildRequest antOptingIn = antRequest();
        antOptingIn.putArgument("db.legacy", "false");
        assertFalse(executor.isDatabaseLegacyMode(antOptingIn, true),
                "an Ant project has to be able to opt in to the portable contract");

        BuildRequest mavenPinning = mavenRequest();
        mavenPinning.putArgument("db.legacy", "true");
        assertTrue(executor.isDatabaseLegacyMode(mavenPinning, true),
                "a Maven project has to be able to pin compatibility mode");
    }

    @Test
    void theExplicitHintIsReadCaseInsensitively() {
        BuildRequest r = mavenRequest();
        r.putArgument("db.legacy", "TRUE");
        assertTrue(executor.isDatabaseLegacyMode(r, true));
    }

    @Test
    void emitsStubSourceOnlyWhenTheSwitchIsOn() {
        assertEquals("", executor.databaseLegacyStubProperty(mavenRequest(), true));
        assertEquals("", executor.databaseLegacyStubCall(mavenRequest(), true));

        String property = executor.databaseLegacyStubProperty(antRequest(), true);
        assertTrue(property.contains("setProperty(\"db.legacy\", \"true\")"), property);

        // The launcher variant runs before Display exists, so it must not go through it, and it
        // reaches the switch reflectively so it still compiles against a core that predates it.
        String call = executor.databaseLegacyStubCall(antRequest(), true);
        assertTrue(call.contains("setLegacyBehavior"), call);
        assertTrue(call.contains("Class.forName"), call);
        assertTrue(call.contains("catch (Throwable"), call);
        assertFalse(call.contains("Display"), call);
    }
}
