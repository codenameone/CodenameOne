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

import com.codename1.mcp.MCP;
import com.codename1.mcp.MCPTransport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/// Loopback socket MCP transport for the JavaSE desktop port. Lets an already running,
/// human visible tool (the simulator, Settings, ...) accept an attaching MCP host on a
/// local port, so an agent can drive a live session the user is watching rather than
/// launching a fresh headless subprocess.
///
/// It lives in the JavaSE port rather than the portable core so it can bind a real
/// {@link java.net.ServerSocket} to the loopback address only. A wildcard bind would
/// expose this control channel on every network interface; keeping it on the loopback
/// interface means an attaching agent must run on this machine.
///
/// A fresh instance is created for every server run (there is no shared mutable state),
/// and it owns both the listening socket and the accepted client socket so {@link #close()}
/// can close them and unblock a pending {@code accept()} or {@code read()} promptly.
public final class MCPSocketTransport implements MCPTransport {
    private final int port;
    private final Object lock = new Object();
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private boolean closed;
    private BufferedReader reader;
    private Writer writer;

    MCPSocketTransport(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    /// Registers this transport as the platform socket factory so
    /// {@link MCP#startSocketServer(int)} works on the desktop. Safe to call repeatedly.
    public static void register() {
        MCP.setSocketTransportFactory(new MCP.SocketTransportFactory() {
            @Override
            public MCPTransport createSocketTransport(int port) {
                return new MCPSocketTransport(port);
            }
        });
    }

    @Override
    public void open() throws IOException {
        // Bind to the loopback interface only so the MCP control channel is never exposed
        // to the local network. Backlog of one: a single agent attaches at a time, but the
        // listening socket stays bound across client sessions so an agent can disconnect and
        // reconnect (each screenshot / drive call is its own short-lived connection) without
        // the server thread exiting. The first client is accepted lazily in readMessage().
        ServerSocket ss = new ServerSocket(port, 1, InetAddress.getLoopbackAddress());
        synchronized (lock) {
            if (closed) {
                try {
                    ss.close();
                } catch (IOException ignored) {
                    // best effort close of the socket we are abandoning
                }
                throw new IOException("Socket transport closed before it could listen");
            }
            serverSocket = ss;
        }
    }

    /// Accepts the next client on the already-bound listening socket. Returns false when
    /// the transport has been closed (server shutting down).
    private boolean acceptNextClient() throws IOException {
        ServerSocket ss;
        synchronized (lock) {
            ss = serverSocket;
        }
        if (ss == null) {
            return false;
        }
        Socket socket;
        try {
            socket = ss.accept();
        } catch (IOException ex) {
            if (isClosed()) {
                return false;
            }
            throw ex;
        }
        synchronized (lock) {
            clientSocket = socket;
        }
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        writer = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
        return true;
    }

    /// Drops the current client (its session ended) but keeps the listening socket bound.
    private void closeClientOnly() {
        Socket cs;
        synchronized (lock) {
            cs = clientSocket;
            clientSocket = null;
        }
        reader = null;
        writer = null;
        if (cs != null) {
            try {
                cs.close();
            } catch (IOException ignored) {
                // best effort
            }
        }
    }

    private boolean isClosed() {
        synchronized (lock) {
            return closed;
        }
    }

    @Override
    public String readMessage() throws IOException {
        // Reads the next line from the current client; when that client disconnects,
        // transparently accepts the next one so the server survives reconnects. Returns
        // null only when the whole transport is closed (server shutdown).
        while (true) {
            if (reader == null) {
                if (!acceptNextClient()) {
                    return null;
                }
            }
            String line;
            try {
                line = reader.readLine();
            } catch (IOException ex) {
                line = null;   // treat a read error as a disconnect
            }
            if (line != null) {
                return line;
            }
            closeClientOnly();
            if (isClosed()) {
                return null;
            }
        }
    }

    @Override
    public void writeMessage(String message) throws IOException {
        Writer w = writer;
        if (w == null) {
            // No current client (it disconnected between request and response); the reply
            // is moot. Dropping it keeps the server alive for the next connection.
            return;
        }
        w.write(message);
        w.write('\n');
        w.flush();
    }

    @Override
    public void close() {
        Socket cs;
        ServerSocket ss;
        synchronized (lock) {
            closed = true;
            cs = clientSocket;
            clientSocket = null;
            ss = serverSocket;
            serverSocket = null;
        }
        if (cs != null) {
            try {
                cs.close();
            } catch (IOException ignored) {
                // best effort close of the accepted connection
            }
        }
        if (ss != null) {
            try {
                ss.close();
            } catch (IOException ignored) {
                // best effort close of the listening socket, unblocks a pending accept()
            }
        }
    }
}
