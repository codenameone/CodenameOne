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
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

/**
 * HTTP/2 is deliberately absent from the local Java SE runtime.
 *
 * h2 is only ever reached through ALPN on a TLS connection, and the local runtime
 * does not terminate TLS (see Tls), so this could not be entered even if it were
 * implemented. Keeping the shape and refusing at create() means the shared server
 * code above needs no target-specific branch.
 */
public final class Http2 {
    /** The ALPN protocol identifier, needed by the shared code even here. */
    public static final String ALPN = "h2";

    private static final String UNSUPPORTED =
            "HTTP/2 is not available in the local Java SE runtime -- it is reached "
            + "through ALPN over TLS, which the local runtime does not terminate";

    private Http2() {
    }

    public static Http2 create() throws IOException {
        throw new IOException(UNSUPPORTED);
    }

    /** Mirrors the native runtime's stream shape so shared code compiles. */
    public static final class Stream {
        final int id;
        final String method;
        final String path;
        final String authority;
        final Map headers;
        final byte[] body;

        Stream(int id, String method, String path, String authority, Map headers, byte[] body) {
            this.id = id;
            this.method = method;
            this.path = path;
            this.authority = authority;
            this.headers = headers;
            this.body = body;
        }

        public int getId() {
            return id;
        }

        public String getMethod() {
            return method;
        }

        public String getPath() {
            return path;
        }

        public String getAuthority() {
            return authority;
        }

        public Map getHeaders() {
            return headers;
        }

        public String getBodyAsString() {
            if(body == null || body.length == 0) {
                return null;
            }
            try {
                return new String(body, "UTF-8");
            } catch (UnsupportedEncodingException err) {
                return new String(body);
            }
        }
    }

    public void receive(byte[] buffer, int offset, int length) throws IOException {
        throw new IOException(UNSUPPORTED);
    }

    public Stream nextRequest() {
        return null;
    }

    /**
     * As {@link #respond}, with the body read from a descriptor rather than the heap.
     * Unsupported here for the same reason the rest of this class is: the local run
     * does not terminate TLS, so it never speaks HTTP/2.
     */
    public void respondFile(int streamId, int status, String contentType, List extraHeaders,
            int fd, long offset, long length) throws IOException {
        throw new IOException(UNSUPPORTED);
    }

    public void respond(int streamId, int status, String contentType, List extraHeaders,
            byte[] body) throws IOException {
        throw new IOException(UNSUPPORTED);
    }

    public byte[] drain() throws IOException {
        throw new IOException(UNSUPPORTED);
    }

    public boolean isAlive() {
        return false;
    }

    /** Nothing is ever submitted here, so nothing is ever outstanding. */
    public long pendingBodyBytes() {
        return 0;
    }

    /** Likewise: no session, so no file-backed body holds a descriptor. */
    public static int pendingBodyFiles() {
        return 0;
    }

    /** And nothing is submitted, so no body holds heap either. */
    public static long pendingBodyBytesAll() {
        return 0;
    }

    public void close() {
    }
}
