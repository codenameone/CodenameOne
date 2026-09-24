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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The findings from review, pinned.
 *
 * Kept apart from {@link WebSocketServerTest} because these are not protocol
 * cases: each one is a way the server was wrong that no frame could express, and
 * the reason for each is in the test rather than in the diff.
 */
class WebSocketReviewFixesTest {

    /** Registers one silent endpoint on `path`, the way a generated main does. */
    private static Backend.WebSocketEndpoints routes(final String path) {
        return new Backend.WebSocketEndpoints() {
            public void register(HttpServer.WebSocketRegistry registry, DataSource dataSource,
                                 com.codename1.backend.orm.EntityManager entities) {
                registry.route(path, silent());
            }
        };
    }

    /** An endpoint that does nothing, for tests about the server around it. */
    private static WebSocket silent() {
        return new WebSocket() {
            public void onOpen(WebSocketSession session) {
            }
            public void onText(WebSocketSession session, String message) {
            }
            public void onBinary(WebSocketSession session, byte[] m, int o, int l) {
            }
        };
    }

    @Test
    @DisplayName("a server with only websocket routes starts")
    void webSocketOnlyServerStarts() throws Exception {
        // This is what the build generates for a module whose only endpoints are
        // @WebSocketMapping. The builder counted HTTP routers alone, so that
        // generated application compiled, started, and threw "This server has no
        // handlers" before it ever bound a port -- and the processor test that
        // covered the generator asserted the emitted SOURCE, so it saw nothing.
        Backend backend = Backend.builder().port(0).quiet()
                .webSockets(routes("/chat"))
                .start();
        try {
            assertTrue(backend.getServer().getPort() > 0, "the listener never bound");
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a websocket router alone is also a server")
    void webSocketRouterOnlyServerStarts() throws Exception {
        Backend backend = Backend.builder().port(0).quiet()
                .webSockets(new Backend.WebSocketEndpoints() {
                    public void register(HttpServer.WebSocketRegistry registry,
                                         DataSource dataSource,
                                         com.codename1.backend.orm.EntityManager entities) {
                        registry.fallback(new HttpServer.WebSocketHandler() {
                            public WebSocket open(HttpServer.Request request) {
                                return silent();
                            }
                        });
                    }
                })
                .start();
        try {
            assertTrue(backend.getServer().getPort() > 0);
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("an ordinary request to a websocket-only server is a 404, not a crash")
    void webSocketOnlyServerAnswersHttpWithNotFound() throws Exception {
        Backend backend = Backend.builder().port(0).quiet()
                .webSockets(routes("/chat"))
                .start();
        try {
            Http.Response response = Http.get("127.0.0.1", backend.getServer().getPort(), "/x");
            assertEquals(404, response.getStatus());
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a percent-spelled path reaches the same endpoint as the plain one")
    void pathsAreRoutedCanonically() throws Exception {
        // pathIs canonicalizes percent-encoded unreserved octets for every HTTP
        // route. Routing websockets on the raw target substring instead meant
        // /ch%61t missed the /chat endpoint and fell through to a catch-all or a
        // 404 -- so which endpoint served a client, and which credentials it was
        // checked against, depended on how the client spelled the URI.
        HttpServer server = HttpServer.start("127.0.0.1", 0, 16, 4, null, null,
                new HttpServer.WebSocketRoutes() {
            public void register(HttpServer.WebSocketRegistry registry) {
                registry.route("/chat", silent());
            }
        });
        try {
            int port = server.getPort();
            RawWebSocketClient plain = new RawWebSocketClient(port, "/chat", null);
            try {
                assertTrue(plain.getStatusLine().startsWith("HTTP/1.1 101"),
                        plain.getStatusLine());
            } finally {
                plain.close();
            }
            RawWebSocketClient escaped = new RawWebSocketClient(port, "/ch%61t", null);
            try {
                assertTrue(escaped.getStatusLine().startsWith("HTTP/1.1 101"),
                        "an equivalent spelling must reach the same endpoint: "
                                + escaped.getStatusLine());
            } finally {
                escaped.close();
            }
        } finally {
            server.stop(500);
        }
    }

    @Test
    @DisplayName("a route that could never match is refused at registration")
    void unreachableRoutesAreRefused() {
        // Matched after percent-decoding and without the query, so either of these
        // would register something no request could ever select. The callback
        // throwing takes the whole start down rather than leaving a server running
        // with an endpoint nothing can reach.
        assertThrows(IOException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() throws Exception {
                HttpServer.start("127.0.0.1", 0, 8, 2, null, null,
                        new HttpServer.WebSocketRoutes() {
                    public void register(HttpServer.WebSocketRegistry registry) {
                        registry.route("/ch%61t", silent());
                    }
                });
            }
        });
        assertThrows(IOException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() throws Exception {
                HttpServer.start("127.0.0.1", 0, 8, 2, null, null,
                        new HttpServer.WebSocketRoutes() {
                    public void register(HttpServer.WebSocketRegistry registry) {
                        registry.route("/chat?room=1", silent());
                    }
                });
            }
        });
    }

    @Test
    @DisplayName("a close code that may not go on the wire is corrected, not sent")
    void outboundCloseCodesAreValidated() {
        // 1005, 1006 and 1015 are what a LOCAL implementation reports; a peer that
        // receives one reports a protocol error instead of the orderly close the
        // endpoint asked for. Same for 1004 and anything out of range.
        int[] refused = {0, 999, 1004, 1005, 1006, 1015, 5000, -1};
        for(int iter = 0 ; iter < refused.length ; iter++) {
            assertTrue(!WebSocketFrames.isValidCloseCode(refused[iter]),
                    "should not be sendable: " + refused[iter]);
        }
        // And the ones an endpoint may legitimately choose still are.
        int[] allowed = {1000, 1001, 1008, 1011, 3000, 4999};
        for(int iter = 0 ; iter < allowed.length ; iter++) {
            assertTrue(WebSocketFrames.isValidCloseCode(allowed[iter]),
                    "should be sendable: " + allowed[iter]);
        }
    }

    @Test
    @DisplayName("the reassembly budget is a process-wide ceiling, not a per-session one")
    void reassemblyBudgetIsShared() throws Exception {
        // The per-message cap bounds ONE session. A peer may open as many as
        // MAX_CONNECTIONS allows, so without a shared ceiling a hundred sessions
        // each parking a message just under the cap is hundreds of megabytes of
        // live heap, held for as long as control frames keep them alive.
        HttpServer server = HttpServer.start("127.0.0.1", 0, 8, 2, null);
        try {
            long huge = 1024L * 1024L * 1024L * 8L;
            assertTrue(!server.reserveWebSocketMemory(huge),
                    "a reservation past the ceiling must be refused");
            assertTrue(server.reserveWebSocketMemory(1024),
                    "a small reservation still fits");
            server.releaseWebSocketMemory(1024);
            // And giving it back makes it available again, or the budget shrinks
            // permanently as sessions come and go.
            assertTrue(server.reserveWebSocketMemory(1024));
            server.releaseWebSocketMemory(1024);
        } finally {
            server.stop(500);
        }
    }
}
