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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Base64;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.SecuredServer.Reply;
import com.codename1.backend.security.apikey.ApiKeyGenerator;
import com.codename1.backend.security.apikey.GeneratedApiKey;
import com.codename1.backend.security.apikey.InMemoryApiKeyRepository;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.ratelimit.InMemoryRateLimiter;
import com.codename1.backend.security.ratelimit.RateLimitKeyResolver;
import com.codename1.backend.security.ratelimit.RateLimitKeys;
import com.codename1.backend.security.ratelimit.RateLimiter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// Rate limits, and the bound on password checks: what is refused, how the
/// refusal is worded on the wire, and that it lifts.
class RateLimitTest {
    /// A clock a test moves.
    static final class Moving implements Clock {
        long now = 1700000000000L;

        @Override
        public synchronized long currentTimeMillis() {
            return now;
        }

        synchronized void advance(long millis) {
            now += millis;
        }
    }

    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            return HttpServer.Response.text(200, "ok " + request.pathFrom(0));
        }
    };

    private static String basic(String user, String password) {
        return "Basic " + Base64.encode((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("A token bucket: a burst up to the limit, then the steady rate, per key")
    void bucket() {
        Moving clock = new Moving();
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(3, 60);
        limiter.setClock(clock);
        assertTrue(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("a"));
        assertFalse(limiter.tryAcquire("a"));
        assertFalse(limiter.tryAcquire("a"), "a refusal does not use anything up");
        // One token comes back every 20 seconds.
        assertEquals(20, limiter.retryAfterSeconds("a"));
        // Another key has its own.
        assertTrue(limiter.tryAcquire("b"));
        assertEquals(1, limiter.retryAfterSeconds("b"));
        assertEquals(1, limiter.retryAfterSeconds("never seen"));

        clock.advance(19000);
        assertFalse(limiter.tryAcquire("a"));
        assertEquals(1, limiter.retryAfterSeconds("a"));
        clock.advance(1000);
        assertTrue(limiter.tryAcquire("a"));
        assertFalse(limiter.tryAcquire("a"));
        // A full period later the whole burst is back, and no more than that.
        clock.advance(600000);
        assertTrue(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("a"));
        assertFalse(limiter.tryAcquire("a"));
        // A clock that goes backwards grants nothing.
        clock.advance(-600000);
        assertFalse(limiter.tryAcquire("a"));

        assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(null));
        assertThrows(IllegalArgumentException.class, () -> new InMemoryRateLimiter(0, 60));
        assertThrows(IllegalArgumentException.class, () -> new InMemoryRateLimiter(1, 0));
    }

    @Test
    @DisplayName("Memory is bounded: a flood of keys does not grow the limiter")
    void boundedMemory() {
        Moving clock = new Moving();
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(2, 60, 100);
        limiter.setClock(clock);
        // A client that is over its limit, then a flood of other keys.
        assertTrue(limiter.tryAcquire("victim"));
        assertTrue(limiter.tryAcquire("victim"));
        assertFalse(limiter.tryAcquire("victim"));
        for (int iter = 0 ; iter < 50 ; iter++) {
            assertTrue(limiter.tryAcquire("flood-" + iter));
            // Still in use, so still among the most recently used.
            assertFalse(limiter.tryAcquire("victim"), "at " + iter);
        }
        assertEquals(51, limiter.size());
        for (int iter = 0 ; iter < 100000 ; iter++) {
            limiter.tryAcquire("flood2-" + iter);
        }
        assertTrue(limiter.size() <= 100, "size " + limiter.size());

        // Keys whose buckets refilled are forgotten first: they cost nothing to forget.
        InMemoryRateLimiter small = new InMemoryRateLimiter(1, 10, 3);
        small.setClock(clock);
        assertTrue(small.tryAcquire("x"));
        assertTrue(small.tryAcquire("y"));
        clock.advance(11000);
        assertTrue(small.tryAcquire("z"));
        assertFalse(small.tryAcquire("z"));
        assertEquals(3, small.size());
        assertTrue(small.tryAcquire("w"));
        assertEquals(2, small.size(), "x and y had refilled and went; z, still short, stayed");
        assertFalse(small.tryAcquire("z"));
    }

    @Test
    @DisplayName("By client address: 429 with Retry-After, over real connections")
    void byClientAddress() throws Exception {
        final Moving clock = new Moving();
        final InMemoryRateLimiter limiter = new InMemoryRateLimiter(3, 60);
        limiter.setClock(clock);
        try (SecuredServer server = SecuredServer.start(APP, new SecuredServer.Chain() {
            @Override
            public SecurityFilterChain build(HttpSecurity http) {
                http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .rateLimit("/login", RateLimitKeys.clientAddress(), limiter);
                return http.build();
            }
        })) {
            String login = SecuredServer.request("/login");
            List<String> answers = server.onOneConnection(login, login, login, login,
                    SecuredServer.request("/other"));
            assertEquals("HTTP/1.1 200 OK / ok /login", answers.get(0));
            assertEquals("HTTP/1.1 200 OK / ok /login", answers.get(2));
            assertEquals("HTTP/1.1 429 Too Many Requests / Too Many Requests", answers.get(3));
            // The limit is on /login; the rest of the server is not touched by it.
            assertEquals("HTTP/1.1 200 OK / ok /other", answers.get(4));
            // Another connection from the same address is the same client.
            java.net.HttpURLConnection again = (java.net.HttpURLConnection) new java.net.URL(
                    "http://127.0.0.1:" + server.port() + "/login").openConnection();
            assertEquals(429, again.getResponseCode());
            assertEquals("20", again.getHeaderField("Retry-After"));
            again.disconnect();
            assertEquals(3, server.reached().size() - 1, "a refused request never reaches the app");

            // It lifts at the steady rate.
            clock.advance(20000);
            assertEquals("HTTP/1.1 200 OK / ok /login", server.onOneConnection(login).get(0));
            assertEquals("HTTP/1.1 429 Too Many Requests / Too Many Requests",
                    server.onOneConnection(login).get(0));
            // A request that arrived on no connection has no address, and so no
            // key: the limit does not apply to it.
            assertEquals(200, server.get("/login").status);
        }
    }

    @Test
    @DisplayName("By who signed in, by session and by API key: each key its own count")
    void byIdentity() throws Exception {
        final Moving clock = new Moving();
        final InMemoryRateLimiter perUser = new InMemoryRateLimiter(2, 60);
        final InMemoryRateLimiter perSession = new InMemoryRateLimiter(2, 60);
        final InMemoryRateLimiter perKey = new InMemoryRateLimiter(1, 60);
        perUser.setClock(clock);
        perSession.setClock(clock);
        perKey.setClock(clock);
        GeneratedApiKey first = new ApiKeyGenerator().generate("ci-bot", "read");
        GeneratedApiKey second = new ApiKeyGenerator().generate("ci-bot", "read");
        final InMemoryApiKeyRepository keys = new InMemoryApiKeyRepository(first.getApiKey(),
                second.getApiKey());
        Object[] beans = {new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}pw").roles("USER").build(),
                User.withUsername("ray").password("{noop}pw").roles("USER").build())};
        Properties dev = SecuredServer.settings();
        try (SecuredServer server = SecuredServer.start(dev, "dev", beans, APP,
                new SecuredServer.Chain() {
                    @Override
                    public SecurityFilterChain build(HttpSecurity http) {
                        http.authorizeHttpRequests(auth -> auth
                                .requestMatchers("/open/**").permitAll()
                                .anyRequest().authenticated())
                            .httpBasic(Customizer.<HttpBasicConfigurer>withDefaults())
                            .apiKey(a -> a.repository(keys))
                            .csrf(csrf -> csrf.disable())
                            .rateLimit("/api/**", RateLimitKeys.principal(), perUser)
                            .rateLimit("/keyed/**", RateLimitKeys.apiKeyId(), perKey)
                            .rateLimit("/open/cart", RateLimitKeys.sessionId(), perSession);
                        return http.build();
                    }
                })) {
            // Per user.
            assertEquals(200, server.get("/api/a", "Authorization", basic("ada", "pw")).status);
            assertEquals(200, server.get("/api/b", "Authorization", basic("ada", "pw")).status);
            Reply limited = server.get("/api/c", "Authorization", basic("ada", "pw"));
            assertEquals(429, limited.status, limited.toString());
            assertEquals("30", limited.header("Retry-After"));
            assertEquals("Too Many Requests", limited.body);
            // ray has his own count, and ada's other routes are untouched.
            assertEquals(200, server.get("/api/a", "Authorization", basic("ray", "pw")).status);
            assertEquals(200, server.get("/elsewhere", "Authorization", basic("ada", "pw")).status);
            // Nobody signed in: no key, so this rule says nothing -- the request
            // is asked to sign in, as it would be anyway.
            assertEquals(401, server.get("/api/a").status);
            // Wrong password: refused as that, and not counted against ada.
            assertEquals(401, server.get("/api/a", "Authorization", basic("ada", "nope")).status);
            clock.advance(30000);
            assertEquals(200, server.get("/api/c", "Authorization", basic("ada", "pw")).status);
            assertEquals(429, server.get("/api/c", "Authorization", basic("ada", "pw")).status);

            // Per API key: the id, so two keys of one owner are two counts.
            assertEquals(200, server.get("/keyed/x", "X-API-Key", first.getPlaintext()).status);
            assertEquals(429, server.get("/keyed/x", "X-API-Key", first.getPlaintext()).status);
            assertEquals(200, server.get("/keyed/x", "X-API-Key", second.getPlaintext()).status);
            // Signed in another way: no API key, no key for this rule.
            assertEquals(200, server.get("/keyed/x", "Authorization", basic("ray", "pw")).status);
            assertEquals(200, server.get("/keyed/x", "Authorization", basic("ray", "pw")).status);
        }
    }

    @Test
    @DisplayName("By session, and a key of the application's own")
    void bySessionAndCustomKey() throws Exception {
        final InMemoryRateLimiter perSession = new InMemoryRateLimiter(2, 60);
        final InMemoryRateLimiter perTenant = new InMemoryRateLimiter(1, 60);
        final RateLimitKeyResolver tenant = new RateLimitKeyResolver() {
            @Override
            public String resolve(HttpServer.Request request) {
                return request.getHeader("X-Tenant");
            }
        };
        HttpServer.Handler app = new HttpServer.Handler() {
            @Override
            public HttpServer.Response handle(HttpServer.Request request) {
                if (request.pathFrom(0).endsWith("/start")) {
                    request.getSession(true).setAttribute("cart", "1");
                }
                return HttpServer.Response.text(200, "ok");
            }
        };
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "test",
                new Object[] {perTenant}, app, new SecuredServer.Chain() {
                    @Override
                    public SecurityFilterChain build(HttpSecurity http) {
                        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                            .rateLimit("/cart/**", RateLimitKeys.sessionId(), perSession)
                            // No limiter named: the application's one RateLimiter bean.
                            .rateLimit(AntPathRequestMatcher.antMatcher("/t/**"), tenant, null)
                            .rateLimit("/both/**", RateLimitKeys.firstOf(RateLimitKeys.principal(),
                                    RateLimitKeys.sessionId()), perSession);
                        return http.build();
                    }
                })) {
            // No session yet: nothing to count under.
            for (int iter = 0 ; iter < 5 ; iter++) {
                assertEquals(200, server.get("/cart/view").status);
            }
            assertEquals(200, server.get("/start").status);
            assertFalse(server.cookies.isEmpty());
            assertEquals(200, server.get("/cart/view").status);
            assertEquals(200, server.get("/cart/view").status);
            assertEquals(429, server.get("/cart/view").status);
            // firstOf: nobody signed in, so the session is the key -- the same
            // limiter and the same key, so the same count.
            assertEquals(429, server.get("/both/x").status);
            // Another browser.
            server.cookies.clear();
            assertEquals(200, server.get("/cart/view").status);

            assertEquals(200, server.get("/t/x", "X-Tenant", "acme").status);
            assertEquals(429, server.get("/t/x", "X-Tenant", "acme").status);
            assertEquals(200, server.get("/t/x", "X-Tenant", "globex").status);
            assertEquals(200, server.get("/t/x").status);
        }
        assertEquals("rateLimit() was given no RateLimiter, and this application has no single "
                + "RateLimiter bean to use instead. Pass one: new InMemoryRateLimiter(permits, "
                + "periodSeconds).", assertThrows(IllegalStateException.class,
                        () -> SecuredServer.start(APP, new SecuredServer.Chain() {
                            @Override
                            public SecurityFilterChain build(HttpSecurity http) {
                                http.rateLimit("/x", RateLimitKeys.clientAddress(), null);
                                return http.build();
                            }
                        })).getMessage());
    }

    /// A limiter that reports how long to wait as something of its own.
    @Test
    @DisplayName("A limiter of the application's own, and what it says about waiting")
    void customLimiter() throws Exception {
        final List<String> asked = new ArrayList<String>();
        final RateLimiter shared = new RateLimiter() {
            @Override
            public boolean tryAcquire(String key) {
                synchronized (asked) {
                    asked.add(key);
                    return asked.size() < 2;
                }
            }

            @Override
            public long retryAfterSeconds(String key) {
                return 90;
            }
        };
        final RateLimiter silent = new RateLimiter() {
            @Override
            public boolean tryAcquire(String key) {
                return false;
            }
        };
        try (SecuredServer server = SecuredServer.start(APP, new SecuredServer.Chain() {
            @Override
            public SecurityFilterChain build(HttpSecurity http) {
                http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .rateLimit("/a", request -> "everyone", shared)
                    .rateLimit("/b", request -> "everyone", silent);
                return http.build();
            }
        })) {
            assertEquals(200, server.get("/a").status);
            Reply refused = server.get("/a");
            assertEquals(429, refused.status);
            assertEquals("90", refused.header("Retry-After"));
            assertEquals("1", server.get("/b").header("Retry-After"));
            synchronized (asked) {
                assertEquals(java.util.Arrays.asList("everyone", "everyone"), asked);
            }
        }
    }

    /// A password encoder whose check stands still until the test lets it go.
    static final class Held implements PasswordEncoder {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        @Override
        public String encode(CharSequence rawPassword) {
            return "held:" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            entered.countDown();
            try {
                release.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
            }
            return encodedPassword.equals("held:" + rawPassword);
        }
    }

    @Test
    @DisplayName("Password checks are bounded: one more than the bound is 503, at once")
    void passwordGate() throws Exception {
        final Held encoder = new Held();
        Object[] beans = {encoder, new InMemoryUserDetailsManager(
                User.withUsername("ada").password("held:pw").roles("USER").build())};
        Properties settings = SecuredServer.settings();
        settings.setProperty(HttpSecurity.PASSWORD_MAX_CONCURRENT, "1");
        try (final SecuredServer server = SecuredServer.start(settings, "test", beans, APP,
                new SecuredServer.Chain() {
                    @Override
                    public SecurityFilterChain build(HttpSecurity http) {
                        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                            .httpBasic(Customizer.<HttpBasicConfigurer>withDefaults());
                        return http.build();
                    }
                })) {
            final Object[] slow = new Object[1];
            // A second browser: its own cookie jar, so the two do not share one.
            Thread first = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        slow[0] = com.codename1.impl.backend.BackendAccess.get().status(
                                com.codename1.impl.backend.BackendAccess.get().dispatch(
                                        server.backend, "GET", "/page", headers("ada", "pw"), null));
                    } catch (Exception err) {
                        slow[0] = err;
                    }
                }
            });
            first.start();
            assertTrue(encoder.entered.await(30, TimeUnit.SECONDS), "the first check began");
            // While it is being checked, one more is turned away -- before the
            // user is looked up, whatever the password.
            long before = System.nanoTime();
            Reply busy = server.get("/page", "Authorization", basic("ada", "pw"));
            long tookMillis = (System.nanoTime() - before) / 1000000L;
            assertEquals(503, busy.status, busy.toString());
            assertEquals("1", busy.header("Retry-After"));
            assertEquals("Service Unavailable", busy.body);
            assertNull(busy.header("WWW-Authenticate"), "not a refusal of the credentials");
            assertTrue(tookMillis < 5000, "it did not wait for the first: " + tookMillis + "ms");
            assertEquals(503, server.get("/page", "Authorization", basic("nobody", "x")).status);
            // Requests that check no password are not held up by it.
            assertEquals(401, server.get("/page").status);

            encoder.release.countDown();
            first.join(30000);
            assertEquals(Integer.valueOf(200), slow[0]);
            // The place is free again.
            assertEquals(200, server.get("/page", "Authorization", basic("ada", "pw")).status);
            assertEquals(401, server.get("/page", "Authorization", basic("ada", "wrong")).status);
        }
    }

    private static java.util.Map<String, String> headers(String user, String password) {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<String, String>();
        map.put("Host", "localhost");
        map.put("Authorization", basic(user, password));
        return map;
    }

    @Test
    @DisplayName("The bound on a provider used directly, and no bound unless one is set")
    void passwordGateOnTheProvider() throws Exception {
        final Held encoder = new Held();
        final DaoAuthenticationProvider provider = new DaoAuthenticationProvider(
                new InMemoryUserDetailsManager(User.withUsername("ada").password("held:pw")
                        .roles("USER").build()));
        provider.setPasswordEncoder(encoder);
        provider.setMaxConcurrentPasswordChecks(1);
        final Object[] slow = new Object[1];
        Thread first = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    slow[0] = provider.authenticate(
                            UsernamePasswordAuthenticationToken.unauthenticated("ada", "pw"));
                } catch (RuntimeException err) {
                    slow[0] = err;
                }
            }
        });
        first.start();
        assertTrue(encoder.entered.await(30, TimeUnit.SECONDS));
        ServiceBusyException busy = assertThrows(ServiceBusyException.class,
                () -> provider.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated("ada", "pw")));
        assertEquals("Too many sign-ins are being checked at once", busy.getMessage());
        assertEquals(1, busy.getRetryAfterSeconds());
        // A provider with no bound of its own is not held to this one's.
        DaoAuthenticationProvider unbounded = new DaoAuthenticationProvider(
                new InMemoryUserDetailsManager(User.withUsername("ray").password("{noop}pw")
                        .roles("USER").build()));
        com.codename1.backend.security.crypto.NoOpPasswordEncoder.setDevelopmentProfile(true);
        try {
            assertEquals("ray", unbounded.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated("ray", "pw")).getName());
        } finally {
            com.codename1.backend.security.crypto.NoOpPasswordEncoder.setDevelopmentProfile(false);
        }
        encoder.release.countDown();
        first.join(30000);
        assertTrue(slow[0] instanceof Authentication, String.valueOf(slow[0]));
        assertEquals("ada", provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("ada", "pw")).getName());
    }
}
