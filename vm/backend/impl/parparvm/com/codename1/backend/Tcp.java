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
        // The same range ServerSocket.bind refuses, and for the same reason: the
        // native side renders this into the service string getaddrinfo parses, and
        // a value past 65535 does not fail there -- glibc wraps it, so 65536 dials
        // port 0 and 99999 dials 34463. The Java SE arm rejects it outright, so a
        // malformed database URL reached a DIFFERENT port only once packaged.
        if(port < 0 || port > 65535) {
            throw new IllegalArgumentException("port out of range: " + port);
        }
        // And the timeout, for the same reason one line up: the native side reads
        // every NON-POSITIVE value as "block with no deadline", so a negative one
        // hangs a packaged server for the OS TCP timeout while the Java SE arm
        // fails immediately out of Socket.connect. Zero keeps its documented
        // meaning; below zero is not a shorter wait, it is a different API.
        if(timeoutMillis < 0) {
            throw new IllegalArgumentException("connect timeout must not be negative: "
                    + timeoutMillis);
        }
        // The name reaches a resolver; see Urls.requireHostName for what a NUL
        // in it does to the packaged arm, and why both arms refuse it.
        Urls.requireHostName(host);
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
        // BOTH STRINGS, and the name is the one that decides who the peer is
        // allowed to be. It crosses to a native as a C string, so a NUL inside it
        // ends the name there and OpenSSL verifies the certificate against the
        // prefix alone: "attacker.example\0.trusted.example" satisfies a caller's
        // own endsWith(".trusted.example") check and then verifies as
        // "attacker.example". connect() validating the address it dialled does not
        // cover this, because the verification name is supplied separately and may
        // come from somewhere else entirely.
        Urls.requireHostName(host);
        // The trust root is a file name that crosses to a native; see
        // Urls.requireNoNul for what a NUL in it loads instead.
        Urls.requireNoNul("An sslrootcert path", caFile);
        checkOpen();
        if(tls != 0) {
            return;
        }
        long fd;
        synchronized(this) {
            if(handle == 0) {
                throw new IOException("Socket closed");
            }
            fd = handle;
        }
        long session = startTlsImpl(fd, host, caFile);
        if(session == 0) {
            throw new IOException("TLS handshake with " + host + " failed: " + tlsErrorImpl());
        }
        // Published under the same monitor the claim takes, so a thread that goes
        // on to read sees a session rather than a zero and the plain socket.
        //
        // AND RECHECKED, because the handshake above holds no claim -- there is no
        // session to claim yet. A close() during it closes the descriptor, which
        // is what makes the handshake fail; the race that remains is the narrow
        // one where it had just SUCCEEDED. Publishing then would report success on
        // a closed socket and leave an SSL* that only a second close would free --
        // by which time its descriptor number may belong to another connection,
        // and SSL_shutdown would write TLS bytes into that one.
        boolean lost;
        synchronized(this) {
            lost = handle == 0;
            if(!lost) {
                tls = session;
            }
        }
        if(lost) {
            // Freed WITHOUT a shutdown: the descriptor is gone, so there is
            // nothing to tell the peer and nothing safe to write to.
            tlsDiscardImpl(session);
            throw new IOException("Socket closed");
        }
    }

    /**
     * Refuses a slice that does not lie inside the array.
     *
     * recv() and SSL_read() index the array straight through the pointer they are
     * given, and ParparVM adds no bounds check of its own, so a bad offset here is
     * a native read or write of whatever is next in the heap rather than an
     * exception. The JavaSE implementation gets this free from the stream API,
     * which is why the same code is safe on the simulator and unsafe only once it
     * is packaged. The subtraction avoids the overflow `offset + length` has.
     */
    private static void checkRange(byte[] buffer, int offset, int length) {
        if(buffer == null) {
            throw new NullPointerException("buffer");
        }
        if(offset < 0 || length < 0 || length > buffer.length - offset) {
            throw new IndexOutOfBoundsException("offset " + offset + ", length "
                    + length + ", buffer " + buffer.length);
        }
    }

    /** Operations currently inside a native on this connection. */
    private int inFlight;

    /** Whether close() has been called; the fields below are then its leftovers. */
    private boolean closing;

    /** What the last operation out has to free, when close() could not. */
    private long tlsAwaitingClose;
    private long handleAwaitingClose;

    /**
     * Takes a claim on this connection for one operation.
     *
     * <p>Returns the TLS session to use, or 0 to use the plain socket, and leaves
     * the descriptor in `claimed` -- both read under the monitor so they describe
     * the same instant. The claim is what stops close() from releasing either one
     * while this operation is inside a native with it.
     */
    private synchronized long claim(long[] claimed) throws IOException {
        if(handle == 0) {
            throw new IOException("Socket closed");
        }
        inFlight++;
        claimed[0] = handle;
        return tls;
    }

    /**
     * Releases a claim, finishing a close() that was waiting on it.
     *
     * <p>The session is DISCARDED rather than shut down: close() has already
     * shut the socket down, so there is no longer a peer to tell. The descriptor
     * is closed last, and only here -- which is the whole point. Closing it in
     * close() would release the NUMBER while this operation was still inside
     * read() or write() with it, and a number is handed to the next socket this
     * process opens, so the read would be served by an unrelated connection.
     */
    private void release() {
        long discard = 0;
        long closeNow = 0;
        synchronized(this) {
            inFlight--;
            if(inFlight == 0 && closing) {
                discard = tlsAwaitingClose;
                tlsAwaitingClose = 0;
                closeNow = handleAwaitingClose;
                handleAwaitingClose = 0;
            }
        }
        if(discard != 0) {
            tlsDiscardImpl(discard);
        }
        if(closeNow != 0) {
            closeImpl(closeNow);
        }
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
        checkRange(buffer, offset, length);
        // Answered here, never dispatched. InputStream returns 0 for a zero-length
        // read and the Java SE arm inherits that, while recv(_, 0) returns 0 and
        // the native maps a zero-byte read to END OF STREAM -- so a caller that
        // computed an empty slice was told the peer had gone away, but only once
        // packaged. SSL_read(_, 0) is worse: OpenSSL leaves it undefined and it
        // can report an error.
        if(length == 0) {
            return 0;
        }
        long[] claimed = new long[1];
        long session = claim(claimed);
        int n;
        try {
            n = session == 0 ? readImpl(claimed[0], buffer, offset, length)
                             : tlsReadImpl(session, buffer, offset, length);
        } finally {
            release();
        }
        if(n < -1) {
            throw new IOException("Socket read failed");
        }
        return n;
    }

    public void write(byte[] buffer, int offset, int length) throws IOException {
        checkOpen();
        checkRange(buffer, offset, length);
        // Symmetry with read, and for the same reason on the TLS side:
        // SSL_write(_, 0) is undefined too. This one happens to be harmless today
        // -- the check below is n != length, and 0 != 0 is false -- which is a
        // reason to make it explicit rather than to leave it resting on that.
        if(length == 0) {
            return;
        }
        long[] claimed = new long[1];
        long session = claim(claimed);
        int n;
        try {
            n = session == 0 ? writeImpl(claimed[0], buffer, offset, length)
                             : tlsWriteImpl(session, buffer, offset, length);
        } finally {
            release();
        }
        if(n != length) {
            throw new IOException("Socket write failed");
        }
    }

    /**
     * Closes the connection, and does not release anything an operation still
     * holds.
     *
     * <p>When nothing is in flight this is the plain teardown it always was: shut
     * the session down politely, close the descriptor, done.
     *
     * <p>When something IS in flight -- the ordinary case, since close() is how a
     * blocked read gets cancelled -- the descriptor is SHUT DOWN rather than
     * closed. That is what wakes the other thread, and it leaves the number
     * allocated to us until that thread returns. Closing it here instead would
     * hand the number back to the process while a read() or write() was still
     * inside a native with it, and the next socket this process opens is given
     * that same number: the read would then be served, silently, by an unrelated
     * connection. The last operation out closes it for real.
     *
     * <p>close() still returns at once either way. It has to: the thread it is
     * cancelling may be one that never comes back.
     */
    public void close() {
        long closeNow = 0;
        long shutDownNow = 0;
        long tlsCloseNow = 0;
        synchronized(this) {
            if(closing) {
                return;                 // idempotent, and only the first one frees
            }
            closing = true;
            if(inFlight == 0) {
                tlsCloseNow = tls;
                closeNow = handle;
            } else {
                tlsAwaitingClose = tls;
                handleAwaitingClose = handle;
                shutDownNow = handle;
            }
            tls = 0;
            handle = 0;
        }
        if(shutDownNow != 0) {
            shutdownImpl(shutDownNow);
        }
        if(tlsCloseNow != 0) {
            tlsCloseImpl(tlsCloseNow);
        }
        if(closeNow != 0) {
            closeImpl(closeNow);
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

    /**
     * shutdown(2) on the descriptor: wakes whatever is blocked on it WITHOUT
     * giving the number back to the process. See close().
     */
    private static native void shutdownImpl(long handle);

    /** Frees a session whose descriptor has already gone; see startTls. */
    private static native void tlsDiscardImpl(long session);
}
