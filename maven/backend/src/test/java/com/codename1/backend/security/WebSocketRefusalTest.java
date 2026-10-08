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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Base64;
import com.codename1.backend.HttpServer;
import com.codename1.backend.RawWebSocketClient;
import com.codename1.backend.WebSocket;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.rememberme.InMemoryTokenRepositoryImpl;
import com.codename1.backend.security.rememberme.PersistentRememberMeToken;
import com.codename1.backend.security.rememberme.PersistentTokenBasedRememberMeServices;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSet;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtDecoder;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtEncoder;
import com.codename1.backend.security.oauth2.jwt.JwtClaimsSet;
import com.codename1.backend.security.oauth2.jwt.JwtEncoderParameters;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// A WebSocket handshake a chain refuses is answered as the same request over
/// plain HTTP would be: the status, the challenge that says what to send
/// instead, the chain's security headers -- one whole response, over a real
/// socket, on a connection the server then closes.
class WebSocketRefusalTest {
    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            return HttpServer.Response.text(200, "app");
        }
    };

    private final List<String> opened = new ArrayList<String>();

    private final WebSocket endpoint = new WebSocket() {
        @Override
        public void onOpen(WebSocketSession session) {
            synchronized (opened) {
                opened.add(session.getPath());
            }
        }

        @Override
        public void onText(WebSocketSession session, String message) {
            try {
                session.sendText("echo " + message);
            } catch (java.io.IOException gone) {
                throw new IllegalStateException(gone);
            }
        }

        @Override
        public void onBinary(WebSocketSession session, byte[] message, int offset, int length) {
        }
    };

    private static Object[] users() {
        return new Object[] {new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}ada-pw").roles("USER").build(),
                User.withUsername("root").password("{noop}root-pw").roles("ADMIN").build())};
    }

    private static String basic(String user, String password) throws Exception {
        return "Authorization: Basic " + Base64.encode((user + ":" + password).getBytes("UTF-8"))
                + "\r\n";
    }

    private void assertNothingOpened() {
        synchronized (opened) {
            assertTrue(opened.isEmpty(), "a refused handshake reached its endpoint: " + opened);
        }
    }

    @Test
    void acceptedUpgradeReturnsRotatedRememberAndSessionCookies() throws Exception {
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}ada-pw").roles("USER").build());
        InMemoryTokenRepositoryImpl tokens = new InMemoryTokenRepositoryImpl();
        PersistentTokenBasedRememberMeServices remember =
                new PersistentTokenBasedRememberMeServices("key", users, tokens);
        final long[] now = {System.currentTimeMillis()};
        remember.setClock(() -> now[0]);
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev",
                new Object[] {users}, APP, endpoint, http -> http
                        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .csrf(csrf -> csrf.disable()).formLogin(Customizer.withDefaults())
                        .rememberMe(config -> config.rememberMeServices(remember)).build())) {
            for (String path : new String[] {"/ws", "/ws-routed", "/ws-nowhere"}) {
                server.cookies.clear();
                server.post("/login", "username=ada&password=ada-pw&remember-me=on");
                String original = server.cookies.get("remember-me");
                String series = original.substring(0, original.indexOf(':'));
                PersistentRememberMeToken token = tokens.getTokenForSeries(series);
                tokens.updateToken(series, token.getTokenHash(), token.getTokenHash(),
                        now[0] - 11000L);
                server.cookies.clear();
                try (RawWebSocketClient socket = new RawWebSocketClient(server.port(), path,
                        "Cookie: remember-me=" + original + "\r\n")) {
                    boolean missing = "/ws-nowhere".equals(path);
                    assertEquals(missing ? "HTTP/1.1 404 Not Found"
                            : "HTTP/1.1 101 Switching Protocols", socket.getStatusLine());
                    for (String cookie : socket.getResponseCookies()) {
                        String pair = cookie.substring(0, cookie.indexOf(';'));
                        int equals = pair.indexOf('=');
                        server.cookies.put(pair.substring(0, equals), pair.substring(equals + 1));
                    }
                    assertNotNull(server.cookies.get("CN1SESSION"));
                    assertEquals("nosniff", socket.getResponseHeader("X-Content-Type-Options"));
                    assertNotNull(server.cookies.get("remember-me"));
                    assertNotEquals(original, server.cookies.get("remember-me"));
                    if (missing) {
                        assertEquals("not found", socket.readRefusal());
                    } else {
                        socket.sendText("hi");
                        assertTrue(socket.readFrame());
                        assertEquals("echo hi", socket.getLastText());
                    }
                }
                now[0] += 11000L;
                String renewed = server.cookies.remove("remember-me");
                assertEquals(200, server.get("/me").status, "the new session must be usable");
                server.cookies.clear();
                server.cookies.put("remember-me", renewed);
                assertEquals(200, server.get("/me").status, "the rotated remember token must work");
            }
        }
    }

    @Test
    @DisplayName("HTTP Basic: 401 with its challenge, 403 without the authority, 101 with it")
    void basic() throws Exception {
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev", users(),
                APP, endpoint, http -> http.authorizeHttpRequests(auth -> auth
                                .requestMatchers("/ws-routed").hasRole("ADMIN")
                                .anyRequest().authenticated())
                        .httpBasic(basic -> basic.realmName("sockets")).build())) {
            int port = server.port();
            for (String path : new String[] {"/ws", "/ws-routed", "/ws-nowhere"}) {
                try (RawWebSocketClient refused = new RawWebSocketClient(port, path, null)) {
                    assertEquals("HTTP/1.1 401 Unauthorized", refused.getStatusLine(), path);
                    assertEquals("Basic realm=\"sockets\"",
                            refused.getResponseHeader("WWW-Authenticate"), path);
                    // What the chain writes on every answer is on this one too.
                    assertEquals("nosniff", refused.getResponseHeader("X-Content-Type-Options"));
                    assertEquals("DENY", refused.getResponseHeader("X-Frame-Options"));
                    assertEquals("close", refused.getResponseHeader("Connection"));
                    assertNull(refused.getResponseHeader("Upgrade"));
                    assertNull(refused.getResponseHeader("Sec-WebSocket-Accept"));
                    // The whole body, then the end of the stream.
                    assertEquals("Unauthorized", refused.readRefusal());
                }
            }
            try (RawWebSocketClient wrong = new RawWebSocketClient(port, "/ws",
                    basic("ada", "nope"))) {
                assertEquals("HTTP/1.1 401 Unauthorized", wrong.getStatusLine());
                assertEquals("Basic realm=\"sockets\"", wrong.getResponseHeader("WWW-Authenticate"));
                wrong.readRefusal();
            }
            try (RawWebSocketClient denied = new RawWebSocketClient(port, "/ws-routed",
                    basic("ada", "ada-pw"))) {
                assertEquals("HTTP/1.1 403 Forbidden", denied.getStatusLine());
                // Signed in: there is nothing more to send, so no challenge.
                assertNull(denied.getResponseHeader("WWW-Authenticate"));
                assertEquals("nosniff", denied.getResponseHeader("X-Content-Type-Options"));
                assertEquals("Forbidden", denied.readRefusal());
            }
            assertNothingOpened();
            try (RawWebSocketClient open = new RawWebSocketClient(port, "/ws-routed",
                    basic("root", "root-pw"))) {
                assertTrue(open.getStatusLine().startsWith("HTTP/1.1 101"), open.getStatusLine());
                // An accepted handshake is a socket: it carries frames.
                open.sendText("hi");
                assertTrue(open.readFrame());
                assertEquals("echo hi", open.getLastText());
            }
            synchronized (opened) {
                assertEquals("[/ws-routed]", opened.toString());
            }
        }
    }

    @Test
    @DisplayName("a bearer token: 401 with the Bearer challenge and its error, 403 for the scope")
    void bearer() throws Exception {
        final byte[] secret = "a-secret-of-thirty-two-bytes-ok!".getBytes("UTF-8");
        long now = System.currentTimeMillis() / 1000L;
        DefaultJwtEncoder encoder = new DefaultJwtEncoder(JwkSet.of(Jwk.ofSecret(secret)));
        String read = encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder().subject("ada")
                .issuedAt(now).expiresAt(now + 300).claim("scope", "feed:read").build()))
                .getTokenValue();
        String other = encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder().subject("ada")
                .issuedAt(now).expiresAt(now + 300).claim("scope", "other").build()))
                .getTokenValue();
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev",
                new Object[0], APP, endpoint, http -> http.authorizeHttpRequests(auth -> auth
                                .anyRequest().hasAuthority("SCOPE_feed:read"))
                        .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(
                                DefaultJwtDecoder.withSecretKey(secret).build()))).build())) {
            int port = server.port();
            try (RawWebSocketClient anonymous = new RawWebSocketClient(port, "/ws", null)) {
                assertEquals("HTTP/1.1 401 Unauthorized", anonymous.getStatusLine());
                // No token: no error, and the scope the route asks for.
                assertEquals("Bearer scope=\"feed:read\"",
                        anonymous.getResponseHeader("WWW-Authenticate"));
                anonymous.readRefusal();
            }
            try (RawWebSocketClient forged = new RawWebSocketClient(port, "/ws",
                    "Authorization: Bearer " + read.substring(0, read.length() - 2) + "xx\r\n")) {
                assertEquals("HTTP/1.1 401 Unauthorized", forged.getStatusLine());
                String challenge = forged.getResponseHeader("WWW-Authenticate");
                assertTrue(challenge != null && challenge.startsWith("Bearer error=\"invalid_token\""),
                        String.valueOf(challenge));
                forged.readRefusal();
            }
            try (RawWebSocketClient narrow = new RawWebSocketClient(port, "/ws",
                    "Authorization: Bearer " + other + "\r\n")) {
                assertEquals("HTTP/1.1 403 Forbidden", narrow.getStatusLine());
                String challenge = narrow.getResponseHeader("WWW-Authenticate");
                assertTrue(challenge != null && challenge.startsWith(
                        "Bearer error=\"insufficient_scope\""), String.valueOf(challenge));
                narrow.readRefusal();
            }
            assertNothingOpened();
            try (RawWebSocketClient open = new RawWebSocketClient(port, "/ws",
                    "Authorization: Bearer " + read + "\r\n")) {
                assertTrue(open.getStatusLine().startsWith("HTTP/1.1 101"), open.getStatusLine());
                open.sendText("hi");
                assertTrue(open.readFrame());
                assertEquals("echo hi", open.getLastText());
            }
        }
    }

    @Test
    @DisplayName("a chain that would redirect to a login page tells a socket to authenticate")
    void aLoginRedirectBecomesA401() throws Exception {
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev", users(),
                APP, endpoint, http -> http.authorizeHttpRequests(auth -> auth
                                .anyRequest().authenticated())
                        .formLogin(Customizer.withDefaults()).build())) {
            try (RawWebSocketClient refused = new RawWebSocketClient(server.port(), "/ws", null)) {
                assertEquals("HTTP/1.1 401 Unauthorized", refused.getStatusLine());
                // A socket cannot follow a redirect, so it is given none.
                assertNull(refused.getResponseHeader("Location"));
                assertEquals("nosniff", refused.getResponseHeader("X-Content-Type-Options"));
                assertEquals("Unauthorized", refused.readRefusal());
            }
            assertNothingOpened();
        }
    }
}
