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
package com.codename1.maven;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Packaging a backend must work on the JDK the developer already has.
///
/// `cn1:backend-package` used to demand a JDK 8 and fail without one, which asked
/// a developer on a current JDK to install a compiler from 2014 to build a server.
/// The premise was that a newer javac emits class files the translator cannot
/// read; it does not, because the compile passes `-source 1.8 -target 1.8` and the
/// class file version is 52 whichever javac produces it.
///
/// So the assertion is now unconditional: whatever JDK runs this test is the JDK
/// that must be resolved. A test that branched on the running version was how the
/// old premise went unexamined.
class BackendPackageJdkFallbackTest {

    private static File resolve(BackendPackageMojo mojo) throws Exception {
        Method resolve = BackendPackageMojo.class.getDeclaredMethod("resolveJdk");
        resolve.setAccessible(true);
        return (File) resolve.invoke(mojo);
    }

    private static void set(BackendPackageMojo mojo, String field, String value)
            throws Exception {
        Field declared = BackendPackageMojo.class.getDeclaredField(field);
        declared.setAccessible(true);
        declared.set(mojo, value);
    }

    @Test
    void resolvesTheRunningJdkWhateverItsVersion() throws Exception {
        BackendPackageMojo mojo = new BackendPackageMojo();
        set(mojo, "jdkHome", null);
        set(mojo, "jdk8Home", null);

        File home = resolve(mojo);
        assertNotNull(home, "the JDK running the build is the one to translate with");
        assertTrue(new File(home, "bin/javac").isFile()
                        || new File(home, "bin/javac.exe").isFile(),
                "and it must actually carry a javac: " + home);
    }

    /// The variable this goal used to require still selects a JDK, because
    /// projects and CI jobs set it. Dropping a requirement must not break the
    /// setups that were made to satisfy it.
    @Test
    void theOldJdk8PropertyStillWins() throws Exception {
        File running = resolve(new BackendPackageMojo());

        BackendPackageMojo mojo = new BackendPackageMojo();
        set(mojo, "jdkHome", null);
        set(mojo, "jdk8Home", running.getAbsolutePath());
        assertEquals(running.getAbsolutePath(), resolve(mojo).getAbsolutePath(),
                "JDK_8_HOME still names the JDK to use");
    }

    /// An explicit cn1.backend.jdk outranks the legacy variable, so a project
    /// that sets both gets the one it chose deliberately.
    @Test
    void theNewPropertyOutranksTheOldOne() throws Exception {
        File running = resolve(new BackendPackageMojo());

        BackendPackageMojo mojo = new BackendPackageMojo();
        set(mojo, "jdkHome", running.getAbsolutePath());
        set(mojo, "jdk8Home", new File(running, "no-such-jdk").getAbsolutePath());
        assertEquals(running.getAbsolutePath(), resolve(mojo).getAbsolutePath());
    }

    /// A configured directory that is not a JDK falls through to the running one
    /// rather than failing: the goal has a working compiler either way, and the
    /// old code already behaved this way for JDK_8_HOME.
    @Test
    void aConfiguredNonJdkFallsThroughToTheRunningOne() throws Exception {
        BackendPackageMojo mojo = new BackendPackageMojo();
        set(mojo, "jdkHome", new File("no-such-directory-anywhere").getAbsolutePath());
        set(mojo, "jdk8Home", null);
        assertEquals(resolve(new BackendPackageMojo()).getAbsolutePath(),
                resolve(mojo).getAbsolutePath());
    }

    /// Both spellings javac prints, and the failure answer for anything else.
    /// Parsed rather than assumed because the refusal below 8 is the one place a
    /// wrong reading would reject a JDK that works.
    @Test
    void readsBothSpellingsOfAJavacVersion() {
        assertEquals(8, BackendPackageMojo.parseJavacVersion("javac 1.8.0_402"));
        assertEquals(7, BackendPackageMojo.parseJavacVersion("javac 1.7.0_80"));
        assertEquals(11, BackendPackageMojo.parseJavacVersion("javac 11.0.22"));
        assertEquals(21, BackendPackageMojo.parseJavacVersion("javac 21.0.2"));
        assertEquals(25, BackendPackageMojo.parseJavacVersion("javac 25"));
        assertEquals(-1, BackendPackageMojo.parseJavacVersion(""));
        assertEquals(-1, BackendPackageMojo.parseJavacVersion(null));
        assertEquals(-1, BackendPackageMojo.parseJavacVersion("command not found"));
    }

    /// The floor is 8, and it is the only version check left. A JDK that cannot
    /// be asked its version is accepted, because the compile that follows reports
    /// what is really wrong with it.
    @Test
    void refusesOnlyBelowEight() throws Exception {
        BackendPackageMojo mojo = new BackendPackageMojo();
        Method require = BackendPackageMojo.class.getDeclaredMethod(
                "requireEightOrNewer", File.class);
        require.setAccessible(true);

        File running = resolve(new BackendPackageMojo());
        Object accepted = require.invoke(mojo, running);
        assertEquals(running, accepted, "the JDK running this test is 8 or newer");

        try {
            require.invoke(mojo, new File("no-javac-here"));
        } catch (java.lang.reflect.InvocationTargetException wrapped) {
            throw new AssertionError("a javac that cannot be run is accepted, not "
                    + "refused", wrapped.getCause());
        }
    }

    /// The remedy the hint names has to be one that WORKS. `-Dcn1.backend.jdk`
    /// selects the two FORKED steps -- the javac that compiles for translation and
    /// the java that runs the translator -- and not the in-process compile of the
    /// generated router and entry point, which JSR 199 takes from the JDK running
    /// Maven and offers no way to redirect. An earlier wording sent the developer
    /// to that property, which would have moved the failure by one step and no
    /// further.
    @Test
    void theSourceEightRemedyIsTheJdkRunningMaven() {
        String hint = BackendPackageMojo.SOURCE_EIGHT_REMOVED_HINT;
        assertTrue(hint.contains("Run Maven itself on a JDK"),
                "the remedy has to be the JDK running Maven: " + hint);
        // Named, but as the thing that does NOT cover the generated sources. The
        // assertion is that the sentence saying so is still there, because
        // deleting it leaves the property reading like the whole answer.
        assertTrue(hint.contains("compiled in process by the JDK running Maven"),
                "and it has to say why the property is not the answer: " + hint);
    }

    /// The hint added to javac's own wording fires on that wording and nothing
    /// else -- a compile error in the developer's sources must not be reported as
    /// a JDK problem.
    @Test
    void recognisesOnlyTheSourceEightRemoval() throws Exception {
        Method drops = BackendPackageMojo.class.getDeclaredMethod(
                "dropsSourceEight", MojoFailureException.class);
        drops.setAccessible(true);
        assertEquals(Boolean.TRUE, drops.invoke(null, new MojoFailureException(
                "Could not compile the backend sources:\n"
                        + "error: Source option 8 is no longer supported. Use 11 or later.")));
        assertEquals(Boolean.FALSE, drops.invoke(null, new MojoFailureException(
                "Could not compile the backend sources:\n"
                        + "Api.java:12: error: cannot find symbol")));
        assertEquals(Boolean.FALSE, drops.invoke(null,
                new MojoFailureException((String) null)));
    }
}
