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
            // What the generated router answers itself, and what a handler does.
            if ("/api/decode".equals(path)) {
                return request.frameworkError(400, "malformed body");
            }
            if ("/api/type".equals(path)) {
                return request.frameworkError(415, "Unsupported Media Type");
            }
            if ("/api/own".equals(path)) {
                return request.respond(400, "text/plain", bytes("mine"));
            }
            if ("/api/boom".equals(path)) {
                throw new IllegalStateException("boom");
            }
            if ("/api/teapot".equals(path)) {
                throw new ResponseStatusException(418, "short and stout");
            }
            if ("/api/get-only".equals(path)) {
                // A route for GET alone, as a generated router declines others.
                return "GET".equals(request.getMethod())
                        ? new HttpServer.Response(200, "text/plain", bytes("got")) : null;
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
            Object thrown = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
            return new HttpServer.Response(((Integer) status).intValue(), "application/json",
                    bytes("{\"status\":" + status + ",\"path\":\"" + uri + "\",\"message\":\""
                            + request.getAttribute(RequestDispatcher.ERROR_MESSAGE) + "\""
                            + (thrown == null ? "" : ",\"exception\":\"" + thrown.getClass().getSimpleName() + "\"")
                            + "}"));
        }
    };

    /// The same application as a generated router presents itself: a
    /// controller's declared error route is the one errors are handed to.
    private static final class Controller implements HttpServer.Handler,
            com.codename1.backend.web.ControllerHooks.Invocation {
        /// 0: /error takes any method; 1: GET only, as @GetMapping("/error");
        /// 2: the error page itself throws.
        private final int errorRoute;
        Controller() {
            this(0);
        }
        Controller(int errorRoute) {
            this.errorRoute = errorRoute;
        }
        public HttpServer.Response handle(HttpServer.Request request) throws Exception {
            if ("/error".equals(request.pathFrom(0))) {
                if (errorRoute == 1 && !"GET".equals(request.getMethod())) {
                    return null;
                }
                if (errorRoute == 2) {
                    throw new IllegalStateException("the error page is broken");
                }
            }
            return APP.handle(request);
        }
        public HttpServer.Response proceed(int route, HttpServer.Request request, String[] bound) {
            throw new UnsupportedOperationException();
        }
    }

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
    @DisplayName("the router's own errors and escaping exceptions reach the error route")
    void frameworkErrorsAndExceptionsReachTheErrorRoute(@TempDir File dir) throws Exception {
        Backend backend = Backend.builder(Config.of(settings(dir), "test")).quiet().handler(new Controller()).start();
        try {
            Reply decode = send("POST", "/api/decode", "application/json");
            assertEquals(400, decode.status);
            assertTrue(decode.body.contains("\"path\":\"/api/decode\""), decode.body);
            assertTrue(decode.body.contains("\"message\":\"malformed body\""), decode.body);
            assertFalse(decode.body.contains("exception"), decode.body);

            Reply type = send("POST", "/api/type", "application/json");
            assertEquals(415, type.status);
            assertTrue(type.body.contains("\"status\":415"), type.body);

            // A handler's own error is its answer, not the error page's.
            Reply own = send("POST", "/api/own", "application/json");
            assertEquals(400, own.status);
            assertEquals("mine", own.body);

            Reply boom = send("GET", "/api/boom", "application/json");
            assertEquals(500, boom.status);
            assertTrue(boom.body.contains("\"exception\":\"IllegalStateException\""), boom.body);

            Reply teapot = send("GET", "/api/teapot", "application/json");
            assertEquals(418, teapot.status);
            assertTrue(teapot.body.contains("\"message\":\"short and stout\""), teapot.body);

            // A method the route does not take is a path nothing serves: a 404.
            Reply method = send("DELETE", "/api/get-only", "application/json");
            assertEquals(404, method.status);
            assertTrue(method.body.contains("\"path\":\"/api/get-only\""), method.body);

            // On one kept-alive connection, a router error then a handler's own
            // 400: the reused Response must not carry the mark into the second.
            String[] both = twoOnOneConnection("/api/decode", "/api/own");
            assertTrue(both[0].startsWith("HTTP/1.1 400") && both[0].contains("\"path\":\"/api/decode\""), both[0]);
            assertTrue(both[1].startsWith("HTTP/1.1 400") && both[1].endsWith("mine"), both[1]);
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("without an error route the router's answer and the plain 500 stand")
    void withoutAnErrorRouteTheOwnAnswersStand(@TempDir File dir) throws Exception {
        Properties settings = settings(dir);
        settings.setProperty(Config.ERROR_PATH, "");
        Backend backend = Backend.builder(Config.of(settings, "test")).quiet().handler(new Controller()).start();
        try {
            Reply decode = send("POST", "/api/decode", "application/json");
            assertEquals(400, decode.status);
            assertEquals("malformed body", decode.body);
            assertEquals(500, send("GET", "/api/boom", null).status);
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a GET-only error route still answers a failed POST")
    void aGetOnlyErrorRouteAnswersAFailedPost(@TempDir File dir) throws Exception {
        Backend backend = Backend.builder(Config.of(settings(dir), "test")).quiet().handler(new Controller(1)).start();
        try {
            Reply boom = send("POST", "/api/boom", "application/json");
            assertEquals(500, boom.status);
            assertTrue(boom.body.contains("\"exception\":\"IllegalStateException\""), boom.body);
            Reply decode = send("POST", "/api/decode", "application/json");
            assertEquals(400, decode.status);
            assertTrue(decode.body.contains("\"path\":\"/api/decode\""), decode.body);
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a failing error page leaves the original status standing")
    void aFailingErrorPageLeavesTheOriginalStatus(@TempDir File dir) throws Exception {
        Backend backend = Backend.builder(Config.of(settings(dir), "test")).quiet().handler(new Controller(2)).start();
        try {
            assertEquals(500, send("GET", "/api/boom", null).status);
            // Serving a 404, the page's own failure leaves the 404.
            assertEquals(404, send("GET", "/nowhere", null).status);
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a hand-written handler at the error path does not take over errors")
    void aCatchAllHandlerIsNotAnErrorRoute(@TempDir File dir) throws Exception {
        Backend backend = Backend.builder(Config.of(settings(dir), "test")).quiet().handler(APP).start();
        try {
            Reply decode = send("POST", "/api/decode", "application/json");
            assertEquals(400, decode.status);
            assertEquals("malformed body", decode.body);
            assertEquals(500, send("GET", "/api/boom", null).status);
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
        return send("GET", path, accept);
    }

    /// Two GETs written on one socket, each answer read by its Content-Length.
    private String[] twoOnOneConnection(String first, String second) throws Exception {
        java.net.Socket socket = new java.net.Socket("127.0.0.1", port);
        try {
            socket.setSoTimeout(5000);
            java.io.OutputStream out = socket.getOutputStream();
            java.io.DataInputStream in = new java.io.DataInputStream(socket.getInputStream());
            String[] answers = new String[2];
            String[] paths = {first, second};
            for (int i = 0; i < 2; i++) {
                out.write(bytes("POST " + paths[i] + " HTTP/1.1\r\nHost: x\r\nAccept: application/json\r\nContent-Length: 0\r\n\r\n"));
                out.flush();
                StringBuilder head = new StringBuilder();
                while (!head.toString().endsWith("\r\n\r\n")) {
                    head.append((char) in.readUnsignedByte());
                }
                int length = 0;
                for (String line : head.toString().split("\r\n")) {
                    if (line.toLowerCase(java.util.Locale.ROOT).startsWith("content-length:")) {
                        length = Integer.parseInt(line.substring(15).trim());
                    }
                }
                byte[] body = new byte[length];
                in.readFully(body);
                answers[i] = head.toString().trim() + "\n" + new String(body, "UTF-8");
            }
            return answers;
        } finally {
            socket.close();
        }
    }

    private Reply send(String method, String path, String accept) throws Exception {
        HttpURLConnection connection = (HttpURLConnection)
                new URL("http://127.0.0.1:" + port + path).openConnection();
        connection.setRequestMethod(method);
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
