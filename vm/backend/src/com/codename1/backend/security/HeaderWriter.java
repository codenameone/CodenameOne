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
package com.codename1.backend.security;

import com.codename1.backend.HttpServer;

/// Writes a header onto every response a chain's requests get:
///
/// ```java
/// http.headers(headers -> headers.addHeaderWriter((request, response) ->
///         response.setIfAbsent("Referrer-Policy", "no-referrer")));
/// ```
public interface HeaderWriter {
    void writeHeaders(HttpServer.Request request, Headers response);

    /// The headers of the response being written.
    interface Headers {
        /// Whether the handler, or an earlier writer, set this header.
        boolean contains(String name);

        /// Sets the header, replacing what was there.
        void set(String name, String value);

        /// Sets the header unless the handler already did, which is how a
        /// default leaves a deliberate choice alone.
        void setIfAbsent(String name, String value);

        /// Whether the request arrived over TLS this server terminated.
        boolean isSecure();

        /// The status of the response.
        int getStatus();
    }
}
