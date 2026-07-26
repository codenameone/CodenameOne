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

import com.codename1.ai.Tool;
import com.codename1.ui.Display;

/// Public entry point for the Codename One MCP headless API. Invoking a starter here is
/// the enablement; there is no build hint or property to toggle. A single server
/// instance exists per process.
///
/// The stdio transport is supported by the Codename One JavaSE port, which powers the
/// simulator and the desktop tooling. The socket transport also serves a build running on
/// a device, on any port that can bind the loopback interface. It is blocked on a release
/// build - see [#setAllowOnReleaseBuilds(boolean)].
///
/// #### Typical usage
///
/// ```
/// // Let a running tool or the simulator accept an attaching agent on a local port:
/// MCP.startSocketServer(8642);
///
/// // Or serve over stdio when a host launches the tool as a subprocess:
/// MCP.startStdioServer();
///
/// // Publish domain tools alongside the automatic UI driving tools:
/// MCP.addTool(myTool);
/// ```
public final class MCP {
    private static MCPServer server;
    private static StdioTransportFactory stdioTransportFactory;
    private static SocketTransportFactory socketTransportFactory;
    /// Lifts the release build block; see [#setAllowOnReleaseBuilds(boolean)].
    private static boolean allowOnReleaseBuilds;

    private MCP() {
    }

    /// Supplies the platform stdio transport. The stdio transport lives outside the
    /// portable core because it needs process standard input, which is not available on
    /// every target (for example the ParparVM Java runtime). The JavaSE port registers
    /// its implementation during initialization.
    public interface StdioTransportFactory {
        MCPTransport createStdioTransport();
    }

    /// Supplies the platform loopback socket transport. The socket transport lives outside
    /// the portable core because it binds a real server socket to the loopback interface,
    /// which the portable {@link com.codename1.io.Socket} API does not allow (it binds the
    /// wildcard address, exposing the channel on every network interface). The JavaSE port
    /// registers its implementation during initialization.
    public interface SocketTransportFactory {
        MCPTransport createSocketTransport(int port);
    }

    /// Registers the platform stdio transport factory. Called by the JavaSE port.
    public static void setStdioTransportFactory(StdioTransportFactory factory) {
        stdioTransportFactory = factory;
    }

    /// Registers the platform socket transport factory. Called by the JavaSE port.
    public static void setSocketTransportFactory(SocketTransportFactory factory) {
        socketTransportFactory = factory;
    }

    /// Whether the PORT supplied a transport of its own. Distinct from
    /// [#isSocketSupported()], which also counts the portable fallback - the fallback
    /// must not see itself as already installed.
    static boolean hasPlatformSocketTransport() {
        return socketTransportFactory != null;
    }

    /// Whether an stdio transport is available on this platform.
    public static boolean isStdioSupported() {
        return stdioTransportFactory != null;
    }

    /// Whether a loopback socket transport is available on this platform - either one the
    /// port registered itself, or the portable one, which needs
    /// [com.codename1.io.Socket#isLoopbackServerSocketSupported()].
    public static boolean isSocketSupported() {
        return socketTransportFactory != null
                || com.codename1.io.Socket.isLoopbackServerSocketSupported();
    }

    /// Returns the shared server, creating it on first use.
    public static synchronized MCPServer getServer() {
        if (server == null) {
            server = new MCPServer();
        }
        return server;
    }

    public static synchronized boolean isRunning() {
        return server != null && server.isRunning();
    }

    /// Starts the stdio transport server (the standard MCP local transport). Requires a
    /// platform stdio transport factory (registered by the JavaSE port).
    public static synchronized MCPServer startStdioServer() {
        if (stdioTransportFactory == null) {
            throw new IllegalStateException(
                    "No stdio MCP transport is available on this platform. Use startSocketServer(int) "
                            + "for socket attach, or run on the JavaSE port.");
        }
        MCPServer s = getServer();
        s.start(stdioTransportFactory.createStdioTransport());
        return s;
    }

    /// Starts a loopback socket server so an agent can attach to this running process.
    /// Requires a platform socket transport factory (registered by the JavaSE port).
    ///
    /// Refuses to bind on a RELEASE build, throwing [IllegalStateException] - see
    /// [#setAllowOnReleaseBuilds(boolean)] for why, and for the deliberate override.
    public static synchronized MCPServer startSocketServer(int port) {
        checkAllowedOnThisBuild();
        // Ports that did not register a transport of their own fall back to the portable
        // one, which binds loopback through com.codename1.io.Socket - so attaching to a
        // running app on a device or simulator works, not only on the desktop.
        MCPLoopbackSocketTransport.registerIfPlatformHasNone();
        if (socketTransportFactory == null) {
            throw new IllegalStateException(
                    "No socket MCP transport is available on this platform: it cannot bind a "
                            + "loopback server socket.");
        }
        MCPServer s = getServer();
        s.start(socketTransportFactory.createSocketTransport(port));
        return s;
    }

    /// Permits the socket server to bind on a RELEASE build. Off by default, and turning
    /// it on should be a deliberate, reviewed decision.
    ///
    /// The reason for the default: an attached agent can read the screen and drive the
    /// user interface, and the loopback interface is shared by everything on the device,
    /// not private to one application. On a phone that means any OTHER app installed
    /// alongside yours can connect to the port and drive your application. That is a fine
    /// trade while developing - it is how an agent attaches to a running app - and a poor
    /// one in an app a user installs.
    ///
    /// A development build is detected as a debuggable Android package, a development
    /// provisioned iOS build, or the simulator and desktop tooling. Anything else is
    /// treated as a release build, so a port that cannot tell withholds the server rather
    /// than exposing it.
    ///
    /// Set this only for a build that ships to a controlled fleet - a kiosk, a test lab,
    /// an enterprise deployment - where the device itself is trusted.
    ///
    /// #### Parameters
    ///
    /// - `allow`: true to permit the socket server on a release build
    public static synchronized void setAllowOnReleaseBuilds(boolean allow) {
        allowOnReleaseBuilds = allow;
    }

    /// Whether the socket server may bind on a release build.
    public static synchronized boolean isAllowedOnReleaseBuilds() {
        return allowOnReleaseBuilds;
    }

    /// Throws unless this build may serve MCP over a socket.
    private static void checkAllowedOnThisBuild() {
        if (allowOnReleaseBuilds || Display.getInstance().isDebuggableBuild()) {
            return;
        }
        throw new IllegalStateException(
                "The MCP socket server is blocked on a release build. An attached agent can "
                        + "read the screen and drive the UI, and any other app on the device "
                        + "can reach the loopback port. Use a development build, or call "
                        + "MCP.setAllowOnReleaseBuilds(true) if this build ships to a "
                        + "controlled fleet.");
    }

    /// Stops the shared server if it is running.
    public static synchronized void stop() {
        if (server != null) {
            server.stop();
        }
    }

    /// Registers a developer defined tool with the shared server.
    public static void addTool(Tool tool) {
        getServer().addTool(tool);
    }

    /// Sets how much of the MCP conversation is echoed to the Codename One log so a
    /// developer can watch and debug what an agent is doing.
    public static void setVerbosity(MCPVerbosity verbosity) {
        getServer().setVerbosity(verbosity);
    }

    public static MCPVerbosity getVerbosity() {
        return getServer().getVerbosity();
    }
}
