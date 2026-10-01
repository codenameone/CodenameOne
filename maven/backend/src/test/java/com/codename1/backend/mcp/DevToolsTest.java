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
package com.codename1.backend.mcp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Which SQLite pragmas backend_sql runs without write=true. */
class DevToolsTest {

    @Test
    @DisplayName("only pragmas that read run unconfirmed, in either spelling")
    void pragmaAllowList() {
        assertTrue(DevTools.readOnlyPragma("PRAGMA table_info(notes)"));
        assertTrue(DevTools.readOnlyPragma("pragma main.table_list"));
        assertTrue(DevTools.readOnlyPragma("PRAGMA busy_timeout"));
        assertFalse(DevTools.readOnlyPragma("PRAGMA busy_timeout(0)"),
                "a setting given in parentheses was run unconfirmed");
        assertFalse(DevTools.readOnlyPragma("PRAGMA cache_size = 123"));
        assertFalse(DevTools.readOnlyPragma("PRAGMA writable_schema(ON)"));
        assertFalse(DevTools.readOnlyPragma("PRAGMA optimize"));
    }

    @Test
    @DisplayName("with the backend down, the bridge answers under the request's own id")
    void bridgeAnswersUnderTheTopLevelId() throws Exception {
        java.net.ServerSocket probe = new java.net.ServerSocket(0);
        int closed = probe.getLocalPort();
        probe.close();
        java.net.URL url = new java.net.URL("http://127.0.0.1:" + closed + "/mcp");
        String answer = StdioBridge.post(url, null, "{\"jsonrpc\":\"2.0\",\"method\":"
                + "\"tools/call\",\"params\":{\"arguments\":{\"id\":\"nested\"}},\"id\":7}");
        assertTrue(answer != null && answer.contains("\"id\":7") && !answer.contains("nested"),
                String.valueOf(answer));
        assertTrue(StdioBridge.post(url, null, "{\"jsonrpc\":\"2.0\",\"method\":\"note\","
                + "\"params\":{\"id\":1}}") == null, "a notification got an answer");
    }

    @Test
    @DisplayName("a backend that accepts and never answers is reported, not waited on forever")
    void aStalledBackendTimesOut() throws Exception {
        java.net.ServerSocket silent = new java.net.ServerSocket(0);
        final java.util.List held = new java.util.ArrayList();
        Thread acceptor = new Thread(new Runnable() {
            public void run() {
                try {
                    held.add(silent.accept());           // accepted, never answered
                } catch (java.io.IOException closed) {
                    // the test is over
                }
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
        try {
            java.net.URL url = new java.net.URL("http://127.0.0.1:" + silent.getLocalPort()
                    + "/mcp");
            long start = System.currentTimeMillis();
            String answer = StdioBridge.post(url, null,
                    "{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":3}", 300);
            assertTrue(System.currentTimeMillis() - start < 10000, "the wait was not bounded");
            assertTrue(answer != null && answer.contains("\"id\":3")
                    && answer.contains("unreachable"), String.valueOf(answer));
        } finally {
            silent.close();
        }
        assertTrue(StdioBridge.readTimeoutMillis() > 0);
    }

    @Test
    @DisplayName("an HTTP refusal, a 401 most often, is answered under the host's own id")
    void aRefusalKeepsTheRequestId() throws Exception {
        com.sun.net.httpserver.HttpServer refusing = com.sun.net.httpserver.HttpServer.create(
                new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        refusing.createContext("/mcp", exchange -> {
            byte[] answer = ("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32001,"
                    + "\"message\":\"A bearer token is required\"}}").getBytes("UTF-8");
            java.io.InputStream in = exchange.getRequestBody();
            while(in.read() >= 0) {
                // drain the request
            }
            exchange.sendResponseHeaders(401, answer.length);
            exchange.getResponseBody().write(answer);
            exchange.close();
        });
        refusing.start();
        try {
            java.net.URL url = new java.net.URL("http://127.0.0.1:"
                    + refusing.getAddress().getPort() + "/mcp");
            String answer = StdioBridge.post(url, null,
                    "{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":9}", 5000);
            assertTrue(answer != null && answer.contains("\"id\":9")
                    && answer.contains("401") && answer.contains("bearer token"),
                    String.valueOf(answer));
            assertTrue(StdioBridge.post(url, null,
                    "{\"jsonrpc\":\"2.0\",\"method\":\"note\"}", 5000) == null,
                    "a notification got an answer");
        } finally {
            refusing.stop(0);
        }
    }

    @Test
    @DisplayName("a batch the backend cannot take gets an error for each request in it")
    void aFailedBatchAnswersEveryId() throws Exception {
        java.net.ServerSocket probe = new java.net.ServerSocket(0);
        int closed = probe.getLocalPort();
        probe.close();
        java.net.URL url = new java.net.URL("http://127.0.0.1:" + closed + "/mcp");
        String answer = StdioBridge.post(url, null, "[{\"jsonrpc\":\"2.0\",\"method\":\"ping\","
                + "\"id\":1},{\"jsonrpc\":\"2.0\",\"method\":\"note\"},"
                + "{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":\"two\"}]", 2000);
        assertTrue(answer != null && answer.startsWith("[") && answer.contains("\"id\":1")
                && answer.contains("\"id\":\"two\""), String.valueOf(answer));
    }

    @Test
    @DisplayName("credentials in exporter headers and URL queries are masked")
    void credentialsInValuesAreMasked() {
        assertTrue(DevTools.secret("cn1.otel.headers", "api-key=abc123"));
        assertTrue(DevTools.secret("cn1.datasource.url",
                "postgres://db.example/app?user=app&password=hunter2"));
        assertTrue(DevTools.secret("DATABASE_URL", "postgres://app:hunter2@db/app"));
        assertFalse(DevTools.secret("cn1.server.port", "8080"));
        assertFalse(DevTools.secret("cn1.datasource.url", "sqlite:app.db"));
    }
}
