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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// A handler that throws [ResponseStatusException] is answered with the status
/// and the reason it chose, on the wire and through the in-process dispatch a
/// test uses, and anything else it throws is still a 500 that says nothing.
class ResponseStatusExceptionTest {

    @Test
    void aClientErrorIsAnsweredWithItsStatusAndReason() throws Exception {
        Backend backend = serve();
        try {
            int port = backend.getServer().getPort();
            assertAnswer(port, "/taken", 409, "That e-mail already has an account");
            // The server is still serving after it: the throw was an answer,
            // not a failure of the connection.
            assertAnswer(port, "/ok", 200, "ok");
        } finally {
            backend.stop();
        }
    }

    @Test
    void aServerErrorIsAnsweredWithItsStatusAndReason() throws Exception {
        Backend backend = serve();
        try {
            int port = backend.getServer().getPort();
            assertAnswer(port, "/down", 503, "The index is rebuilding");
        } finally {
            backend.stop();
        }
    }

    /// The reason of an ordinary exception is the application's, and may hold
    /// anything; only the one thrown to be an answer is sent.
    @Test
    void anyOtherThrowIsStillABareInternalError() throws Exception {
        Backend backend = serve();
        try {
            int port = backend.getServer().getPort();
            assertAnswer(port, "/broken", 500, "internal error");
        } finally {
            backend.stop();
        }
    }

    @Test
    void anAbsentReasonIsAnEmptyBody() throws Exception {
        Backend backend = serve();
        try {
            int port = backend.getServer().getPort();
            assertAnswer(port, "/bare", 404, "");
        } finally {
            backend.stop();
        }
    }

    /// What MockMvc calls. Without the same translation a test of a handler
    /// that refuses a request saw the exception, where a client sees a 409.
    @Test
    void theInProcessDispatchGivesTheSameAnswer() throws Exception {
        Backend backend = serve();
        try {
            HttpServer.Response taken = backend.dispatch(request("/taken"));
            assertEquals(409, taken.getStatus());
            assertEquals("That e-mail already has an account",
                    new String(com.codename1.impl.backend.BackendAccess.get().body(taken), "UTF-8"));
            assertEquals(503, backend.dispatch(request("/down")).getStatus());
            assertEquals(200, backend.dispatch(request("/ok")).getStatus());
        } finally {
            backend.stop();
        }
    }

    /// The request log records what the client was sent. A chosen 409 used to
    /// be logged as a 500 carrying the exception, so every refused sign-up read
    /// as a failure of the server; a chosen 503 is one, and says why.
    @Test
    void theRequestLogRecordsTheChosenStatus() throws Exception {
        Backend backend = serve();
        try {
            backend.getRequestLog().enable(8);
            int port = backend.getServer().getPort();
            assertAnswer(port, "/taken", 409, "That e-mail already has an account");
            assertAnswer(port, "/down", 503, "The index is rebuilding");
            assertAnswer(port, "/broken", 500, "internal error");

            java.util.List all = backend.getRequestLog().recent(8, false);
            assertEquals(3, all.size(), String.valueOf(all));
            java.util.Map broken = (java.util.Map) all.get(0);
            java.util.Map down = (java.util.Map) all.get(1);
            java.util.Map taken = (java.util.Map) all.get(2);
            assertEquals(Integer.valueOf(409), taken.get("status"));
            assertNull(taken.get("error"), "an answer is not an error");
            assertEquals(Integer.valueOf(503), down.get("status"));
            assertEquals(Integer.valueOf(500), broken.get("status"));
            assertEquals(2, backend.getRequestLog().recent(8, true).size(),
                    "only the two the server failed");
            String why = String.valueOf(down.get("error")) + down.get("causes");
            assertEquals(true, why.contains("The index is rebuilding") && why.contains("disk"), why);
        } finally {
            backend.stop();
        }
    }

    @Test
    void onlyAnErrorStatusIsAccepted() {
        assertThrows(IllegalArgumentException.class, () -> new ResponseStatusException(200, "fine"));
        assertThrows(IllegalArgumentException.class, () -> new ResponseStatusException(399, "moved"));
        assertThrows(IllegalArgumentException.class, () -> new ResponseStatusException(600, "none"));
        assertEquals(400, new ResponseStatusException(400, "bad").getStatus());
        assertEquals(599, new ResponseStatusException(599, "worse").getStatus());
    }

    @Test
    void itCarriesItsCauseAndNeverANullReason() {
        IllegalStateException cause = new IllegalStateException("why");
        ResponseStatusException thrown = new ResponseStatusException(502, "upstream", cause);
        assertSame(cause, thrown.getCause());
        assertEquals("upstream", thrown.getReason());
        assertEquals("502 upstream", thrown.getMessage());
        ResponseStatusException bare = new ResponseStatusException(404, null);
        assertEquals("", bare.getReason());
        assertNull(bare.getCause());
    }

    private static Backend serve() throws Exception {
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, "0");
        return Backend.builder(Config.of(settings, "test"))
                .quiet()
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
                        String path = request.getTarget();
                        if ("/taken".equals(path)) {
                            throw new ResponseStatusException(409,
                                    "That e-mail already has an account");
                        }
                        if ("/down".equals(path)) {
                            throw new ResponseStatusException(503, "The index is rebuilding",
                                    new IOException("disk"));
                        }
                        if ("/bare".equals(path)) {
                            throw new ResponseStatusException(404, null);
                        }
                        if ("/broken".equals(path)) {
                            throw new IllegalStateException("a secret the caller must not read");
                        }
                        return HttpServer.Response.text(200, "ok");
                    }
                })
                .start();
    }

    private static HttpServer.Request request(String target) {
        return new HttpServer.Request("GET", target, "HTTP/1.1", new HashMap(), null);
    }

    private static void assertAnswer(int port, String path, int status, String body)
            throws Exception {
        HttpURLConnection connection = (HttpURLConnection)
                new URL("http://127.0.0.1:" + port + path).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        try {
            assertEquals(status, connection.getResponseCode(), path);
            InputStream in = status < 400 ? connection.getInputStream() : connection.getErrorStream();
            assertEquals(body, in == null ? "" : read(in), path);
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            int n = in.read(chunk);
            while (n > 0) {
                out.write(chunk, 0, n);
                n = in.read(chunk);
            }
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }
}
