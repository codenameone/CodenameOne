/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.db;

import com.codename1.db.sql.SqlPlatform;
import com.codename1.db.sql.SqlSocket;
import com.codename1.io.Socket;
import com.codename1.io.SocketConnection;
import com.codename1.io.Util;
import com.codename1.security.CryptoEngine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// The client's [SqlPlatform]: plain TCP through [Socket] and randomness from
/// [CryptoEngine]. There is no TLS on these sockets, so [ServerDatabase] never
/// asks for it: a mode that demands TLS is refused before connecting.
final class ClientSqlPlatform extends SqlPlatform {
    private static final ClientSqlPlatform INSTANCE = new ClientSqlPlatform();
    private static boolean installed;

    private ClientSqlPlatform() {
    }

    static synchronized void install() {
        if (!installed) {
            installed = true;
            SqlPlatform.install(INSTANCE);
        }
    }

    @Override
    public SqlSocket connect(String host, int port, int timeoutMillis) throws IOException {
        if (!Socket.isSupported()) {
            throw new IOException("This platform has no TCP sockets");
        }
        Pending pending = new Pending(timeoutMillis);
        try {
            Socket.connect(host, port, pending);
        } catch (IllegalArgumentException e) {
            // Socket refuses a host holding both '.' and ':' (an IPv4-mapped IPv6
            // literal); the engines handle IOException only.
            IOException io = new IOException("Cannot connect to " + host + ": " + e.getMessage());
            io.initCause(e);
            throw io;
        }
        return pending.await(host, port, timeoutMillis);
    }

    @Override
    public byte[] randomBytes(int length) {
        return CryptoEngine.random(length);
    }

    /// [Socket]'s callback, waited for on the connecting thread. Once connected it
    /// keeps the first error the streams report: [Socket]'s output stream reports a
    /// failed write here instead of throwing.
    private static final class Pending extends SocketConnection {
        private InputStream in;
        private OutputStream out;
        private String error;
        private boolean done;
        private boolean abandoned;

        Pending(int timeoutMillis) {
            super(timeoutMillis);
        }

        @Override
        public synchronized void connectionError(int errorCode, String message) {
            if (error == null) {
                error = message == null ? "error " + errorCode : message;
            }
            done = true;
            notifyAll();
        }

        @Override
        public synchronized void connectionEstablished(InputStream is, OutputStream os) {
            if (abandoned) {
                // The caller timed out and left; nobody will ever close these.
                Util.cleanup(is);
                Util.cleanup(os);
                return;
            }
            in = is;
            out = os;
            done = true;
            notifyAll();
        }

        synchronized String failure() {
            return in == null ? null : error;
        }

        synchronized SqlSocket await(String host, int port, int timeoutMillis)
                throws IOException {
            long deadline = timeoutMillis > 0 ? System.currentTimeMillis() + timeoutMillis : 0;
            while (!done) {
                long wait = 1000;
                if (deadline != 0) {
                    wait = deadline - System.currentTimeMillis();
                    if (wait <= 0) {
                        break;
                    }
                }
                try {
                    wait(wait);
                } catch (InterruptedException e) {
                    abandoned = true;
                    IOException io = new IOException("Interrupted connecting to " + host + ":" + port);
                    io.initCause(e);
                    throw io;
                }
            }
            if (!done) {
                abandoned = true;
                throw new IOException("Timed out connecting to " + host + ":" + port);
            }
            if (in == null) {
                // The platform reports no reason: refused, unreachable or an unknown host.
                throw new IOException("Could not connect to " + host + ":" + port
                        + " (refused, unreachable or unknown host): " + error);
            }
            return new StreamSocket(this, in, out, timeoutMillis);
        }
    }

    private static final class StreamSocket extends SqlSocket {
        private final Pending connection;
        private final InputStream in;
        private final OutputStream out;
        private int readTimeout;
        private boolean closed;

        /// Until the engine sets its own, a read waits no longer than connecting
        /// may, so a peer that accepts and then says nothing cannot hang sign-in.
        StreamSocket(Pending connection, InputStream in, OutputStream out, int timeoutMillis) {
            this.connection = connection;
            this.in = in;
            this.out = out;
            this.readTimeout = timeoutMillis;
        }

        private void check() throws IOException {
            String failure = connection.failure();
            if (failure != null) {
                close();
                throw new IOException("Connection failed: " + failure);
            }
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            check();
            if (readTimeout > 0) {
                long deadline = System.currentTimeMillis() + readTimeout;
                while (!closed && in.available() <= 0) {
                    check();
                    if (System.currentTimeMillis() >= deadline) {
                        close();
                        throw new IOException("Timed out after " + readTimeout
                                + "ms waiting for the server");
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        close();
                        IOException io = new IOException("Interrupted waiting for the server");
                        io.initCause(e);
                        throw io;
                    }
                }
            }
            return in.read(buffer, offset, length);
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            check();
            out.write(buffer, offset, length);
            out.flush();
            check();
        }

        /// A deadline on each wait for the server, polled: these sockets have no
        /// timeout of their own. 0 waits as long as the server takes.
        @Override
        public void setReadTimeout(int millis) {
            readTimeout = millis;
        }

        @Override
        public void startTls(String host, String caFile, boolean verifyHost, String alpn)
                throws IOException {
            throw new IOException("TLS is not available on this platform's sockets");
        }

        @Override
        public void startTlsTunnelled(String host, String caFile, boolean verifyHost,
                                      int packetType) throws IOException {
            throw new IOException("TLS is not available on this platform's sockets");
        }

        @Override
        public void close() {
            closed = true;
            Util.cleanup(in);
            Util.cleanup(out);
        }
    }
}
