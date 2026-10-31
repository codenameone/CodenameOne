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
package com.codename1.python.transpiler;

import com.codename1.python.runtime.PyHttp;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// HTTP for the conformance programs, over the JDK's client: what a
/// Codename One application gets from `ConnectionRequest`.
public final class JvmHttp extends PyHttp {
    private static final Map<Integer, String> REASONS = Map.ofEntries(Map.entry(200, "OK"),
            Map.entry(201, "Created"), Map.entry(204, "No Content"), Map.entry(301, "Moved Permanently"),
            Map.entry(302, "Found"), Map.entry(400, "Bad Request"), Map.entry(401, "Unauthorized"),
            Map.entry(403, "Forbidden"), Map.entry(404, "Not Found"), Map.entry(418, "I'm a Teapot"),
            Map.entry(500, "Internal Server Error"), Map.entry(503, "Service Unavailable"));
    private static final java.util.Set<String> RESTRICTED = java.util.Set.of("connection", "content-length",
            "host", "expect", "upgrade");

    @Override
    public Response send(String method, String url, String[] headers, byte[] body, int timeoutMillis,
            boolean followRedirects) throws IOException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(followRedirects ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofMillis(timeoutMillis > 0 ? timeoutMillis : 10000)).build();
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofByteArray(body));
        for (int i = 0; i + 1 < headers.length; i += 2) {
            if (!RESTRICTED.contains(headers[i].toLowerCase(java.util.Locale.ROOT))) {
                b.header(headers[i], headers[i + 1]);
            }
        }
        if (timeoutMillis > 0) {
            b.timeout(Duration.ofMillis(timeoutMillis));
        }
        HttpResponse<byte[]> r;
        try {
            r = client.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted", e);
        }
        List<String> hs = new ArrayList<String>();
        for (Map.Entry<String, List<String>> e : r.headers().map().entrySet()) {
            if (e.getKey().startsWith(":")) {
                continue;
            }
            for (String v : e.getValue()) {
                hs.add(e.getKey());
                hs.add(v);
            }
        }
        return new Response(r.statusCode(), REASONS.getOrDefault(r.statusCode(), ""), r.uri().toString(),
                hs.toArray(new String[0]), r.body());
    }
}
