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
 * TLS is deliberately absent from the local Java SE runtime.
 *
 * This twin exists so the shared server code compiles and runs unchanged on the
 * JVM; it is the fast edit-run loop, not the deployment target. Terminating TLS
 * here would mean a second, differently-behaving handshake and ALPN
 * implementation (SSLEngine) whose bugs would not be the ones production has --
 * worse than not having it, because it would look like coverage. Run the native
 * binary to exercise TLS; the integration suite does exactly that.
 */
public final class Tls {
    private static final String UNSUPPORTED =
            "TLS is not available in the local Java SE runtime -- run the native "
            + "binary (or set CN1_BACKEND_TLS_CERT only there) to serve HTTPS";

    private Tls() {
    }

    public static Tls create(String certPath, String keyPath) throws IOException {
        throw new IOException(UNSUPPORTED);
    }

    public static Tls create(String certPath, String keyPath, boolean offerHttp2)
            throws IOException {
        throw new IOException(UNSUPPORTED);
    }

    /** The budget the translated arm bounds its handshake with; see that one. */
    public long accept(int fd, long budgetMillis) {
        throw new IllegalStateException(UNSUPPORTED);
    }

    public void close() {
    }

    static int read(long session, byte[] buffer, int offset, int length) throws IOException {
        throw new IOException(UNSUPPORTED);
    }

    static void write(long session, byte[] buffer, int offset, int length) throws IOException {
        throw new IOException(UNSUPPORTED);
    }

    static void closeSession(long session) {
    }

    public static String negotiatedProtocol(long session) {
        return null;
    }
}
