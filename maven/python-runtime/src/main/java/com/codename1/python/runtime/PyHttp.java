/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.python.runtime;

import java.io.IOException;

/// The HTTP client `requests`, `httpx` and `urllib.request` are written over.
///
/// An application sends through Codename One's `ConnectionRequest` unless
/// something else is installed: the server's own client on the backend, a
/// stub in a test. A request is synchronous; on the EDT the network manager
/// keeps the UI running while it waits.
public abstract class PyHttp {
    private static PyHttp current;

    /// The client in use.
    public static PyHttp get() {
        if (current == null) {
            current = new Cn1Http();
        }
        return current;
    }

    /// Replaces the client, for a host that is not a Codename One
    /// application.
    public static void install(PyHttp http) {
        current = http;
    }

    /// A response, of any status.
    public static final class Response {
        public final int status;
        public final String reason;
        /// The URL that answered, after redirects.
        public final String url;
        /// Header names and values, alternating, in the order received; a
        /// header sent twice appears twice.
        public final String[] headers;
        public final byte[] body;

        public Response(int status, String reason, String url, String[] headers, byte[] body) {
            this.status = status;
            this.reason = reason;
            this.url = url;
            this.headers = headers;
            this.body = body;
        }
    }

    /// Sends one request.
    ///
    /// @param headers names and values, alternating
    /// @param body the request body, or null
    /// @param timeoutMillis 0 for the client's default
    /// @throws IOException when no response arrived (the host could not be
    ///         reached, the connection failed or timed out)
    public abstract Response send(String method, String url, String[] headers, byte[] body, int timeoutMillis,
            boolean followRedirects) throws IOException;
}
