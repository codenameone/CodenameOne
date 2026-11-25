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

/// The `android.debugGuard` launch gate the builder writes into the generated activity.
class AndroidDebugGuardCallTest {

    @Test
    void offWritesNothing() {
        assertEquals("", AndroidGradleBuilder.debugGuardCall("false", "", "exit"));
        assertEquals("", AndroidGradleBuilder.debugGuardCall(null, "debugger", "warn"));
    }

    @Test
    void defaultModeExitsOnEveryCheck() {
        assertEquals("        com.codename1.impl.android.DebugEnvironmentDetector.runDebugGuard(this, \"\", true);\n",
                AndroidGradleBuilder.debugGuardCall("true", "", "exit"));
    }

    @Test
    void warnModeOnlyLogs() {
        assertEquals("        com.codename1.impl.android.DebugEnvironmentDetector.runDebugGuard(this, \"debugger,usbDebugging\", false);\n",
                AndroidGradleBuilder.debugGuardCall("true", "debugger,usbDebugging", "warn"));
    }

    @Test
    void checksAreEscapedIntoTheJavaLiteral() {
        assertEquals("        com.codename1.impl.android.DebugEnvironmentDetector.runDebugGuard(this, \"a\\\"b\\\\c\", true);\n",
                AndroidGradleBuilder.debugGuardCall("true", "a\"b\\c", "exit"));
    }
}
