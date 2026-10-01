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

/// desktop.width and desktop.height reach the native Linux and Windows stubs as a call made
/// BEFORE Display.init, since init is what creates the window. Both hints were declared and read
/// only by the JavaSE wrapper, so every ParparVM desktop window opened at a hard-coded 800x600.
class DesktopWindowSizeStubCallTest {

    /// Executor is abstract; none of these hooks are exercised here.
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

    private static final String IMPL = "com.codename1.impl.linux.LinuxImplementation";

    private final TestExecutor executor = new TestExecutor();

    private static BuildRequest size(String width, String height) {
        BuildRequest r = new BuildRequest();
        if (width != null) {
            r.putArgument("desktop.width", width);
        }
        if (height != null) {
            r.putArgument("desktop.height", height);
        }
        return r;
    }

    @Test
    void noHintKeepsThePortDefault() {
        assertEquals("", executor.desktopWindowSizeStubCall(size(null, null), IMPL));
    }

    @Test
    void bothDimensionsReachTheStub() {
        assertEquals("        " + IMPL + ".setDefaultWindowSize(1280, 720);\n",
                executor.desktopWindowSizeStubCall(size("1280", " 720 "), IMPL));
    }

    @Test
    void oneDimensionAloneLeavesTheOtherToThePort() {
        // Zero is "keep the port default" in setDefaultWindowSize.
        assertEquals("        " + IMPL + ".setDefaultWindowSize(0, 900);\n",
                executor.desktopWindowSizeStubCall(size(null, "900"), IMPL));
    }

    @Test
    void anUnusableValueIsDroppedRatherThanCompiledIntoTheStub() {
        assertEquals("", executor.desktopWindowSizeStubCall(size("wide", "-3"), IMPL));
    }
}
