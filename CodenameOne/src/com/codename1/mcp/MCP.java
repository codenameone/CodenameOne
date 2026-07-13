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

/// Public entry point for the Codename One MCP headless API. Invoking a starter here is
/// the enablement; there is no build hint or property to toggle. A single server
/// instance exists per process.
///
/// This API is supported by the Codename One JavaSE port, which powers the simulator and
/// the desktop tooling. It is not part of a packaged application build.
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

    private MCP() {
    }

    /// Supplies the platform stdio transport. The stdio transport lives outside the
    /// portable core because it needs process standard input, which is not available on
    /// every target (for example the ParparVM Java runtime). The JavaSE port registers
    /// its implementation during initialization.
    public interface StdioTransportFactory {
        MCPTransport createStdioTransport();
    }

    /// Registers the platform stdio transport factory. Called by the JavaSE port.
    public static void setStdioTransportFactory(StdioTransportFactory factory) {
        stdioTransportFactory = factory;
    }

    /// Whether an stdio transport is available on this platform.
    public static boolean isStdioSupported() {
        return stdioTransportFactory != null;
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
    public static synchronized MCPServer startSocketServer(int port) {
        MCPServer s = getServer();
        s.start(new SocketTransport(port));
        return s;
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
}
