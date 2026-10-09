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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.HttpServer;
import com.codename1.backend.security.SecuredServer.Reply;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.rememberme.InMemoryTokenRepositoryImpl;
import com.codename1.backend.security.rememberme.PersistentTokenBasedRememberMeServices;
import com.codename1.backend.security.rememberme.PersistentTokenRepository;
import com.codename1.backend.security.rememberme.PersistentRememberMeToken;
import com.codename1.impl.backend.security.SecuritySupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// Remember-me through a real chain: the cookie issued, used, replaced, stolen
/// and withdrawn, and what a remembered user may and may not do.
class RememberMeTest {
    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            Authentication who = SecuritySupport.authentication();
            return HttpServer.Response.text(200, request.pathFrom(0) + " " + (who == null
                    ? "nobody" : who.getName() + " " + who.getAuthorities() + " "
                    + who.getClass().getSimpleName()));
        }
    };

    private static final class Ticking implements Clock {
        long now = 1700000000000L;

        @Override
        public long currentTimeMillis() {
            return now;
        }
    }

    private final InMemoryTokenRepositoryImpl tokens = new InMemoryTokenRepositoryImpl();
    private final InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
            User.withUsername("ada").password("{noop}ada-pw").roles("USER").build(),
            User.withUsername("ray").password("{noop}ray-pw").roles("USER").build());
    private final Ticking clock = new Ticking();

    private SecuredServer start() throws Exception {
        return start(tokens);
    }

    private SecuredServer start(PersistentTokenRepository repository) throws Exception {
        final PersistentTokenBasedRememberMeServices services =
                new PersistentTokenBasedRememberMeServices("key", users, repository);
        services.setClock(clock);
        return SecuredServer.start(SecuredServer.settings(), "dev", new Object[] {users}, APP,
                http -> http.authorizeHttpRequests(auth -> auth
                                .requestMatchers("/open").permitAll()
                                .requestMatchers("/full").fullyAuthenticated()
                                .requestMatchers("/remembered").rememberMe()
                                .anyRequest().authenticated())
                        .csrf(csrf -> csrf.disable())
                        .formLogin(Customizer.withDefaults())
                        .rememberMe(remember -> remember.rememberMeServices(services)).build());
    }

    private static String rememberCookie(Reply reply) {
        for (String cookie : reply.headers("Set-Cookie")) {
            if (cookie.startsWith("remember-me=")) {
                return cookie;
            }
        }
        return null;
    }

    @Test
    @DisplayName("the cookie is issued when asked for, and signs a new session in")
    void issuedAndUsed() throws Exception {
        try (SecuredServer server = start()) {
            // The generated login page offers it.
            assertTrue(server.get("/login").body.contains(
                    "<input type=\"checkbox\" id=\"remember-me\" name=\"remember-me\">"));
            // Not asked for: no cookie.
            Reply plain = server.post("/login", "username=ray&password=ray-pw");
            assertEquals("/", plain.header("Location"));
            assertNull(rememberCookie(plain));
            assertEquals(0, tokens.size());
            server.cookies.clear();

            Reply signedIn = server.post("/login", "username=ada&password=ada-pw&remember-me=on");
            assertEquals("/", signedIn.header("Location"));
            String cookie = rememberCookie(signedIn);
            assertNotNull(cookie, signedIn.toString());
            assertTrue(cookie.endsWith("; Path=/; Max-Age=1209600; HttpOnly; SameSite=Lax"), cookie);
            assertEquals(1, tokens.size());
            String first = server.cookies.get("remember-me");
            // The server keeps a hash of the token, never the token.
            String series = first.substring(0, first.indexOf(':'));
            assertEquals(PersistentTokenBasedRememberMeServices.hash(
                    first.substring(first.indexOf(':') + 1)),
                    tokens.getTokenForSeries(series).getTokenHash());
            // Signed in during this session: fully authenticated.
            assertEquals("/full ada [ROLE_USER] UsernamePasswordAuthenticationToken",
                    server.get("/full").body);
            assertEquals(403, server.get("/remembered").status);

            // The browser is closed: the session is gone, the cookie is not.
            String firstSession = server.cookies.remove("CN1SESSION");
            clock.now += 3600000;
            Reply back = server.get("/me");
            assertEquals("/me ada [ROLE_USER] RememberMeAuthenticationToken", back.body);
            // The token was replaced, under the same series.
            String second = server.cookies.get("remember-me");
            assertNotEquals(first, second);
            assertEquals(series, second.substring(0, second.indexOf(':')));
            assertEquals(1, tokens.size());
            assertEquals(clock.now, tokens.getTokenForSeries(series).getLastUsed());
            // A session started, with an id of its own.
            assertNotNull(server.cookies.get("CN1SESSION"));
            assertNotEquals(firstSession, server.cookies.get("CN1SESSION"));

            // Inside that session the cookie is not consulted again...
            assertNull(rememberCookie(server.get("/me")));
            assertEquals(second, server.cookies.get("remember-me"));
            // ...and the user stays remembered, not fully signed in, through
            // the session's own storage.
            server.cookies.remove("remember-me");
            assertEquals("/remembered ada [ROLE_USER] RememberMeAuthenticationToken",
                    server.get("/remembered").body);
            Reply refused = server.get("/full", "Accept", "text/html");
            assertEquals("302 /login", refused.status + " " + refused.header("Location"));
            // Signing in again is what gets them there, and back to where they
            // were going.
            assertEquals("/full", server.post("/login", "username=ada&password=ada-pw")
                    .header("Location"));
            assertEquals("/full ada [ROLE_USER] UsernamePasswordAuthenticationToken",
                    server.get("/full").body);
        }
    }

    @Test
    void concurrentRotationDoesNotEraseRememberedSignIns() throws Exception {
        PersistentTokenRepository racing = new PersistentTokenRepository() {
            public void createNewToken(PersistentRememberMeToken token) {
                tokens.createNewToken(token);
            }
            public PersistentRememberMeToken getTokenForSeries(String series) {
                return tokens.getTokenForSeries(series);
            }
            public boolean updateToken(String series, String expected, String next, long now) {
                // Another request rotates after this request's read, before its CAS.
                assertTrue(tokens.updateToken(series, expected,
                        PersistentTokenBasedRememberMeServices.hash("winning-token"), now));
                return tokens.updateToken(series, expected, next, now);
            }
            public void removeToken(String series) { tokens.removeToken(series); }
            public void removeUserTokens(String username) { tokens.removeUserTokens(username); }
        };
        try (SecuredServer server = start(racing)) {
            server.post("/login", "username=ada&password=ada-pw&remember-me=true");
            String original = server.cookies.get("remember-me");
            String series = original.substring(0, original.indexOf(':'));
            server.cookies.remove("CN1SESSION");
            Reply lostRace = server.get("/me");
            assertEquals("/me ada [ROLE_USER] RememberMeAuthenticationToken", lostRace.body);
            assertNull(rememberCookie(lostRace), "loser must not overwrite the winner's cookie");
            assertEquals(1, tokens.size());
            // A request arriving just after the winning write also shares the grace.
            clock.now += 9999;
            server.cookies.remove("CN1SESSION");
            assertEquals(200, server.get("/me").status);
            assertEquals(1, tokens.size());
            assertEquals(PersistentTokenBasedRememberMeServices.hash("winning-token"),
                    tokens.getTokenForSeries(series).getTokenHash());
            // Grace cannot be extended by presenting the old cookie repeatedly.
            clock.now++;
            server.cookies.remove("CN1SESSION");
            assertEquals(302, server.get("/me", "Accept", "text/html").status);
            assertEquals(0, tokens.size());
        }
    }

    @Test
    @DisplayName("a cookie replayed after the rotation grace forgets the user everywhere")
    void theft() throws Exception {
        try (SecuredServer server = start()) {
            server.post("/login", "username=ada&password=ada-pw&remember-me=true");
            String phone = server.cookies.get("remember-me");
            server.cookies.clear();
            // A second browser of the same user.
            server.post("/login", "username=ada&password=ada-pw&remember-me=1");
            String laptop = server.cookies.get("remember-me");
            server.cookies.clear();
            server.post("/login", "username=ray&password=ray-pw&remember-me=yes");
            String rays = server.cookies.get("remember-me");
            server.cookies.clear();
            assertEquals(3, tokens.size());

            // The phone's cookie is used, which replaces its token...
            server.cookies.put("remember-me", phone);
            assertEquals("/me ada [ROLE_USER] RememberMeAuthenticationToken", server.get("/me").body);
            String rotated = server.cookies.get("remember-me");
            server.cookies.clear();
            // ...and then the copy somebody took is presented after the grace.
            clock.now += 10000;
            server.cookies.put("remember-me", phone);
            Reply stolen = server.get("/me", "Accept", "text/html");
            assertEquals("302 /login", stolen.status + " " + stolen.header("Location"));
            assertTrue(rememberCookie(stolen).startsWith("remember-me=; Path=/; Max-Age=0"),
                    stolen.toString());
            // Every remembered sign-in of ada is gone: the phone's new token
            // and the laptop's too. Ray's is not.
            assertEquals(1, tokens.size());
            for (String gone : new String[] {rotated, laptop}) {
                server.cookies.clear();
                server.cookies.put("remember-me", gone);
                assertEquals(302, server.get("/me", "Accept", "text/html").status, gone);
            }
            server.cookies.clear();
            server.cookies.put("remember-me", rays);
            assertEquals("/me ray [ROLE_USER] RememberMeAuthenticationToken", server.get("/me").body);
        }
    }

    @Test
    @DisplayName("an unknown, malformed or expired cookie signs nobody in and is withdrawn")
    void refusedCookies() throws Exception {
        try (SecuredServer server = start()) {
            for (String bad : new String[] {"nonsense", "a:b", ":x", "x:", "a:b:c"}) {
                server.cookies.clear();
                server.cookies.put("remember-me", bad);
                Reply reply = server.get("/open");
                assertEquals("/open nobody", reply.body, bad);
                assertTrue(rememberCookie(reply).contains("Max-Age=0"), bad);
            }
            server.cookies.clear();
            server.post("/login", "username=ada&password=ada-pw&remember-me=on");
            String cookie = server.cookies.get("remember-me");
            server.cookies.clear();
            // Two weeks and a second later.
            clock.now += 1209600L * 1000L + 1000L;
            server.cookies.put("remember-me", cookie);
            assertEquals("/open nobody", server.get("/open").body);
            assertEquals(0, tokens.size());

            // A user who can no longer sign in is not signed in by a cookie.
            server.cookies.clear();
            server.post("/login", "username=ray&password=ray-pw&remember-me=on");
            cookie = server.cookies.get("remember-me");
            server.cookies.clear();
            users.updateUser(User.withUsername("ray").password("{noop}ray-pw").roles("USER")
                    .disabled(true).build());
            server.cookies.put("remember-me", cookie);
            assertEquals("/open nobody", server.get("/open").body);
            assertEquals(0, tokens.size());
        }
    }

    @Test
    @DisplayName("signing out withdraws the cookie and forgets the user; a refused sign-in too")
    void logoutAndFailure() throws Exception {
        try (SecuredServer server = start()) {
            server.post("/login", "username=ada&password=ada-pw&remember-me=on");
            assertEquals(1, tokens.size());
            Reply out = server.call("POST", "/logout", null, null);
            assertEquals("/login?logout", out.header("Location"));
            assertTrue(rememberCookie(out).startsWith("remember-me=; Path=/; Max-Age=0"),
                    out.toString());
            assertEquals(0, tokens.size());
            assertFalse(server.cookies.containsKey("remember-me"));
            assertEquals(302, server.get("/me", "Accept", "text/html").status);

            // A refused sign-in withdraws a cookie the browser still holds.
            server.cookies.put("remember-me", "stale:cookie");
            Reply refused = server.post("/login", "username=ada&password=wrong&remember-me=on");
            assertEquals("/login?error", refused.header("Location"));
            assertTrue(rememberCookie(refused).contains("Max-Age=0"));
            assertEquals(0, tokens.size());
        }
    }

    @Test
    @DisplayName("alwaysRemember, a cookie of another name, and a store of the configurer's own")
    void configured() throws Exception {
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev",
                new Object[] {users, tokens}, APP, http -> http.authorizeHttpRequests(auth -> auth
                                .anyRequest().authenticated())
                        .csrf(csrf -> csrf.disable())
                        .formLogin(Customizer.withDefaults())
                        .rememberMe(remember -> remember.alwaysRemember(true)
                                .rememberMeCookieName("keep").tokenValiditySeconds(60)
                                .sameSite("Strict").useSecureCookie(true)).build())) {
            Reply signedIn = server.post("/login", "username=ada&password=ada-pw");
            String cookie = null;
            for (String each : signedIn.headers("Set-Cookie")) {
                if (each.startsWith("keep=")) {
                    cookie = each;
                }
            }
            assertNotNull(cookie, signedIn.toString());
            assertTrue(cookie.endsWith("; Path=/; Max-Age=60; HttpOnly; Secure; SameSite=Strict"),
                    cookie);
            // The application's PersistentTokenRepository bean was the store.
            assertEquals(1, tokens.size());
            server.cookies.remove("CN1SESSION");
            assertEquals("/me ada [ROLE_USER] RememberMeAuthenticationToken", server.get("/me").body);
        }
    }
}
