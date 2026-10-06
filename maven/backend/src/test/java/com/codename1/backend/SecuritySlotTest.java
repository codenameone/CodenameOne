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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.impl.backend.BackendAccess;
import com.codename1.impl.backend.RequestSecurity;
import com.codename1.impl.backend.WiringEnvironment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// The slot the security layer occupies in the request path, exercised with a
/// stand-in layer: where it sits among the routes, what goes round it, and what
/// it is told when a request ends. The layer itself is tested in
/// `com.codename1.backend.security`.
class SecuritySlotTest {
    private static final HttpServer.Response SHARED = HttpServer.Response.text(200, "shared");

    /// A layer that lets a request through when it names a user, and records
    /// what the server asked of it.
    private static final class Guard implements RequestSecurity {
        final List served = new ArrayList();
        final List upgrades = new ArrayList();
        int cleared;
        int opened;
        List chains;
        boolean tls;

        @Override
        public HttpServer.Response serve(HttpServer.Request request, Next next) throws Exception {
            served.add(request.getMethod() + " " + request.getTarget());
            if (request.getHeader("X-User") == null) {
                return HttpServer.Response.text(401, "who are you");
            }
            return next.route(request);
        }

        @Override
        public void decorate(HttpServer.Request request, HttpServer.Response response) {
            response.header("X-Guard", "on");
        }

        @Override
        public int upgrade(HttpServer.Request request) {
            upgrades.add(request.getTarget());
            return request.getHeader("X-User") == null ? 401 : 0;
        }

        @Override
        public Object enter() {
            return "before";
        }

        @Override
        public void leave(Object entered) {
            if (!"before".equals(entered)) {
                throw new IllegalStateException("handed back " + entered);
            }
            cleared++;
        }
    }

    /// An application whose only bean is one chain, which is all the server needs
    /// to ask for the layer.
    private static final class Chained extends ApplicationRuntimeTest.EmptyApplication {
        private final Object[] chains;
        private final int[] orders;
        private final WebSocket socket;

        Chained(Object[] chains, int[] orders, WebSocket socket) {
            this.chains = chains;
            this.orders = orders;
            this.socket = socket;
        }

        @Override
        public HttpServer.Handler[] create(WiringEnvironment environment) {
            for (int iter = 0 ; iter < chains.length ; iter++) {
                environment.registerSecurityFilterChain(chains[iter], orders[iter]);
            }
            return new HttpServer.Handler[0];
        }

        @Override
        public void registerWebSockets(HttpServer.WebSocketRegistry registry) {
            if (socket != null) {
                registry.route("/exact", socket);
                registry.fallback(new HttpServer.WebSocketHandler() {
                    @Override
                    public WebSocket open(HttpServer.Request request) {
                        return "/routed".equals(request.pathFrom(0)) ? socket : null;
                    }
                });
            }
        }
    }

    private static Backend.SecurityRoute route(final Guard guard) {
        return new Backend.SecurityRoute() {
            @Override
            RequestSecurity open(Config config, List chains, boolean tls) {
                guard.opened++;
                guard.chains = chains;
                guard.tls = tls;
                return guard;
            }
        };
    }

    private static Properties settings() {
        Properties settings = new Properties();
        settings.setProperty(Config.SERVER_PORT, "0");
        return settings;
    }

    private static Backend start(Properties settings, Guard guard, final List reached)
            throws Exception {
        Backend.Builder builder = Backend.builder(Config.of(settings, "test")).quiet()
                .host("127.0.0.1")
                .handler(new HttpServer.Handler() {
                    @Override
                    public HttpServer.Response handle(HttpServer.Request request) {
                        String path = request.pathFrom(0);
                        reached.add(path);
                        if ("/boom".equals(path)) {
                            throw new IllegalStateException("boom");
                        }
                        return "/missing".equals(path) ? null : SHARED;
                    }
                })
                .application(new Chained(new Object[] {"chain"}, new int[] {1}, new Echo()));
        if (guard != null) {
            builder.securityRoute(route(guard));
        }
        return builder.start();
    }

    private static HttpServer.Response dispatch(Backend backend, String method, String target,
                                                String... headers) throws Exception {
        Map map = new HashMap();
        map.put("Host", "localhost");
        for (int iter = 0 ; iter + 1 < headers.length ; iter += 2) {
            map.put(headers[iter], headers[iter + 1]);
        }
        return BackendAccess.get().dispatch(backend, method, target, map, null);
    }

    private static String header(HttpServer.Response response, String name) {
        return response.headerValue(name);
    }

    @Test
    @DisplayName("the layer answers before the application's routers and decorates a copy")
    void theLayerStandsBeforeTheApplication() throws Exception {
        Guard guard = new Guard();
        List reached = new ArrayList();
        Backend backend = start(settings(), guard, reached);
        try {
            assertEquals(1, guard.opened);
            assertEquals(1, guard.chains.size());
            assertFalse(guard.tls);

            HttpServer.Response refused = dispatch(backend, "GET", "/page");
            assertEquals(401, refused.getStatus());
            assertTrue(reached.isEmpty(), "a refused request reached a handler: " + reached);
            assertEquals("on", header(refused, "X-Guard"));

            HttpServer.Response allowed = dispatch(backend, "GET", "/page", "X-User", "ann");
            assertEquals(200, allowed.getStatus());
            assertEquals("[/page]", reached.toString());
            assertEquals("on", header(allowed, "X-Guard"));
            // The handler answers every request with ONE Response; a header
            // written into it would be sent to the next client too.
            assertNull(header(SHARED, "X-Guard"));
            assertEquals(2, guard.cleared, "the thread's state is dropped after every request");
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("the thread's state is dropped when a handler throws")
    void aFailedRequestStillClears() throws Exception {
        Guard guard = new Guard();
        Backend backend = start(settings(), guard, new ArrayList());
        try {
            assertThrows(IllegalStateException.class,
                    () -> dispatch(backend, "GET", "/boom", "X-User", "ann"));
            assertEquals(1, guard.cleared);
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("the server's own routes stay outside the layer, behind their own guards")
    void ownRoutesAreNotGuarded() throws Exception {
        Guard guard = new Guard();
        Properties settings = settings();
        settings.setProperty("cn1.management.enabled", "true");
        settings.setProperty("cn1.management.token", "operator-token");
        List reached = new ArrayList();
        Backend.Builder builder = Backend.builder(Config.of(settings, "test")).quiet()
                .host("127.0.0.1")
                .management()
                .application(new Chained(new Object[] {"chain"}, new int[] {1}, null))
                .securityRoute(route(guard));
        Backend backend = builder.start();
        try {
            // Health is public; the layer, which refuses everyone, was not asked.
            assertEquals(200, dispatch(backend, "GET", "/manage/health").getStatus());
            // And the rest keeps its own token guard, not the layer's answer.
            HttpServer.Response metrics = dispatch(backend, "GET", "/manage/metrics");
            assertEquals(401, metrics.getStatus());
            assertTrue(new String(BackendAccess.get().body(metrics), "UTF-8")
                    .contains("A bearer token is required"));
            assertTrue(guard.served.isEmpty(), "the layer was asked: " + guard.served);
            assertEquals(401, dispatch(backend, "GET", "/anything-else").getStatus());
            assertEquals("[GET /anything-else]", guard.served.toString());
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("a CORS preflight is answered by the policy, round the layer and the application")
    void aPreflightIsNotAskedToAuthenticate() throws Exception {
        Guard guard = new Guard();
        Properties settings = settings();
        settings.setProperty("cn1.cors.allowedOrigins", "https://app.example");
        List reached = new ArrayList();
        Backend backend = start(settings, guard, reached);
        try {
            HttpServer.Response preflight = dispatch(backend, "OPTIONS", "/page",
                    "Origin", "https://app.example",
                    "Access-Control-Request-Method", "POST");
            assertEquals(204, preflight.getStatus());
            assertEquals("https://app.example", header(preflight, "Access-Control-Allow-Origin"));
            assertTrue(guard.served.isEmpty(), "the layer was asked: " + guard.served);
            // Nor the application: a preflight that went round the layer and on
            // to a handler would be a way past the layer.
            assertTrue(reached.isEmpty(), "a preflight reached a handler: " + reached);
            // The request the preflight announced is guarded as usual.
            assertEquals(401, dispatch(backend, "POST", "/page",
                    "Origin", "https://app.example").getStatus());
        } finally {
            backend.stop();
        }
        Guard strict = new Guard();
        Backend plain = start(settings(), strict, new ArrayList());
        try {
            assertEquals(401, dispatch(plain, "OPTIONS", "/page",
                    "Origin", "https://app.example",
                    "Access-Control-Request-Method", "POST").getStatus());
            assertEquals(1, strict.served.size());
        } finally {
            plain.stop();
        }
    }

    @Test
    @DisplayName("chains without the layer linked refuse the start; no chain opens no layer")
    void chainsNeedTheLayer() throws Exception {
        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> start(settings(), null, new ArrayList()));
        assertTrue(refused.getMessage().contains("SecurityFilterChain bean(s), and the "
                + "security layer was not linked"), refused.getMessage());

        Guard guard = new Guard();
        Backend backend = Backend.builder(Config.of(settings(), "test")).quiet()
                .host("127.0.0.1")
                .handler(new HttpServer.Handler() {
                    @Override
                    public HttpServer.Response handle(HttpServer.Request request) {
                        return SHARED;
                    }
                })
                .application(new Chained(new Object[0], new int[0], null))
                .securityRoute(route(guard))
                .start();
        try {
            assertEquals(0, guard.opened);
            assertEquals(200, dispatch(backend, "GET", "/page").getStatus());
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("chains are handed over lowest order first, equal orders as registered")
    void chainsAreOrdered() {
        WiringEnvironment environment = new WiringEnvironment(null, null, null, new ArrayList(),
                new ArrayList());
        environment.registerSecurityFilterChain("last", Integer.MAX_VALUE);
        environment.registerSecurityFilterChain("second-a", 2);
        environment.registerSecurityFilterChain("first", -5);
        environment.registerSecurityFilterChain("second-b", 2);
        environment.registerSecurityFilterChain(null, 0);
        environment.registerSecurityFilterChain("also-last", Integer.MAX_VALUE);
        assertEquals("[first, second-a, second-b, last, also-last]",
                environment.securityFilterChains().toString());
    }

    @Test
    @DisplayName("a websocket handshake is put to the layer for an exact route and a fallback one")
    void aHandshakeIsGuarded() throws Exception {
        Guard guard = new Guard();
        Backend backend = start(settings(), guard, new ArrayList());
        int port = backend.getServer().getPort();
        try {
            try (RawWebSocketClient refused = new RawWebSocketClient(port, "/exact", null)) {
                assertEquals("HTTP/1.1 401 Unauthorized", refused.getStatusLine());
            }
            try (RawWebSocketClient refused = new RawWebSocketClient(port, "/routed", null)) {
                assertEquals("HTTP/1.1 401 Unauthorized", refused.getStatusLine());
            }
            try (RawWebSocketClient open = new RawWebSocketClient(port, "/exact",
                    "X-User: ann\r\n")) {
                assertTrue(open.getStatusLine().startsWith("HTTP/1.1 101"), open.getStatusLine());
            }
            try (RawWebSocketClient open = new RawWebSocketClient(port, "/routed",
                    "X-User: ann\r\n")) {
                assertTrue(open.getStatusLine().startsWith("HTTP/1.1 101"), open.getStatusLine());
            }
            assertEquals("[/exact, /routed, /exact, /routed]", guard.upgrades.toString());
            assertEquals(4, guard.cleared);
        } finally {
            backend.stop();
        }
    }

    /// An endpoint that does nothing; the tests stop at the handshake.
    private static final class Echo implements WebSocket {
        @Override
        public void onOpen(WebSocketSession session) {
        }

        @Override
        public void onText(WebSocketSession session, String message) {
        }

        @Override
        public void onBinary(WebSocketSession session, byte[] message, int offset, int length) {
        }

        @Override
        public void onClose(WebSocketSession session, int code, String reason) {
        }

        @Override
        public void onError(WebSocketSession session, Exception error) {
        }
    }
}
