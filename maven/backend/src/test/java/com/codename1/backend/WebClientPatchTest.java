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

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/// #6240: HttpURLConnection has a fixed set of verbs and refuses PATCH, so the
/// JVM WebClient sends any other verb through java.net.http over HTTP/1.1.
class WebClientPatchTest {
    @Test
    void patchReachesTheServerWithItsBody() throws Exception {
        assumeFalse(System.getProperty("java.version").startsWith("1."), "java.net.http is Java 11+");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            ByteArrayOutputStream got = new ByteArrayOutputStream();
            try (InputStream in = exchange.getRequestBody()) {
                byte[] b = new byte[256];
                for (int n; (n = in.read(b)) > 0; ) {
                    got.write(b, 0, n);
                }
            }
            byte[] body = (exchange.getRequestMethod() + " " + got.toString("UTF-8")).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        WebClient client = new WebClient();
        try {
            Web.Result result = client.request("PATCH", "http://127.0.0.1:" + server.getAddress().getPort() + "/x",
                    null, "{\"a\":1}".getBytes(StandardCharsets.UTF_8));
            assertEquals(200, result.getStatus());
            assertEquals("PATCH {\"a\":1}", new String(result.getBody(), StandardCharsets.UTF_8));
        } finally {
            client.close();
            server.stop(0);
        }
    }
}
