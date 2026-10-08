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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Holds the chain's [HeaderWriter]s. They run when the response is known --
/// including the server's own 404 and 500 -- against the server's copy of it.
public final class HeaderWriterFilter implements SecurityFilter {
    private final List<HeaderWriter> writers;

    HeaderWriterFilter(List<HeaderWriter> writers) {
        this.writers = Collections.unmodifiableList(new ArrayList<HeaderWriter>(writers));
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        return chain.doFilter(request);
    }

    void write(HttpServer.Request request, HeaderWriter.Headers headers) {
        for (HeaderWriter writer : writers) {
            writer.writeHeaders(request, headers);
        }
    }
}
