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
package com.codename1.backend;

import java.io.IOException;

/**
 * Blocking TCP client socket for server-side (clean-target) binaries. Deliberately
 * not com.codename1.io.Socket: that routes through CodenameOneImplementation, which
 * a translated server binary does not have.
 */
public final class Tcp {
    private long handle;
    /** An OpenSSL session once startTls has run; 0 while the socket is plaintext. */
    private long tls;

    private Tcp(long handle) {
        this.handle = handle;
    }

    public static Tcp connect(String host, int port, int timeoutMillis) throws IOException {
        long h = connectImpl(host, port, timeoutMillis);
        if(h == 0) {
            throw new IOException("Connection to " + host + ":" + port + " failed");
        }
        return new Tcp(h);
    }

    /**
     * Upgrades this connection to TLS, verifying the peer certificate against the
     * system trust store and against `host`.
     *
     * An upgrade rather than a secure connect because that is the shape the
     * database protocols need: PostgreSQL and MySQL both begin in plaintext and
     * ask to start TLS mid-conversation, so a connect-time flag could not express
     * it. Calling it immediately after connect gives the ordinary secure-connect
     * behaviour.
     */
    public void startTls(String host) throws IOException {
        startTls(host, null);
    }

    /**
     * As {@link #startTls(String)}, verifying against the PEM bundle at `caFile`
     * INSTEAD of the system trust store.
     *
     * This is what a managed database needs: RDS, Cloud SQL and the like present
     * certificates from a private CA, and a development container presents one it
     * generated for itself. Falling back to the system store when the named bundle
     * fails to load would verify against roots the caller deliberately did not
     * choose, so that is an error rather than a fallback.
     */
    public void startTls(String host, String caFile) throws IOException {
        checkOpen();
        if(tls != 0) {
            return;
        }
        long session = startTlsImpl(handle, host, caFile);
        if(session == 0) {
            throw new IOException("TLS handshake with " + host + " failed: " + tlsErrorImpl());
        }
        tls = session;
    }

    /** Whether this connection is encrypted. */
    public boolean isSecure() {
        return tls != 0;
    }

    /**
     * Reads up to length bytes. Returns -1 at end of stream, matching InputStream.
     */
    public int read(byte[] buffer, int offset, int length) throws IOException {
        checkOpen();
        int n = tls == 0 ? readImpl(handle, buffer, offset, length)
                         : tlsReadImpl(tls, buffer, offset, length);
        if(n < -1) {
            throw new IOException("Socket read failed");
        }
        return n;
    }

    public void write(byte[] buffer, int offset, int length) throws IOException {
        checkOpen();
        int n = tls == 0 ? writeImpl(handle, buffer, offset, length)
                         : tlsWriteImpl(tls, buffer, offset, length);
        if(n != length) {
            throw new IOException("Socket write failed");
        }
    }

    public void close() {
        if(tls != 0) {
            long t = tls;
            tls = 0;
            tlsCloseImpl(t);
        }
        if(handle != 0) {
            long h = handle;
            handle = 0;
            closeImpl(h);
        }
    }

    private void checkOpen() throws IOException {
        if(handle == 0) {
            throw new IOException("Socket closed");
        }
    }

    private static native long connectImpl(String host, int port, int timeoutMillis);
    private static native int readImpl(long handle, byte[] buffer, int offset, int length);
    private static native int writeImpl(long handle, byte[] buffer, int offset, int length);
    private static native int closeImpl(long handle);
    private static native long startTlsImpl(long handle, String host, String caFile);
    private static native String tlsErrorImpl();
    private static native int tlsReadImpl(long session, byte[] buffer, int offset, int length);
    private static native int tlsWriteImpl(long session, byte[] buffer, int offset, int length);
    private static native void tlsCloseImpl(long session);
}
