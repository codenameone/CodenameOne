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
 * Server-side TLS. One context for the process, one session per connection.
 *
 * The handshake runs on the worker that picks a connection up, not on the reactor
 * thread: a handshake is several round trips, and doing it on the reactor would
 * block every other connection behind one slow client.
 *
 * A TLS connection costs an SSL object, so the "an idle connection allocates
 * nothing" property of the plain server does not hold here -- that is inherent to
 * TLS, not a choice. It is also why static files lose their zero-copy path under
 * TLS: sendfile works because the kernel moves bytes it never looks at, and
 * encrypted bytes have to be produced in user space.
 */
public final class Tls {
    private long context;

    private Tls(long context) {
        this.context = context;
    }

    /**
     * - `certPath`: PEM certificate chain, leaf first
     * - `keyPath`: PEM private key
     */
    public static Tls create(String certPath, String keyPath) throws IOException {
        return create(certPath, keyPath, false);
    }

    /**
     * - `offerHttp2`: advertise "h2" in ALPN. There is no upgrade handshake for
     *   HTTP/2 over TLS, so a server that does not advertise it here will never
     *   speak it however complete the rest of its implementation is.
     */
    public static Tls create(String certPath, String keyPath, boolean offerHttp2) throws IOException {
        long ctx = createContextImpl(certPath, keyPath, offerHttp2);
        if(ctx == 0) {
            throw new IOException("Could not load the certificate and key from "
                    + certPath + " and " + keyPath);
        }
        return new Tls(ctx);
    }

    /**
     * Runs the handshake on an already-blocking descriptor. Returns 0 when it
     * fails, which is ordinary traffic -- a scanner, a client with no common
     * cipher, or a plaintext request sent to the TLS port.
     */
    /**
     * Runs the handshake, giving it `budgetMillis` in total.
     *
     * <p>A TOTAL budget, because SO_RCVTIMEO bounds one read and a handshake is
     * several: a client that delivers a byte just inside each timeout never
     * causes one, so the handshake could be held open for as long as it cared to
     * drip-feed -- occupying a worker throughout, before the request head's own
     * deadline is armed. Zero or less means unbounded, which is what the
     * platforms with no poll in this backend get.
     */
    public long accept(int fd, long budgetMillis) {
        return acceptImpl(context, fd, budgetMillis);
    }

    public void close() {
        if(context != 0) {
            long c = context;
            context = 0;
            freeContextImpl(c);
        }
    }

    /** -1 at end of stream, as InputStream does. */
    static int read(long session, byte[] buffer, int offset, int length) throws IOException {
        checkRange(buffer, offset, length);
        int n = readImpl(session, buffer, offset, length);
        if(n < -1) {
            throw new IOException("TLS read failed");
        }
        return n;
    }

    static void write(long session, byte[] buffer, int offset, int length) throws IOException {
        checkRange(buffer, offset, length);
        if(writeImpl(session, buffer, offset, length) != length) {
            throw new IOException("TLS write failed");
        }
    }

    static void closeSession(long session) {
        if(session != 0) {
            closeImpl(session);
        }
    }

    /** The protocol ALPN settled on: "h2", "http/1.1", or null. */
    public static String negotiatedProtocol(long session) {
        return negotiatedProtocolImpl(session);
    }

    private static native long createContextImpl(String certPath, String keyPath, boolean offerHttp2);
    private static native String negotiatedProtocolImpl(long session);
    private static native void freeContextImpl(long handle);
    private static native long acceptImpl(long context, int fd, long budgetMillis);

    /**
     * Refuses a slice that does not lie inside the array.
     *
     * The natives below index the array through the pointer they are handed and
     * ParparVM adds no bounds check of its own, so a bad offset is a native read
     * or write of whatever is next in the heap rather than an exception. The
     * JavaSE arm gets this free from its stream APIs, which is why such a bug is
     * invisible on the simulator and only appears once packaged. The subtraction
     * avoids the overflow that `offset + length` has.
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

    private static native int readImpl(long session, byte[] buffer, int offset, int length);
    private static native int writeImpl(long session, byte[] buffer, int offset, int length);
    private static native void closeImpl(long session);
}
