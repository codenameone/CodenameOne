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

import com.codename1.backend.web.RequestDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Issue 6310: a path nothing serves is dispatched to the application's error
/// route, as Spring Boot forwards it to its ErrorController, so one app keeps a
/// JSON 404 for API clients and a rendered page for browsers.
class ErrorPageDispatchTest {

    /// An error controller written the way a Boot one is: it reads the status
    /// and the original path from the request attributes and picks HTML only
    /// for a client that asked for it.
    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        public HttpServer.Response handle(HttpServer.Request request) throws Exception {
            String path = request.getTarget();
            if ("/api/ok".equals(path)) {
                return new HttpServer.Response(200, "text/plain", bytes("fine"));
            }
            if (!"/error".equals(path)) {
                return null;
            }
            Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
            Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
            String accept = request.getHeader("Accept");
            if (accept != null && accept.indexOf("text/html") >= 0) {
                return new HttpServer.Response(200, "text/html",
                        bytes("<html><h2>Page not found</h2>" + uri + "</html>"));
            }
            return new HttpServer.Response(((Integer) status).intValue(), "application/json",
                    bytes("{\"status\":" + status + ",\"path\":\"" + uri + "\"}"));
        }
    };

    @Test
    @DisplayName("an unmatched path reaches the error route as JSON or as a page")
    void unmatchedPathReachesTheErrorRoute(@TempDir File dir) throws Exception {
        // A static directory too: its plain-text 404 is what used to answer.
        Properties settings = settings(dir);
        Backend backend = Backend.builder(Config.of(settings, "test")).quiet().handler(APP).start();
        try {
            Reply json = get("/no-such-page-anywhere?x=1", "application/json");
            assertEquals(404, json.status);
            assertTrue(json.body.contains("\"status\":404"), json.body);
            assertTrue(json.body.contains("\"path\":\"/no-such-page-anywhere\""), json.body);
            assertFalse(json.body.contains("<html"), json.body);

            Reply page = get("/no-such-page-anywhere", "text/html");
            // The page set no status, so it keeps the one the request earned.
            assertEquals(404, page.status);
            assertTrue(page.body.contains("Page not found"), page.body);

            // A file missing under the static prefix is the same: nothing served it.
            Reply file = get("/static/missing.css", "application/json");
            assertEquals(404, file.status);
            assertTrue(file.body.contains("\"path\":\"/static/missing.css\""), file.body);

            // A served route is untouched, and so is the error route asked directly.
            assertEquals("fine", get("/api/ok", null).body);
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("an empty error path keeps the server's own 404")
    void anEmptyErrorPathTurnsTheDispatchOff(@TempDir File dir) throws Exception {
        Properties settings = settings(dir);
        settings.setProperty(Config.ERROR_PATH, "");
        Backend backend = Backend.builder(Config.of(settings, "test")).quiet().handler(APP).start();
        try {
            Reply reply = get("/no-such-page-anywhere", "application/json");
            assertEquals(404, reply.status);
            assertFalse(reply.body.contains("\"status\""), reply.body);
        } finally {
            backend.stop();
        }
    }

    private int port;

    private Properties settings(File dir) throws Exception {
        Properties settings = new Properties();
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();
        settings.setProperty(Config.SERVER_PORT, String.valueOf(port));
        settings.setProperty(Config.STATIC_ROOT, dir.getAbsolutePath());
        settings.setProperty(Config.WEBAPP_ENABLED, "false");
        FileOutputStream out = new FileOutputStream(new File(dir, "present.css"));
        out.write(bytes("p{}"));
        out.close();
        return settings;
    }

    private static byte[] bytes(String text) {
        try {
            return text.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException(err);
        }
    }

    private static final class Reply {
        int status;
        String body = "";
    }

    private Reply get(String path, String accept) throws Exception {
        HttpURLConnection connection = (HttpURLConnection)
                new URL("http://127.0.0.1:" + port + path).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        if (accept != null) {
            connection.setRequestProperty("Accept", accept);
        }
        try {
            Reply reply = new Reply();
            reply.status = connection.getResponseCode();
            InputStream in = reply.status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (in != null) {
                ByteArrayOutputStream all = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                for (int n = in.read(buffer); n > 0; n = in.read(buffer)) {
                    all.write(buffer, 0, n);
                }
                reply.body = new String(all.toByteArray(), "UTF-8");
            }
            return reply;
        } finally {
            connection.disconnect();
        }
    }
}
