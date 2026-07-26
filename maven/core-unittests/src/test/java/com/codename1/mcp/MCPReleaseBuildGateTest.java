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
package com.codename1.mcp;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The MCP socket server must not bind on a release build. An attached agent reads the
/// screen and drives the UI, and loopback is shared by everything on the device, so on a
/// shipped app any other installed app could drive it.
///
/// These tests never bind a real socket: loopback support is turned OFF, so a start that
/// gets past the gate fails to bind on the server's own reader thread, which only logs.
/// The gate is therefore observable as the difference between throwing on the calling
/// thread and returning a running server.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MCPReleaseBuildGateTest extends UITestBase {

    private static final String GATE = "release build";

    @AfterEach
    void clearOverride() {
        MCP.setAllowOnReleaseBuilds(false);
        MCP.stop();
    }

    /// Asserts the gate refused, and returns the message so a test can check it names the
    /// reason rather than failing with something generic.
    private String refusal(boolean debuggable) {
        implementation.setDebuggableBuild(debuggable);
        implementation.setServerSocketAvailable(false);
        return assertThrows(IllegalStateException.class,
                () -> MCP.startSocketServer(47899)).getMessage();
    }

    /// Asserts the gate let the start through. Binding still fails (there is no loopback
    /// support here) but that happens on the reader thread, so the call returns.
    private void assertPassesGate(boolean debuggable) {
        implementation.setDebuggableBuild(debuggable);
        implementation.setServerSocketAvailable(false);
        assertNotNull(MCP.startSocketServer(47899), "start must return the shared server");
        assertTrue(MCP.isRunning(), "the server must be running once the gate is passed");
    }

    @Test
    void blocksTheSocketServerOnAReleaseBuild() {
        String message = refusal(false);
        assertTrue(message.contains(GATE),
                "a release build must be refused by the gate, got: " + message);
    }

    @Test
    void allowsTheSocketServerOnADevelopmentBuild() {
        assertPassesGate(true);
    }

    @Test
    void explicitOverrideLiftsTheBlock() {
        MCP.setAllowOnReleaseBuilds(true);
        assertPassesGate(false);
    }

    @Test
    void theOverrideIsOffByDefault() {
        // The safe direction has to be the default: a developer who never thinks about
        // this ships an app that cannot be driven.
        assertFalse(MCP.isAllowedOnReleaseBuilds());
    }

    @Test
    void aBlockedStartLeavesNoServerRunning() {
        // The gate runs before the transport is registered or opened, so a refusal must
        // not leave a half-started server behind.
        refusal(false);
        assertFalse(MCP.isRunning(), "a refused start must not leave a server running");
    }

    @Test
    void theGateIsCheckedBeforeTransportAvailability() {
        // Both failure modes apply at once here. The gate must win, otherwise a release
        // build on a port that CAN bind would be reported as a platform limitation and
        // then quietly succeed once the platform gained support.
        implementation.setDebuggableBuild(false);
        implementation.setServerSocketAvailable(true);
        String message = assertThrows(IllegalStateException.class,
                () -> MCP.startSocketServer(47898)).getMessage();
        assertTrue(message.contains(GATE),
                "the build gate must be evaluated first, got: " + message);
        assertFalse(MCP.isRunning());
    }
}
