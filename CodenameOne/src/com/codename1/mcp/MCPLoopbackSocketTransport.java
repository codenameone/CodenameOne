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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// Loopback socket MCP transport built on the portable [com.codename1.io.Socket] API, so
/// an agent can attach to a running app on ANY port that can bind the loopback interface —
/// a device or simulator, not only the desktop.
///
/// The channel is bound to loopback only. That is the security boundary: an attaching
/// agent can read the screen and drive the UI, so it must already be on the device (or on
/// a host forwarding into it), never merely on the same network. There is no wildcard
/// fallback; if the port cannot bind loopback this transport refuses to open.
///
/// The listening socket stays bound across client sessions, so an agent may disconnect and
/// reconnect — each drive call is typically its own short-lived connection — without the
/// server thread exiting.
public final class MCPLoopbackSocketTransport implements MCPTransport {
    /// Lets [Connection], which the socket API instantiates reflectively via a public
    /// no-argument constructor, find the transport it belongs to. A single reference
    /// suffices because MCP runs one server per process.
    private static MCPLoopbackSocketTransport active;

    private final int port;
    private final Object lock = new Object();
    private Socket.StopListening listening;
    private InputStream in;
    private OutputStream out;
    private boolean closed;

    MCPLoopbackSocketTransport(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    /// Registers this transport as the socket factory when the platform has not supplied
    /// one of its own. The JavaSE port keeps its native implementation (it can use
    /// java.net directly); every other port with loopback support gets this one.
    static void registerIfPlatformHasNone() {
        if (MCP.hasPlatformSocketTransport()) {
            return;
        }
        MCP.setSocketTransportFactory(new MCP.SocketTransportFactory() {
            @Override
            public MCPTransport createSocketTransport(int port) {
                return new MCPLoopbackSocketTransport(port);
            }
        });
    }

    @Override
    public void open() throws IOException {
        if (!Socket.isLoopbackServerSocketSupported()) {
            throw new IOException("This platform cannot bind a loopback server socket, so an "
                    + "MCP agent cannot attach to it");
        }
        synchronized (MCPLoopbackSocketTransport.class) {
            if (active != null && active != this) {
                throw new IOException("Another MCP socket transport is already open on port "
                        + active.port);
            }
            active = this;
        }
        listening = Socket.listenLoopback(port, Connection.class);
    }

    /// Called from the connection callback thread when a client attaches. The previous
    /// client, if any, is dropped: one agent drives at a time.
    void attach(InputStream is, OutputStream os) {
        synchronized (lock) {
            in = is;
            out = os;
            lock.notifyAll();
        }
    }

    /// Called when a client's session ends.
    void detach(InputStream is) {
        synchronized (lock) {
            if (in == is) {
                in = null;
                out = null;
                lock.notifyAll();
            }
        }
    }

    @Override
    public String readMessage() throws IOException {
        while (true) {
            InputStream stream;
            synchronized (lock) {
                while (in == null && !closed) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ex) {
                        // fall through: re-check the loop conditions
                    }
                }
                if (closed) {
                    return null;
                }
                stream = in;
            }
            String line = readLine(stream);
            if (line != null) {
                return line;
            }
            // client disconnected; wait for the next one rather than ending the server
            detach(stream);
            if (isClosed()) {
                return null;
            }
        }
    }

    /// Reads one newline-delimited UTF-8 message. Returns null at end of stream.
    ///
    /// Read byte at a time rather than through a buffered reader: a buffer would swallow
    /// bytes belonging to the next message, and this transport hands the same stream back
    /// after a reconnect.
    private String readLine(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        while (true) {
            int b;
            try {
                b = stream.read();
            } catch (IOException ex) {
                return null;   // a read error on the client is a disconnect
            }
            if (b < 0) {
                return buffer.size() == 0 ? null : toUtf8(buffer);
            }
            if (b == '\n') {
                return toUtf8(buffer);
            }
            if (b != '\r') {
                buffer.write(b);
            }
        }
    }

    private static String toUtf8(ByteArrayOutputStream buffer) throws IOException {
        return new String(buffer.toByteArray(), "UTF-8");
    }

    @Override
    public void writeMessage(String message) throws IOException {
        OutputStream stream;
        synchronized (lock) {
            stream = out;
        }
        if (stream == null) {
            throw new IOException("No MCP client is attached");
        }
        stream.write(message.getBytes("UTF-8"));
        stream.write('\n');
        stream.flush();
    }

    private boolean isClosed() {
        synchronized (lock) {
            return closed;
        }
    }

    @Override
    public void close() {
        Socket.StopListening l;
        InputStream is;
        synchronized (lock) {
            closed = true;
            l = listening;
            listening = null;
            is = in;
            in = null;
            out = null;
            lock.notifyAll();
        }
        synchronized (MCPLoopbackSocketTransport.class) {
            if (active == this) {
                active = null;
            }
        }
        if (l != null) {
            l.stop();
        }
        if (is != null) {
            try {
                is.close();
            } catch (IOException ignored) {
                // best effort: closing is what unblocks a pending read
            }
        }
    }

    /// The per-connection callback the socket API instantiates. Public with a no-argument
    /// constructor because [Socket#listenLoopback] creates it reflectively.
    public static final class Connection extends SocketConnection {
        @Override
        public void connectionError(int errorCode, String message) {
            // The listen loop reports its own failures; nothing useful to add per client.
        }

        @Override
        public void connectionEstablished(InputStream is, OutputStream os) {
            MCPLoopbackSocketTransport transport;
            synchronized (MCPLoopbackSocketTransport.class) {
                transport = active;
            }
            if (transport == null) {
                return;
            }
            transport.attach(is, os);
            // Hold this callback thread for the life of the session: the socket API closes
            // the streams as soon as it returns, and the server reads them from its own
            // thread.
            synchronized (transport.lock) {
                while (transport.in == is && !transport.closed) {
                    try {
                        transport.lock.wait();
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
            }
        }
    }
}
