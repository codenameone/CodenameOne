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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The desktop.titleBar build hint has to reach the generated stub, because that Display property
 * is the only way a ParparVM desktop port learns the project asked for anything.
 *
 * <p>It did not, and the failure was silent in the way that matters: Form.getDesktopTitleBarMode()
 * documents that the build hint wins and a theme must not talk it out of that, but with nothing
 * emitting the property, getConfiguredDesktopTitleBarMode() answered null on every native desktop
 * build and the theme constant decided alone. Windows and macOS looked correct only because Fluent
 * and Aqua ask for "native" anyway; on Linux, Adwaita asks for "custom", so hellocodenameone --
 * which sets desktop.titleBar=native -- kept a CN1 Toolbar with a hamburger instead of moving its
 * title to the window and its commands to the GTK menu bar.
 *
 * <p>Pinned here rather than left to a device build, because a device build is exactly where it
 * went unnoticed: the screenshots showed a plausible-looking toolbar rather than an error.
 */
class DesktopTitleBarStubPropertyTest {

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

    private final TestExecutor executor = new TestExecutor();

    private static BuildRequest withTitleBar(String value) {
        BuildRequest r = new BuildRequest();
        if (value != null) {
            r.putArgument("desktop.titleBar", value);
        }
        return r;
    }

    @Test
    void anUnsetHintEmitsNothingSoTheThemeConstantCanStillAnswer() {
        // Not a default: emitting one would destroy the distinction
        // getConfiguredDesktopTitleBarMode() exists to express. Null there means "nobody asked",
        // which is what lets a native theme's own constant decide.
        assertEquals("", executor.desktopTitleBarStubProperty(withTitleBar(null)));
    }

    @Test
    void eachOfTheThreeModesReachesTheStub() {
        for (String mode : new String[] {"native", "custom", "toolbar"}) {
            assertEquals(
                    "        Display.getInstance().setProperty(\"desktop.titleBar\", \""
                            + mode + "\");\n",
                    executor.desktopTitleBarStubProperty(withTitleBar(mode)),
                    "mode " + mode + " must reach the generated stub");
        }
    }

    @Test
    void theHintIsNormalisedRatherThanPassedThroughVerbatim() {
        // The hint is written by hand in codenameone_settings.properties, and the maven plugin's
        // own tests spell it "NATIVE". A mode Form cannot match reads as "toolbar", which is
        // indistinguishable from the hint being ignored -- the bug this whole property fixes.
        assertEquals(
                "        Display.getInstance().setProperty(\"desktop.titleBar\", \"native\");\n",
                executor.desktopTitleBarStubProperty(withTitleBar("  NATIVE  ")));
    }

    @Test
    void anUnknownModeIsDroppedRatherThanForwarded() {
        assertEquals("", executor.desktopTitleBarStubProperty(withTitleBar("headerbar")));
    }

    @Test
    void theEmittedLineIsValidStubJava() {
        String line = executor.desktopTitleBarStubProperty(withTitleBar("custom"));
        assertTrue(line.startsWith("        Display.getInstance().setProperty("), line);
        assertTrue(line.endsWith(");\n"), line);
        // One statement, one newline: it is concatenated into a stub between other property
        // lines, so a missing terminator would break the whole generated class.
        assertEquals(1, line.split("\n", -1).length - 1, line);
    }
}
