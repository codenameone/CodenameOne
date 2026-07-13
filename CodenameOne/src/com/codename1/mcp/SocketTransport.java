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

import com.codename1.io.Socket;
import com.codename1.io.SocketConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/// Loopback socket MCP transport. Lets an already running, human visible application
/// accept an attaching MCP host on a local port, so an agent can drive a live session
/// the user is watching rather than launching a fresh headless subprocess.
///
/// Only one socket transport is active at a time, which matches the single server per
/// process model of {@link MCP}. Built on the portable {@link com.codename1.io.Socket}
/// server API so it links on every target, while only functioning where
/// {@link com.codename1.io.Socket#isServerSocketSupported()} is true.
public class SocketTransport implements MCPTransport {
    private static SocketTransport active;

    private static synchronized void setActive(SocketTransport transport) {
        active = transport;
    }

    private static synchronized SocketTransport currentActive() {
        return active;
    }

    private final int port;
    private final Object connectionLock = new Object();
    private Socket.StopListening stopListening;
    private InputStream in;
    private OutputStream out;
    private InputStreamReader reader;
    private Writer writer;

    public SocketTransport(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    @Override
    public void open() throws IOException {
        if (!Socket.isServerSocketSupported()) {
            throw new IOException("Server sockets are not supported on this platform");
        }
        setActive(this);
        stopListening = Socket.listen(port, Bridge.class);
        synchronized (connectionLock) {
            while (in == null) {
                try {
                    connectionLock.wait();
                } catch (InterruptedException ex) {
                    IOException io = new IOException("Interrupted while awaiting MCP client connection");
                    io.initCause(ex);
                    throw io;
                }
            }
        }
        reader = new InputStreamReader(in, "UTF-8");
        writer = new OutputStreamWriter(out, "UTF-8");
    }

    @Override
    public String readMessage() throws IOException {
        if (reader == null) {
            return null;
        }
        return MCPLineReader.readLine(reader);
    }

    @Override
    public void writeMessage(String message) throws IOException {
        if (writer == null) {
            throw new IOException("Socket transport is not open");
        }
        writer.write(message);
        writer.write('\n');
        writer.flush();
    }

    @Override
    public void close() {
        if (stopListening != null) {
            try {
                stopListening.stop();
            } catch (Throwable ignored) {
                // best effort shutdown of the listening socket
            }
            stopListening = null;
        }
        setActive(null);
    }

    void onConnection(InputStream is, OutputStream os) {
        synchronized (connectionLock) {
            this.in = is;
            this.out = os;
            connectionLock.notifyAll();
        }
    }

    /// Delivered to {@link com.codename1.io.Socket#listen} which instantiates one per
    /// accepted connection and reports the streams to the active transport.
    public static final class Bridge extends SocketConnection {
        @Override
        public void connectionError(int errorCode, String message) {
            // The listen loop reports failures here; the transport simply keeps waiting
            // for a subsequent successful connection.
        }

        @Override
        public void connectionEstablished(InputStream is, OutputStream os) {
            SocketTransport transport = currentActive();
            if (transport != null) {
                transport.onConnection(is, os);
            }
        }
    }
}
