/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * A build hint or an entry point overridden on the command line has to survive
 * both hops before it reaches the running simulator: {@code cn1:run} spawns a
 * nested Maven build, and that build forks the simulator JVM.
 *
 * <p>Neither hop carried anything, so {@code -Dcodename1.arg.desktop.titleBar}
 * was accepted, printed in the build's own property dump, and then silently
 * ignored -- while the identical command line changed a device build. The
 * simulator even has a guard for it, publishing an annotated hint only where no
 * system property claims the key; nothing ever set one, so the guard was
 * inert.</p>
 */
public class SimulatorOverrideForwardingTest {

    /** Hop one: cn1:run to the nested build. */
    @Test
    public void theNestedBuildInheritsTheCodenameOneCommandLine() {
        Properties user = new Properties();
        user.setProperty("codename1.arg.desktop.titleBar", "NATIVE");
        user.setProperty("codename1.mainName", "OverriddenApp");
        user.setProperty("unrelated.property", "no");

        Properties nested = CN1RunMojo.nestedBuildProperties(user);
        assertEquals("NATIVE", nested.getProperty("codename1.arg.desktop.titleBar"));
        assertEquals("OverriddenApp", nested.getProperty("codename1.mainName"));
        // Only the codename1 namespace, the same rule the hint overlay applies.
        assertNull(nested.getProperty("unrelated.property"));
        assertEquals("javase", nested.getProperty("codename1.platform"));
    }

    /** ...and this goal IS the javase simulator, whatever the command line says. */
    @Test
    public void theNestedBuildStaysOnJavase() {
        Properties user = new Properties();
        user.setProperty("codename1.platform", "android");
        assertEquals("javase",
                CN1RunMojo.nestedBuildProperties(user).getProperty("codename1.platform"));
    }

    /** With no session there is nothing to inherit, and the platform still holds. */
    @Test
    public void withoutASessionThePlatformIsStillSet() {
        assertEquals("javase",
                CN1RunMojo.nestedBuildProperties(null).getProperty("codename1.platform"));
    }

    /**
     * Hop two: the nested build to the forked simulator.
     *
     * <p>Only what {@code -D} passed, never the settings file's own hints:
     * publishing those as system properties would outrank the file itself in
     * {@code buildHint()} and hide the both-declared conflict the simulator
     * reports.</p>
     */
    @Test
    public void onlyCommandLineHintsAreForwardedToTheFork() {
        Properties user = new Properties();
        user.setProperty("codename1.arg.desktop.titleBar", "NATIVE");
        user.setProperty("codename1.mainName", "OverriddenApp");
        user.setProperty("maven.test.skip", "true");

        Properties forwarded = SimulatorMojo.commandLineBuildHints(user);
        assertEquals("NATIVE", forwarded.getProperty("codename1.arg.desktop.titleBar"));
        // The identity pair travels separately, as its own two properties.
        assertNull(forwarded.getProperty("codename1.mainName"));
        assertNull(forwarded.getProperty("maven.test.skip"));
        assertEquals(1, forwarded.size());
    }

    /** No session, nothing forwarded -- and no exception. */
    @Test
    public void withoutASessionNothingIsForwarded() {
        assertEquals(0, SimulatorMojo.commandLineBuildHints(null).size());
    }
}
