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
package com.codename1.impl.javase;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;

/**
 * The command an MCP host (Claude Desktop, Codex, opencode, ...) launches after a tool's
 * "Install" menu registers it. MCP hosts speak stdio; a running Codename One tool serves
 * MCP over a loopback socket. This launcher bridges the two: it relays the host's stdio
 * to the tool's socket, so the host drives the already-running, human-visible tool.
 *
 * Usage: {@code java -cp <cp> com.codename1.impl.javase.MCPStdioLauncher --attach <port>}
 */
public final class MCPStdioLauncher {
    private MCPStdioLauncher() {
    }

    public static void main(String[] args) throws Exception {
        // Reserve stdout for the protocol; everything else goes to stderr.
        final PrintStream hostOut = System.out;
        System.setOut(System.err);

        int port = 8765;
        for (int i = 0; i < args.length; i++) {
            if ("--attach".equals(args[i]) && i + 1 < args.length) {
                try {
                    port = Integer.parseInt(args[i + 1].trim());
                } catch (NumberFormatException ignored) {
                    // keep the default port
                }
            }
        }

        Socket socket = new Socket("127.0.0.1", port);
        final InputStream fromTool = socket.getInputStream();
        final OutputStream toTool = socket.getOutputStream();

        // tool -> host
        Thread pump = new Thread(new Runnable() {
            @Override
            public void run() {
                relay(fromTool, hostOut);
            }
        }, "cn1-mcp-bridge-in");
        pump.setDaemon(true);
        pump.start();

        // host -> tool (on the main thread; ends when the host closes stdin)
        relay(System.in, toTool);
        try {
            socket.close();
        } catch (Exception ignored) {
            // best effort close
        }
    }

    private static void relay(InputStream in, OutputStream out) {
        byte[] buffer = new byte[4096];
        try {
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                out.flush();
            }
        } catch (Exception ignored) {
            // the far side closed; the bridge is done
        }
    }
}
