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
        Socket.connect(host, port, pending);
        return pending.await(host, port, timeoutMillis);
    }

    @Override
    public byte[] randomBytes(int length) {
        return CryptoEngine.random(length);
    }

    /// Waits for [Socket]'s callback on the connecting thread.
    private static final class Pending extends SocketConnection {
        private InputStream in;
        private OutputStream out;
        private String error;
        private boolean done;

        Pending(int timeoutMillis) {
            super(timeoutMillis);
        }

        @Override
        public synchronized void connectionError(int errorCode, String message) {
            error = message == null ? "error " + errorCode : message;
            done = true;
            notifyAll();
        }

        @Override
        public synchronized void connectionEstablished(InputStream is, OutputStream os) {
            in = is;
            out = os;
            done = true;
            notifyAll();
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
                    IOException io = new IOException("Interrupted connecting to " + host + ":" + port);
                    io.initCause(e);
                    throw io;
                }
            }
            if (!done) {
                throw new IOException("Timed out connecting to " + host + ":" + port);
            }
            if (in == null) {
                throw new IOException("Could not connect to " + host + ":" + port + ": " + error);
            }
            return new StreamSocket(in, out);
        }
    }

    private static final class StreamSocket extends SqlSocket {
        private final InputStream in;
        private final OutputStream out;

        StreamSocket(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            return in.read(buffer, offset, length);
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            out.write(buffer, offset, length);
            out.flush();
        }

        /// These sockets have no per-read timeout; a read waits for the server.
        @Override
        public void setReadTimeout(int millis) {
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
            Util.cleanup(in);
            Util.cleanup(out);
        }
    }
}
