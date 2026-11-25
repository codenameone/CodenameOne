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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// The `android.debugGuard` launch gate the builder writes into the generated activity.
class AndroidDebugGuardCallTest {

    @Test
    void offWritesNothing() {
        assertEquals("", AndroidGradleBuilder.debugGuardCall("false", "", "exit"));
        assertEquals("", AndroidGradleBuilder.debugGuardCall(null, "debugger", "warn"));
    }

    @Test
    void defaultsToTheDebuggerAloneAndExits() {
        assertEquals("        com.codename1.impl.android.DebugEnvironmentDetector.runDebugGuard(this, \"debugger\", true);\n",
                AndroidGradleBuilder.debugGuardCall("true", "", "exit"));
        assertEquals("debugger", AndroidGradleBuilder.debugGuardChecks(" , "));
    }

    @Test
    void anUnknownCheckFailsTheBuild() {
        // a typo must not switch the gate off silently
        assertThrows(IllegalArgumentException.class,
                () -> AndroidGradleBuilder.debugGuardCall("true", "debuger", "exit"));
        assertThrows(IllegalArgumentException.class,
                () -> AndroidGradleBuilder.debugGuardChecks("DEBUGGER"));
    }

    @Test
    void checksAreNormalized() {
        assertEquals("debugger,developerOptions,usbDebugging",
                AndroidGradleBuilder.debugGuardChecks(" debugger , developerOptions,usbDebugging,"));
    }

    @Test
    void warnModeOnlyLogs() {
        assertEquals("        com.codename1.impl.android.DebugEnvironmentDetector.runDebugGuard(this, \"debugger,usbDebugging\", false);\n",
                AndroidGradleBuilder.debugGuardCall("true", "debugger,usbDebugging", "warn"));
    }
}
