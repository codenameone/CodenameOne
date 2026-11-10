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
package com.codename1.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class HttpProxyRewriteTest {
    @Test
    void headersAreRenamedAndStatusesRewritten() throws Exception {
        ServerSocket socket = new ServerSocket(0);
        final int port = socket.getLocalPort();
        socket.close();
        final HttpProxy proxy = new HttpProxy("http://127.0.0.1:" + port)
                .renameRequestHeader("X-Api-Key", "Authorization")
                .renameResponseHeader("X-Upstream", "X-Via")
                .rewriteStatus(401, 403);
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        Backend b = Backend.builder(Config.of(settings, "test")).quiet().handler(
                new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) {
                        if (request.getTarget().startsWith("/proxy")) {
                            return proxy.forward(request, "/up");
                        }
                        return HttpServer.Response.text(401, "auth=" + request.getHeader("Authorization")
                                + " key=" + request.getHeader("X-Api-Key"))
                                .header("X-Upstream", "yes");
                    }
                }).start();
        try {
            HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port
                    + "/proxy").openConnection();
            c.setRequestProperty("X-Api-Key", "secret");
            c.setRequestProperty("Authorization", "Basic old");
            assertEquals(403, c.getResponseCode());
            assertEquals("yes", c.getHeaderField("X-Via"));
            assertNull(c.getHeaderField("X-Upstream"));
            InputStream in = c.getErrorStream();
            byte[] body = new byte[256];
            int n = in.read(body);
            in.close();
            assertEquals("auth=secret key=null", new String(body, 0, n, "UTF-8"));
        } finally {
            b.stop();
        }
    }
}
