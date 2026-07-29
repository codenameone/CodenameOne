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
/// The test implementation serves accepts from an in-memory queue rather than a real
/// socket, so a start that gets past the gate reaches a running server without any port
/// being bound. The gate is observable as the difference between throwing on the calling
/// thread and returning that running server.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MCPReleaseBuildGateTest extends UITestBase {

    private static final String GATE = "release build";

    @AfterEach
    void clearOverride() {
        MCP.setAllowOnReleaseBuilds(false);
        MCP.stop();
    }

    /// Asserts the gate refused, and returns the message so a test can check it names the
    /// reason rather than failing with something generic. Loopback support is ON, so the
    /// only reason to refuse is the gate.
    private String refusal(boolean debuggable) {
        implementation.setDebuggableBuild(debuggable);
        implementation.setServerSocketAvailable(true);
        String message = assertThrows(IllegalStateException.class,
                () -> MCP.startSocketServer(47899)).getMessage();
        // getMessage() is allowed to be null; assert it here so a future change that drops
        // the message fails as a readable assertion rather than a NullPointerException in
        // the caller's contains() check.
        assertNotNull(message, "the refusal must explain itself");
        return message;
    }

    /// Asserts the gate let the start through, leaving a running server behind.
    private void assertPassesGate(boolean debuggable) {
        implementation.setDebuggableBuild(debuggable);
        implementation.setServerSocketAvailable(true);
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
    void theBuildGateIsQueryableSeparatelyFromPlatformCapability() {
        // A caller preflighting with isSocketSupported() alone would be misled: that asks
        // whether the platform CAN bind, and a build can be capable yet not permitted.
        implementation.setServerSocketAvailable(true);

        implementation.setDebuggableBuild(false);
        assertFalse(MCP.isSocketServerAllowedOnThisBuild(),
                "a release build must report that it is not allowed to serve");
        assertTrue(MCP.isSocketSupported(),
                "capability is unchanged by the gate; the platform can still bind");

        implementation.setDebuggableBuild(true);
        assertTrue(MCP.isSocketServerAllowedOnThisBuild(),
                "a development build must report that it is allowed to serve");

        implementation.setDebuggableBuild(false);
        MCP.setAllowOnReleaseBuilds(true);
        assertTrue(MCP.isSocketServerAllowedOnThisBuild(),
                "the override must be reflected in the preflight, not only in the start");
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
        // build would be reported as a mere platform limitation and would start serving
        // the moment the platform gained loopback support.
        implementation.setDebuggableBuild(false);
        implementation.setServerSocketAvailable(false);
        String message = assertThrows(IllegalStateException.class,
                () -> MCP.startSocketServer(47898)).getMessage();
        assertNotNull(message, "the refusal must explain itself");
        assertTrue(message.contains(GATE),
                "the build gate must be evaluated first, got: " + message);
        assertFalse(MCP.isRunning());
    }

    @Test
    void aPlatformThatCannotBindLoopbackFailsOnTheCallingThread() {
        // The portable transport must not register itself on a platform that cannot bind,
        // because then the start would look like it succeeded and only fail later on the
        // server's own thread, where a caller cannot see it.
        implementation.setDebuggableBuild(true);
        implementation.setServerSocketAvailable(false);
        String message = assertThrows(IllegalStateException.class,
                () -> MCP.startSocketServer(47897)).getMessage();
        assertNotNull(message, "the refusal must explain itself");
        assertTrue(message.contains("No socket MCP transport"),
                "an unbindable platform must say so synchronously, got: " + message);
        assertFalse(MCP.isRunning(), "nothing must be left running after that refusal");
    }
}
