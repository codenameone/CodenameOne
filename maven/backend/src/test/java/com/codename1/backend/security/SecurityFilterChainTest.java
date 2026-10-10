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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Base64;
import com.codename1.backend.ByteSink;
import com.codename1.backend.HttpServer;
import com.codename1.backend.WebSocket;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.mvc.Html;
import com.codename1.backend.mvc.Model;
import com.codename1.backend.security.SecuredServer.Reply;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.crypto.BCryptPasswordEncoder;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.impl.backend.security.SecuritySupport;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// Chains at work in a running server: which chain a request is put to, what
/// each rule lets through, how a refusal is answered, and the sign-in
/// mechanisms end to end.
class SecurityFilterChainTest {
    /// Cost 4: the tests sign in many times, and the cost is not what they test.
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);
    private static final HttpServer.Response SHARED = HttpServer.Response.text(200, "shared");

    private static Object[] beans() {
        return new Object[] {ENCODER, new InMemoryUserDetailsManager(
                User.withUsername("ada").password(ENCODER.encode("ada-pw")).roles("USER")
                        .build(),
                User.withUserDetails(User.withUsername("ray").password(ENCODER.encode("ray-pw"))
                        .authorities("ROLE_USER", "notes:read").build()).build(),
                User.withUsername("root").password(ENCODER.encode("root-pw")).roles("ADMIN")
                        .build())};
    }

    /// The application: what a request finds once a chain lets it through.
    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            String path = request.pathFrom(0);
            if (path.endsWith("/boom-denied")) {
                throw new AccessDeniedException("not yours");
            }
            if (path.endsWith("/boom-auth")) {
                throw new BadCredentialsException("the token expired");
            }
            if (path.endsWith("/boom-wrapped")) {
                throw new IllegalStateException(new AccessDeniedException("inside"));
            }
            if (path.endsWith("/boom")) {
                throw new IllegalStateException("boom");
            }
            if (path.endsWith("/missing")) {
                return null;
            }
            if (path.endsWith("/shared")) {
                return SHARED;
            }
            if (path.endsWith("/cached")) {
                return HttpServer.Response.text(200, "cached").header("cache-control", "max-age=60");
            }
            if (path.endsWith("/me")) {
                Authentication who = SecuritySupport.authentication();
                return HttpServer.Response.text(200, who == null ? "nobody"
                        : who.getName() + " " + who.getAuthorities() + " "
                        + (who.getPrincipal() instanceof UserDetails ? "UserDetails" : "other"));
            }
            if (path.endsWith("/csrf")) {
                CsrfToken token = SecuritySupport.csrfToken(request);
                return HttpServer.Response.text(200, token == null ? "none"
                        : token.getHeaderName() + "|" + token.getParameterName() + "|"
                        + token.getToken());
            }
            return HttpServer.Response.text(200, "ok " + path);
        }
    };

    private static String basic(String user, String password) throws Exception {
        return "Basic " + Base64.encode((user + ":" + password).getBytes("UTF-8"));
    }

    private static Properties dev() {
        return SecuredServer.settings();
    }

    // ------------------------------------------------------ chains and order

    @Test
    @DisplayName("a request is put to the first chain that matches it, and to no other")
    void theFirstMatchingChainGuardsTheRequest() throws Exception {
        try (SecuredServer server = SecuredServer.start(dev(), "test", beans(), APP,
                http -> http.securityMatcher("/api/**")
                        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .httpBasic(Customizer.withDefaults())
                        .csrf(csrf -> csrf.disable()).build(),
                // Overlaps the first: /api/admin belongs to the first all the same.
                http -> http.securityMatcher("/admin/**", "/api/admin/**")
                        .authorizeHttpRequests(auth -> auth.anyRequest().denyAll()).build(),
                http -> http.securityMatcher("/app/**")
                        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .formLogin(Customizer.withDefaults()).build())) {
            Reply api = server.get("/api/admin/users");
            assertEquals(401, api.status);
            assertEquals("Basic realm=\"Realm\"", api.header("WWW-Authenticate"));

            // The second chain has no way of signing in: its refusal is a 403.
            Reply admin = server.get("/admin/users");
            assertEquals(403, admin.status);
            assertEquals("Access Denied", admin.body);
            assertNull(admin.header("WWW-Authenticate"));

            Reply app = server.get("/app/home");
            assertEquals(302, app.status);
            assertEquals("/login", app.header("Location"));

            // No chain claims this one: it is not guarded, and not decorated.
            Reply open = server.get("/open");
            assertEquals(200, open.status);
            assertEquals("ok /open", open.body);
            assertNull(open.header("X-Frame-Options"));
            assertEquals("[GET /open]", server.reached().toString());
        }
    }

    @Test
    @DisplayName("a status line carries the status's own reason phrase")
    void reasonPhrases() throws Exception {
        try (SecuredServer server = SecuredServer.start(dev(), "test", beans(), APP,
                http -> http.authorizeHttpRequests(auth -> auth
                                .requestMatchers("/open/**").permitAll()
                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                .anyRequest().authenticated())
                        .formLogin(Customizer.withDefaults())
                        .httpBasic(Customizer.withDefaults()).build())) {
            List<String> lines = server.onOneConnection(
                    SecuredServer.request("/private", "Accept: text/html"),
                    SecuredServer.request("/open/x"),
                    SecuredServer.request("/private", "X-Requested-With: XMLHttpRequest"),
                    SecuredServer.request("/admin/x", "Authorization: " + basic("ada", "ada-pw")),
                    SecuredServer.request("/open/a;b"));
            assertEquals("HTTP/1.1 302 Found / ", lines.get(0));
            assertEquals("HTTP/1.1 200 OK / ok /open/x", lines.get(1));
            assertEquals("HTTP/1.1 401 Unauthorized / Unauthorized", lines.get(2));
            assertEquals("HTTP/1.1 403 Forbidden / Forbidden", lines.get(3));
            assertEquals("HTTP/1.1 400 Bad Request / Bad Request", lines.get(4));
        }
    }

    @Test
    @DisplayName("a chain for every request that is not the last is refused at start-up")
    void aCatchAllChainMustComeLast() {
        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> SecuredServer.start(dev(), "test", beans(), APP,
                        http -> http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                                .build(),
                        http -> http.securityMatcher("/api/**").build()));
        assertTrue(refused.getMessage().contains("A filter chain that matches any request has "
                + "already been configured"), refused.getMessage());
        assertTrue(refused.getMessage().contains("HttpSecurity#securityMatcher"));
    }

    // ----------------------------------------------------------------- rules

    @Test
    @DisplayName("each authorization rule lets through who it names, and the first match decides")
    void authorizationRules() throws Exception {
        try (SecuredServer server = SecuredServer.start(dev(), "test", beans(), APP,
                http -> http.authorizeHttpRequests(auth -> auth
                                .requestMatchers("/public/**", "/also-public").permitAll()
                                .requestMatchers("/none/**").denyAll()
                                .requestMatchers("/user/**").hasRole("USER")
                                .requestMatchers("/staff/**").hasAnyRole("STAFF", "ADMIN")
                                .requestMatchers("/read/**").hasAuthority("notes:read")
                                .requestMatchers("/any/**").hasAnyAuthority("x", "notes:read")
                                .requestMatchers("/guests").anonymous()
                                .requestMatchers("/owner/{name}/**").access((authentication, context) ->
                                        new AuthorizationDecision(context.getVariables().get("name")
                                                .equals(authentication.get().getName())))
                                .requestMatchers(AntPathRequestMatcher.antMatcher("DELETE", "/items/**"))
                                .hasRole("ADMIN")
                                // Shadowed by the rule above for a DELETE only.
                                .requestMatchers("/items/**").permitAll()
                                .anyRequest().authenticated())
                        .httpBasic(Customizer.withDefaults())
                        .csrf(csrf -> csrf.disable()).build())) {
            String ada = basic("ada", "ada-pw");
            String ray = basic("ray", "ray-pw");
            String root = basic("root", "root-pw");
            // {target, anonymous, ada (USER), ray (USER + notes:read), root (ADMIN)}
            Object[][] expected = {
                {"/public/x", 200, 200, 200, 200},
                {"/also-public", 200, 200, 200, 200},
                {"/none/x", 401, 403, 403, 403},
                {"/user/x", 401, 200, 200, 403},
                {"/staff/x", 401, 403, 403, 200},
                {"/read/x", 401, 403, 200, 403},
                {"/any/x", 401, 403, 200, 403},
                {"/guests", 200, 403, 403, 403},
                {"/owner/ada/notes", 401, 200, 403, 403},
                {"/items/1", 200, 200, 200, 200},
                {"/elsewhere", 401, 200, 200, 200},
            };
            String[] who = {null, ada, ray, root};
            for (Object[] row : expected) {
                for (int user = 0 ; user < who.length ; user++) {
                    Reply reply = who[user] == null ? server.get((String) row[0])
                            : server.get((String) row[0], "Authorization", who[user]);
                    assertEquals(row[user + 1], Integer.valueOf(reply.status),
                            row[0] + " as user " + user + " -> " + reply);
                }
            }
            assertEquals(401, server.call("DELETE", "/items/1", null, null).status);
            assertEquals(403, server.call("DELETE", "/items/1", null, null,
                    "Authorization", ada).status);
            assertEquals(200, server.call("DELETE", "/items/1", null, null,
                    "Authorization", root).status);

            // A signed-in caller that is denied is told so; an anonymous one is
            // asked to sign in.
            Reply denied = server.get("/staff/x", "Authorization", ada);
            assertEquals("Forbidden", denied.body);
            assertNull(denied.header("WWW-Authenticate"));
            Reply challenged = server.get("/staff/x");
            assertEquals("Basic realm=\"Realm\"", challenged.header("WWW-Authenticate"));
            // And what was refused never reached the application.
            assertFalse(server.reached().contains("GET /none/x"), server.reached().toString());
        }
    }

    @Test
    @DisplayName("rules written in an order that cannot work are refused when the chain is built")
    void badRules() {
        assertEquals("Can't configure requestMatchers after anyRequest",
                assertThrows(IllegalStateException.class, () -> SecuredServer.start(APP,
                        http -> http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll()
                                .requestMatchers("/x").denyAll()).build())).getMessage());
        assertTrue(assertThrows(IllegalStateException.class, () -> SecuredServer.start(APP,
                http -> {
                    http.authorizeHttpRequests(auth -> auth.requestMatchers("/x"));
                    return http.build();
                })).getMessage().contains("An incomplete mapping was found for "));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> SecuredServer.start(APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ROLE_X"))
                        .build())).getMessage().contains("ROLE_X should not start with ROLE_"));
        IllegalStateException noUsers = assertThrows(IllegalStateException.class,
                () -> SecuredServer.start(APP, http -> http.formLogin(Customizer.withDefaults())
                        .build()));
        assertTrue(noUsers.getMessage().startsWith("formLogin() needs something to check "
                + "credentials against"), noUsers.getMessage());
        IllegalStateException twice = assertThrows(IllegalStateException.class,
                () -> SecuredServer.start(APP, http -> {
                    http.build();
                    return http.build();
                }));
        assertTrue(twice.getMessage().startsWith("This HttpSecurity has already built its chain"),
                twice.getMessage());
    }

    // ----------------------------------------- exceptions from the application

    @Test
    @DisplayName("a security exception thrown by a controller is answered by its chain; anything else is not")
    void exceptionsThrownUnderAChain() throws Exception {
        try (SecuredServer server = SecuredServer.start(dev(), "test", beans(), APP,
                http -> http.securityMatcher("/api/**")
                        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                        .httpBasic(Customizer.withDefaults())
                        .csrf(csrf -> csrf.disable()).build(),
                http -> http.securityMatcher("/pages/**")
                        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                        .formLogin(Customizer.withDefaults()).build())) {
            String ada = basic("ada", "ada-pw");
            // Denied while signed in: 403.
            Reply denied = server.get("/api/boom-denied", "Authorization", ada);
            assertEquals(403, denied.status);
            assertEquals("Forbidden", denied.body);
            // Denied while anonymous: the remedy is to sign in.
            Reply anonymous = server.get("/api/boom-denied");
            assertEquals(401, anonymous.status);
            assertEquals("Basic realm=\"Realm\"", anonymous.header("WWW-Authenticate"));
            // An authentication failure goes to the entry point whoever is asking.
            Reply expired = server.get("/api/boom-auth", "Authorization", ada);
            assertEquals(401, expired.status);
            assertEquals("Basic realm=\"Realm\"", expired.header("WWW-Authenticate"));
            // Found through a wrapper, too.
            assertEquals(403, server.get("/api/boom-wrapped", "Authorization", ada).status);
            // Under form login the entry point is the login page.
            Reply page = server.get("/pages/boom-denied");
            assertEquals(302, page.status);
            assertEquals("/login", page.header("Location"));
            // The handler did run each time: the exception came from it.
            assertTrue(server.reached().contains("GET /api/boom-denied"));

            // What is not a security exception stays the server's 500.
            assertEquals("boom", assertThrows(IllegalStateException.class,
                    () -> server.get("/api/boom", "Authorization", ada)).getMessage());
            // And outside every chain a security exception is one more exception.
            assertEquals("not yours", assertThrows(AccessDeniedException.class,
                    () -> server.get("/elsewhere/boom-denied")).getMessage());
        }
    }

    // ------------------------------------------------------------ form login

    private static String field(String html, String name) {
        String marker = "name=\"" + name + "\" value=\"";
        int at = html.indexOf(marker);
        assertTrue(at >= 0, "no " + name + " field in " + html);
        return html.substring(at + marker.length(), html.indexOf('"', at + marker.length()));
    }

    @Test
    void aSavedRequestCannotRedirectToABackslashAuthority() throws Exception {
        for (String target : new String[] {"/\\attacker.example", "//attacker.example",
                "/private\\report"}) {
            HttpServer.Handler seed = request -> {
                request.getSession(true).setAttribute(HttpSessionRequestCache.SAVED_REQUEST, target);
                return HttpServer.Response.text(200, "seeded");
            };
            try (SecuredServer server = SecuredServer.start(dev(), "test", beans(), seed,
                    http -> http.authorizeHttpRequests(auth -> auth.requestMatchers("/seed").permitAll()
                                    .anyRequest().authenticated())
                            .formLogin(Customizer.withDefaults()).build())) {
                assertEquals(200, server.get("/seed").status);
                String csrf = field(server.get("/login").body, "_csrf");
                Reply signedIn = server.post("/login", "username=ada&password=ada-pw&_csrf=" + csrf);
                assertEquals("/", signedIn.header("Location"), target);
            }
        }
    }

    @Test
    @DisplayName("form login, end to end: saved request, CSRF, session fixation, sign-out")
    void formLoginEndToEnd() throws Exception {
        try (SecuredServer server = SecuredServer.start(dev(), "test", beans(), APP,
                http -> http.authorizeHttpRequests(auth -> auth
                                .requestMatchers("/public/**").permitAll()
                                .anyRequest().authenticated())
                        .formLogin(Customizer.withDefaults()).build())) {
            // A page nobody needs to sign in for starts no session.
            Reply open = server.get("/public/info");
            assertEquals(200, open.status);
            assertTrue(open.headers("Set-Cookie").isEmpty(), open.toString());

            // An anonymous request is sent to sign in, and its address remembered.
            Reply first = server.get("/private/report?year=2026");
            assertEquals(302, first.status);
            assertEquals("/login", first.header("Location"));
            String before = server.cookies.get("CN1SESSION");
            assertNotNull(before, "remembering the address needs a session: " + first);
            assertFalse(server.reached().contains("GET /private/report"));

            // The chain's own login page, with the token the form must send back.
            Reply page = server.get("/login");
            assertEquals(200, page.status);
            assertTrue(page.body.contains("<form method=\"post\" action=\"/login\">"), page.body);
            assertTrue(page.body.contains("name=\"username\""), page.body);
            assertTrue(page.body.contains("name=\"password\""), page.body);
            String token = field(page.body, "_csrf");

            // Without the token the form is refused before the password is looked at.
            Reply forged = server.post("/login", "username=ada&password=ada-pw");
            assertEquals(403, forged.status);
            assertEquals("Forbidden", forged.body);

            // A wrong password goes back to the form.
            Reply wrong = server.post("/login", "username=ada&password=nope&_csrf=" + token);
            assertEquals(302, wrong.status);
            assertEquals("/login?error", wrong.header("Location"));
            assertTrue(server.get("/login?error").body.contains("Bad credentials"));
            assertEquals(302, server.get("/private/report").status, "a refused sign-in signed in");

            // The right one signs in and returns to where the user was going.
            token = field(server.get("/login").body, "_csrf");
            Reply signedIn = server.post("/login", "username=ada&password=ada-pw&_csrf=" + token);
            assertEquals(302, signedIn.status, signedIn.toString());
            assertEquals("/private/report", signedIn.header("Location"));
            String after = server.cookies.get("CN1SESSION");
            assertNotEquals(before, after, "the session kept the id it had before sign-in");

            assertEquals("ok /private/report", server.get("/private/report").body);
            assertEquals("ada [ROLE_USER] UserDetails", server.get("/private/me").body);
            // The remembered address was used once.
            assertEquals(302, server.post("/login", "username=ada&password=ada-pw&_csrf="
                    + field(server.get("/login").body, "_csrf")).status);

            // The old session id no longer names the session.
            String current = server.cookies.get("CN1SESSION");
            server.cookies.put("CN1SESSION", before);
            assertEquals(302, server.get("/private/report").status,
                    "the id from before sign-in still opened the session");
            server.cookies.put("CN1SESSION", current);

            // The token from before sign-in died with it; a new one works, masked
            // differently each time it is handed out.
            assertEquals(403, server.post("/private/save", "_csrf=" + token).status);
            String[] one = server.get("/private/csrf").body.split("\\|");
            String[] two = server.get("/private/csrf").body.split("\\|");
            assertEquals("X-CSRF-TOKEN", one[0]);
            assertEquals("_csrf", one[1]);
            assertNotEquals(one[2], two[2], "the token was sent unmasked, or masked the same twice");
            assertEquals("ok /private/save", server.post("/private/save", "_csrf=" + one[2]).body);
            assertEquals("ok /private/save", server.call("POST", "/private/save", null, null,
                    "X-CSRF-TOKEN", two[2]).body);
            assertEquals(403, server.call("POST", "/private/save", null, null,
                    "X-CSRF-TOKEN", two[2].substring(1) + "A").status);
            assertEquals(403, server.call("PUT", "/private/save", null, null).status);
            assertEquals(403, server.call("DELETE", "/private/save", null, null).status);

            // Signing out is a POST with the token; a GET is an ordinary request.
            assertEquals("ok /logout", server.get("/logout").body);
            assertEquals(403, server.post("/logout", "").status);
            Reply out = server.post("/logout", "_csrf=" + one[2]);
            assertEquals(302, out.status);
            assertEquals("/login?logout", out.header("Location"));
            assertNull(server.cookies.get("CN1SESSION"), "the session cookie was not cleared: " + out);
            assertTrue(server.get("/login?logout").body.contains("You have been signed out"));
            assertEquals(302, server.get("/private/report").status);
            // The ended session is gone on the server, not only forgotten by the client.
            server.cookies.put("CN1SESSION", current);
            assertEquals(302, server.get("/private/report").status);
        }
    }

    @Test
    @DisplayName("an application's own login page, success and failure addresses, and handlers")
    void aCustomLoginPage() throws Exception {
        try (SecuredServer server = SecuredServer.start(dev(), "test", beans(), APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .formLogin(form -> form.loginPage("/signin")
                                .usernameParameter("email").passwordParameter("secret")
                                .defaultSuccessUrl("/home", true)
                                .failureUrl("/signin?bad")
                                .permitAll())
                        .logout(logout -> logout.logoutUrl("/signout").logoutSuccessUrl("/bye")
                                .permitAll())
                        .csrf(csrf -> csrf.disable()).build())) {
            Reply start = server.get("/reports");
            assertEquals("/signin", start.header("Location"));
            // The application serves the page; permitAll() is what lets it be reached.
            assertEquals("ok /signin", server.get("/signin").body);
            assertEquals("ok /signin", server.get("/signin?bad").body);
            assertEquals("/signin?bad",
                    server.post("/signin", "email=ada&secret=nope").header("Location"));
            // The default field names are not this form's.
            assertEquals("/signin?bad",
                    server.post("/signin", "username=ada&password=ada-pw").header("Location"));
            Reply in = server.post("/signin", "email=ada&secret=ada-pw");
            // Always the success address, not the remembered /reports.
            assertEquals("/home", in.header("Location"));
            assertEquals("ok /reports", server.get("/reports").body);
            // With CSRF protection off, sign-out takes any method.
            Reply out = server.get("/signout");
            assertEquals(302, out.status);
            assertEquals("/bye", out.header("Location"));
            assertEquals("ok /bye", server.get("/bye").body);
            assertEquals("/signin", server.get("/reports").header("Location"));
        }
        // The same without permitAll(): the login page redirects to itself.
        try (SecuredServer closed = SecuredServer.start(dev(), "test", beans(), APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .formLogin(form -> form.loginPage("/signin")
                                .successHandler((request, authentication) ->
                                        HttpServer.Response.text(200, "hello " + authentication.getName()))
                                .failureHandler((request, exception) ->
                                        HttpServer.Response.text(401, exception.getMessage())))
                        .csrf(csrf -> csrf.disable()).build())) {
            assertEquals("/signin", closed.get("/signin").header("Location"));
            Reply bad = closed.post("/signin", "username=ada&password=x");
            assertEquals(401, bad.status);
            assertEquals("Bad credentials", bad.body);
            assertEquals("hello ada", closed.post("/signin", "username=ada&password=ada-pw").body);
        }
    }

    // ------------------------------------------------------------- HTTP Basic

    @Test
    @DisplayName("HTTP Basic authenticates each request and keeps nothing")
    void httpBasic() throws Exception {
        try (SecuredServer server = SecuredServer.start(dev(), "test", beans(), APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .httpBasic(basic -> basic.realmName("Orders API"))
                        .csrf(csrf -> csrf.disable()).build())) {
            Reply none = server.get("/orders/me");
            assertEquals(401, none.status);
            assertEquals("Basic realm=\"Orders API\"", none.header("WWW-Authenticate"));
            assertEquals("Unauthorized", none.body);

            assertEquals(401, server.get("/orders/me", "Authorization", basic("ada", "wrong")).status);
            assertEquals(401, server.get("/orders/me", "Authorization", basic("eve", "x")).status);
            assertEquals(401, server.get("/orders/me", "Authorization", "Basic !!!not-base64").status);
            assertEquals(401, server.get("/orders/me", "Authorization",
                    "Basic " + Base64.encode("no-colon".getBytes("UTF-8"))).status);
            // Another scheme is not this filter's to judge; the rules still refuse.
            assertEquals(401, server.get("/orders/me", "Authorization", "Bearer abc").status);
            assertEquals("[]", server.reached().toString());

            Reply ok = server.get("/orders/me", "Authorization", basic("ada", "ada-pw"));
            assertEquals("ada [ROLE_USER] UserDetails", ok.body);
            // The scheme is matched without regard to case.
            assertEquals(200, server.get("/orders/me", "Authorization",
                    "bAsIc " + basic("ada", "ada-pw").substring(6)).status);
            // A password with a colon in it is everything after the first one.
            assertEquals(401, server.get("/orders/me", "Authorization", basic("ada", "ada:pw")).status);

            // Nothing was kept: no cookie, and the next request is anonymous.
            assertTrue(server.cookies.isEmpty(), server.cookies.toString());
            assertEquals(401, server.get("/orders/me").status);
        }
    }

    @Test
    @DisplayName("with both sign-ins, a browser is sent to the form and a script is challenged")
    void formAndBasicTogether() throws Exception {
        try (SecuredServer server = SecuredServer.start(dev(), "test", beans(), APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .httpBasic(Customizer.withDefaults())
                        .formLogin(Customizer.withDefaults()).build())) {
            assertEquals("/login", server.get("/x").header("Location"));
            Reply script = server.get("/x", "X-Requested-With", "XMLHttpRequest");
            assertEquals(401, script.status);
            assertEquals("Basic realm=\"Realm\"", script.header("WWW-Authenticate"));
        }
    }

    // --------------------------------------------------------------- sessions

    @Test
    @DisplayName("a STATELESS chain neither starts nor reads a session")
    void statelessLeavesNoSession() throws Exception {
        try (SecuredServer server = SecuredServer.start(dev(), "test", beans(), APP,
                http -> http.securityMatcher("/api/**")
                        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .httpBasic(Customizer.withDefaults())
                        .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .csrf(csrf -> csrf.disable()).build(),
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .formLogin(Customizer.withDefaults())
                        .csrf(csrf -> csrf.disable()).build())) {
            String ada = basic("ada", "ada-pw");
            for (Reply reply : new Reply[] {server.get("/api/me"),
                server.get("/api/me", "Authorization", ada),
                server.get("/api/me", "Authorization", basic("ada", "no")),
                server.get("/api/boom-denied", "Authorization", ada),
                server.call("POST", "/api/logout", null, null)}) {
                assertTrue(reply.headers("Set-Cookie").isEmpty(), "a stateless chain set a "
                        + "cookie: " + reply);
            }
            assertTrue(server.cookies.isEmpty());

            // Signed in through the other chain's form, the client holds a session.
            assertEquals("/", server.post("/login", "username=root&password=root-pw")
                    .header("Location"));
            assertEquals("root [ROLE_ADMIN] UserDetails", server.get("/me").body);
            // The stateless chain does not look at it.
            assertEquals(401, server.get("/api/me").status);
        }
    }

    @Test
    @DisplayName("ALWAYS starts a session at once; without sign-in or CSRF the default starts none")
    void sessionCreationPolicies() throws Exception {
        try (SecuredServer server = SecuredServer.start(dev(), "test", beans(), APP,
                http -> http.securityMatcher("/always/**")
                        .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)).build(),
                http -> http.securityMatcher("/never/**")
                        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .formLogin(form -> form.loginProcessingUrl("/never/login"))
                        .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.NEVER))
                        .csrf(csrf -> csrf.disable()).build(),
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                        .build())) {
            assertTrue(server.get("/default/x").headers("Set-Cookie").isEmpty());
            // NEVER: an anonymous request's address is not worth a session, and
            // neither is the sign-in itself.
            assertTrue(server.get("/never/x").headers("Set-Cookie").isEmpty());
            Reply in = server.post("/never/login", "username=ada&password=ada-pw");
            assertEquals(302, in.status);
            assertTrue(in.headers("Set-Cookie").isEmpty(), in.toString());
            assertEquals(302, server.get("/never/x").status, "signed in with nowhere to keep it");

            Reply always = server.get("/always/x");
            assertEquals(1, always.headers("Set-Cookie").size(), always.toString());
            assertTrue(always.header("Set-Cookie").startsWith("CN1SESSION="));
            // With a session there already, NEVER uses it.
            assertEquals(302, server.post("/never/login", "username=ada&password=ada-pw").status);
            assertEquals("ok /never/x", server.get("/never/x").body);
        }
    }

    @Test
    @DisplayName("who signed in on one request is not who the next request on the connection is")
    void theContextDoesNotLeakAcrossAKeepAliveConnection() throws Exception {
        try (SecuredServer server = SecuredServer.start(dev(), "test", beans(), APP,
                http -> http.securityMatcher("/api/**")
                        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .httpBasic(Customizer.withDefaults())
                        .csrf(csrf -> csrf.disable()).build())) {
            // In process first, where every request is served by this one thread:
            // the request after a signed-in one, outside every chain, is nobody's.
            assertEquals("ada [ROLE_USER] UserDetails",
                    server.get("/api/me", "Authorization", basic("ada", "ada-pw")).body);
            assertEquals("nobody", server.get("/open/me").body);
            assertNull(SecurityContextHolder.peek(), "the thread kept a context after its request");
            assertNull(SecurityExchange.current(), "the thread kept an exchange after its request");
            // A request that fails leaves nothing behind either.
            assertThrows(IllegalStateException.class,
                    () -> server.get("/api/boom", "Authorization", basic("ada", "ada-pw")));
            assertEquals("nobody", server.get("/open/me").body);

            String ada = "Authorization: " + basic("ada", "ada-pw");
            // And on one connection, which a packaged server gives one thread.
            List<String> statuses = server.onOneConnection(
                    SecuredServer.request("/api/me", ada),
                    SecuredServer.request("/api/me"),
                    // Outside every chain: the holder must be empty here too.
                    SecuredServer.request("/open/me"),
                    SecuredServer.request("/api/me", ada));
            assertEquals("[HTTP/1.1 200 OK / ada [ROLE_USER] UserDetails, "
                    + "HTTP/1.1 401 Unauthorized / Unauthorized, "
                    + "HTTP/1.1 200 OK / nobody, "
                    + "HTTP/1.1 200 OK / ada [ROLE_USER] UserDetails]", statuses.toString());
        }
    }

    // -------------------------------------------------------------------- CORS

    @Test
    @DisplayName("a CORS preflight is answered without credentials; the request it announces is not")
    void aPreflightPasses() throws Exception {
        Properties settings = dev();
        settings.setProperty("cn1.cors.allowedOrigins", "https://app.example");
        settings.setProperty("cn1.cors.allowedHeaders", "Authorization");
        try (SecuredServer server = SecuredServer.start(settings, "test", beans(), APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .httpBasic(Customizer.withDefaults())
                        .csrf(csrf -> csrf.disable()).build())) {
            Reply preflight = server.call("OPTIONS", "/orders", null, null,
                    "Origin", "https://app.example",
                    "Access-Control-Request-Method", "POST",
                    "Access-Control-Request-Headers", "Authorization");
            assertEquals(204, preflight.status, preflight.toString());
            assertEquals("https://app.example", preflight.header("Access-Control-Allow-Origin"));
            assertNull(preflight.header("WWW-Authenticate"));
            // Answered by the policy itself: the application, which would take
            // any request, was not asked.
            assertEquals("[]", server.reached().toString());

            Reply refused = server.call("POST", "/orders", null, null,
                    "Origin", "https://app.example");
            assertEquals(401, refused.status);
            // Readable by the page that made it: the CORS headers are on the 401.
            assertEquals("https://app.example", refused.header("Access-Control-Allow-Origin"));
            assertEquals(200, server.call("POST", "/orders", null, null,
                    "Origin", "https://app.example", "Authorization", basic("ada", "ada-pw")).status);
            // An OPTIONS that is not a preflight is an ordinary request.
            assertEquals(401, server.call("OPTIONS", "/orders", null, null).status);
        }
    }

    // ----------------------------------------------------------------- headers

    private static HttpURLConnection http(SecuredServer server, String path) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + server.port()
                + path).openConnection();
        c.setInstanceFollowRedirects(false);
        return c;
    }

    @Test
    @DisplayName("the default security headers go on every answer under a chain, and into a copy")
    void defaultSecurityHeaders() throws Exception {
        try (SecuredServer server = SecuredServer.start(dev(), "test", beans(), APP,
                http -> http.securityMatcher("/app/**")
                        .authorizeHttpRequests(auth -> auth
                                .requestMatchers("/app/private/**").authenticated()
                                .anyRequest().permitAll())
                        .httpBasic(Customizer.withDefaults()).build())) {
            for (Reply reply : new Reply[] {server.get("/app/x"), server.get("/app/private/x"),
                server.get("/app/shared")}) {
                assertEquals("nosniff", reply.header("X-Content-Type-Options"), reply.toString());
                assertEquals("0", reply.header("X-XSS-Protection"));
                assertEquals("DENY", reply.header("X-Frame-Options"));
                assertEquals("no-cache, no-store, max-age=0, must-revalidate",
                        reply.header("Cache-Control"));
                assertEquals("no-cache", reply.header("Pragma"));
                assertEquals("0", reply.header("Expires"));
                // Plain HTTP: a browser would ignore the header, and it is not sent.
                assertNull(reply.header("Strict-Transport-Security"));
                assertNull(reply.header("Content-Security-Policy"));
            }
            // The handler's one shared Response was not written into.
            assertEquals("shared", server.get("/app/shared").body);
            assertTrue(com.codename1.impl.backend.BackendAccess.get().headers(SHARED).isEmpty(),
                    "the shared response now carries headers");
            // A cache policy the handler chose stands, under the name it used.
            Reply cached = server.get("/app/cached");
            assertEquals("max-age=60", cached.header("Cache-Control"));
            assertEquals(1, cached.headers("Cache-Control").size(), cached.toString());
            assertNull(cached.header("Pragma"));
            assertEquals("DENY", cached.header("X-Frame-Options"));

            // The server's own 404 and 500, which only a real connection shows.
            HttpURLConnection missing = http(server, "/app/missing");
            assertEquals(404, missing.getResponseCode());
            assertEquals("DENY", missing.getHeaderField("X-Frame-Options"));
            assertEquals("nosniff", missing.getHeaderField("X-Content-Type-Options"));
            HttpURLConnection failed = http(server, "/app/boom");
            assertEquals(500, failed.getResponseCode());
            assertEquals("DENY", failed.getHeaderField("X-Frame-Options"));
            // And not on what no chain guards.
            HttpURLConnection outside = http(server, "/other/missing");
            assertEquals(404, outside.getResponseCode());
            assertNull(outside.getHeaderField("X-Frame-Options"));
        }
    }

    @Test
    @DisplayName("each header can be changed or left out, and a writer of the application's added")
    void configuredSecurityHeaders() throws Exception {
        try (SecuredServer server = SecuredServer.start(dev(), "test", beans(), APP,
                http -> http.securityMatcher("/framed/**")
                        .headers(headers -> headers
                                .frameOptions(frame -> frame.sameOrigin())
                                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                                .cacheControl(cache -> cache.disable())
                                .xssProtection(xss -> xss.disable())
                                .addHeaderWriter((request, response) ->
                                        response.setIfAbsent("Referrer-Policy", "no-referrer")))
                        .build(),
                http -> http.securityMatcher("/bare/**")
                        .headers(headers -> headers.defaultsDisabled()
                                .contentTypeOptions(Customizer.withDefaults())).build(),
                http -> http.securityMatcher("/none/**").headers(headers -> headers.disable())
                        .addFilterAfter((request, chain) -> {
                            SecurityExchange.current().setResponseHeader("X-Trace", "t-1");
                            SecurityExchange.current().addResponseHeader("Set-Cookie", "a=1; Path=/");
                            SecurityExchange.current().addResponseHeader("Set-Cookie", "b=2; Path=/");
                            return chain.doFilter(request);
                        }, SecurityContextHolderFilter.class).build())) {
            Reply framed = server.get("/framed/x");
            assertEquals("SAMEORIGIN", framed.header("X-Frame-Options"));
            assertEquals("default-src 'self'", framed.header("Content-Security-Policy"));
            assertEquals("no-referrer", framed.header("Referrer-Policy"));
            assertEquals("nosniff", framed.header("X-Content-Type-Options"));
            assertNull(framed.header("Cache-Control"));
            assertNull(framed.header("X-XSS-Protection"));

            Reply bare = server.get("/bare/x");
            assertEquals("nosniff", bare.header("X-Content-Type-Options"));
            assertNull(bare.header("X-Frame-Options"));
            assertNull(bare.header("Cache-Control"));

            Reply none = server.get("/none/x");
            assertNull(none.header("X-Content-Type-Options"));
            // What a filter recorded for the response is written all the same.
            assertEquals("t-1", none.header("X-Trace"));
            assertEquals("[a=1; Path=/, b=2; Path=/]", none.headers("Set-Cookie").toString());
        }
    }

    // -------------------------------------------------------------------- CSRF

    @Test
    void mvcModelsDeferCsrfUntilTheTokenIsUsed() throws Exception {
        HttpServer.Handler pages = request -> {
            Model source = Html.model(request);
            Model model = new Model().addAllAttributes(source);
            String path = request.pathFrom(0);
            assertTrue(model.containsAttribute("_csrf"));
            if (path.endsWith("/read")) {
                return HttpServer.Response.text(200, "read-only");
            }
            if (path.endsWith("/override")) {
                model.addAttribute("_csrf", null);
            }
            ByteSink out = new ByteSink(128);
            Html.csrf(out, model);
            CsrfToken token = (CsrfToken) model.getAttribute("_csrf");
            assertSame(token, model.getAttribute("_csrf"));
            String text = new String(Html.bytes(out), "UTF-8");
            if (token != null) {
                assertSame(token, source.getAttribute("_csrf"));
                assertTrue(text.contains("name=\"" + token.getParameterName() + "\""));
                assertTrue(text.contains("value=\"" + token.getToken() + "\""));
            }
            return HttpServer.Response.text(200, token == null ? "none" : token.getToken());
        };
        try (SecuredServer server = SecuredServer.start(dev(), "test", beans(), pages,
                http -> http.securityMatcher("/open/**").csrf(csrf -> csrf.disable()).build(),
                http -> http.build())) {
            for (String path : new String[] {"/read", "/open/form", "/override"}) {
                Reply reply = server.get(path);
                assertEquals(200, reply.status);
                assertTrue(reply.headers("Set-Cookie").isEmpty(), reply.toString());
                assertTrue(server.cookies.isEmpty());
            }
            Reply form = server.get("/form");
            assertEquals(200, form.status);
            assertNotNull(server.cookies.get("CN1SESSION"));
            assertNotEquals("none", form.body);
            assertEquals(403, server.post("/save", "").status);
            assertEquals(200, server.post("/save", "_csrf=" + form.body).status);
        }
    }

    @Test
    void csrfCookieCannotBeInjectedOrCopiedFromAnotherSession() throws Exception {
        try (SecuredServer server = SecuredServer.start(dev(), "test", beans(), APP,
                http -> http.csrf(csrf -> csrf.csrfTokenRepository(
                        CookieCsrfTokenRepository.withHttpOnlyFalse())).build())) {
            server.get("/csrf");
            String otherToken = server.cookies.get("XSRF-TOKEN");
            server.cookies.clear();
            server.get("/csrf");
            String ownToken = server.cookies.get("XSRF-TOKEN");
            assertEquals(200, server.call("POST", "/save", null, null,
                    "X-XSRF-TOKEN", ownToken).status);
            for (String injected : new String[] {CsrfFilter.newTokenValue(), otherToken}) {
                server.cookies.put("XSRF-TOKEN", injected);
                assertEquals(403, server.post("/save", "_csrf=" + CsrfFilter.mask(injected)).status);
                server.cookies.put("XSRF-TOKEN", injected);
                assertEquals(403, server.call("POST", "/save", null, null,
                        "X-XSRF-TOKEN", injected).status);
            }
            // A rejected injected cookie is replaced with a fresh session-bound token.
            assertEquals(200, server.call("POST", "/save", null, null,
                    "X-XSRF-TOKEN", server.cookies.get("XSRF-TOKEN")).status);
        }
    }

    @Test
    @DisplayName("the cookie repository gives a script its token; ignored requests need none")
    void csrfVariants() throws Exception {
        try (SecuredServer server = SecuredServer.start(dev(), "test", beans(), APP,
                http -> http.securityMatcher("/spa/**")
                        .csrf(csrf -> csrf
                                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                .ignoringRequestMatchers("/spa/webhooks/**"))
                        .build(),
                http -> http.securityMatcher("/open/**").csrf(csrf -> csrf.disable()).build(),
                http -> http.build())) {
            // The first answer hands the script its cookie, readable by it.
            Reply first = server.get("/spa/index");
            String cookie = null;
            for (String candidate : first.headers("Set-Cookie")) {
                if (candidate.startsWith("XSRF-TOKEN=")) { cookie = candidate; }
            }
            assertNotNull(cookie, first.toString());
            assertFalse(cookie.contains("HttpOnly"), cookie);
            assertTrue(cookie.contains("Path=/"), cookie);
            String token = server.cookies.get("XSRF-TOKEN");
            // Once the client has it, it is not sent again.
            assertTrue(server.get("/spa/index").headers("Set-Cookie").isEmpty());

            assertEquals(403, server.post("/spa/save", "").status);
            assertEquals("ok /spa/save", server.call("POST", "/spa/save", null, null,
                    "X-XSRF-TOKEN", token).body);
            assertEquals(403, server.call("POST", "/spa/save", null, null,
                    "X-XSRF-TOKEN", token + "x").status);
            // The session repository's header name is not this repository's.
            assertEquals(403, server.call("POST", "/spa/save", null, null,
                    "X-CSRF-TOKEN", token).status);
            // The masked form a server-rendered page would carry works too.
            String masked = server.get("/spa/csrf").body.split("\\|")[2];
            assertNotEquals(token, masked);
            assertEquals("ok /spa/save", server.post("/spa/save", "_csrf=" + masked).body);
            // The readable cookie is bound to this session against cookie injection.
            assertNotNull(server.cookies.get("CN1SESSION"));

            assertEquals("ok /spa/webhooks/stripe", server.post("/spa/webhooks/stripe", "").body);
            assertEquals("ok /open/save", server.post("/open/save", "").body);
            assertEquals("none", server.get("/open/csrf").body);

            // The session repository takes the masked token only.
            String[] sessionToken = server.get("/csrf").body.split("\\|");
            assertEquals("X-CSRF-TOKEN", sessionToken[0]);
            assertNotNull(server.cookies.get("CN1SESSION"));
            assertEquals("ok /save", server.post("/save", "_csrf=" + sessionToken[2]).body);
            // A client with no session has no token to match: refused, and no
            // session is started to hold one.
            server.cookies.clear();
            Reply stranger = server.post("/save", "_csrf=" + sessionToken[2]);
            assertEquals(403, stranger.status);
            assertTrue(stranger.headers("Set-Cookie").isEmpty(), stranger.toString());
        }
    }

    // --------------------------------------------------------------- WebSocket

    private static String upgrade(String path, String... headerLines) {
        List<String> lines = new ArrayList<String>();
        lines.add("Upgrade: websocket");
        lines.add("Connection: Upgrade");
        lines.add("Sec-WebSocket-Version: 13");
        lines.add("Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==");
        for (String line : headerLines) {
            lines.add(line);
        }
        return SecuredServer.request(path, lines.toArray(new String[lines.size()]));
    }

    @Test
    @DisplayName("a WebSocket handshake is authenticated and authorized by the chain of its path")
    void webSocketHandshakes() throws Exception {
        final List<String> opened = new ArrayList<String>();
        WebSocket endpoint = new WebSocket() {
            @Override
            public void onOpen(WebSocketSession session) {
                synchronized (opened) {
                    opened.add(session.getPath());
                }
            }

            @Override
            public void onText(WebSocketSession session, String message) {
            }

            @Override
            public void onBinary(WebSocketSession session, byte[] message, int offset, int length) {
            }
        };
        try (SecuredServer server = SecuredServer.start(dev(), "test", beans(), APP, endpoint,
                http -> http.authorizeHttpRequests(auth -> auth
                                .requestMatchers("/ws-routed").hasRole("ADMIN")
                                .anyRequest().authenticated())
                        .httpBasic(Customizer.withDefaults())
                        .formLogin(Customizer.withDefaults()).build())) {
            String ada = "Authorization: " + basic("ada", "ada-pw");
            String root = "Authorization: " + basic("root", "root-pw");
            // Anonymous: the chain would redirect a browser to the form, which a
            // socket cannot follow -- it is told to authenticate.
            assertEquals("[HTTP/1.1 401 Unauthorized / Unauthorized]",
                    server.onOneConnection(upgrade("/ws")).toString());
            assertEquals("[HTTP/1.1 401 Unauthorized / Unauthorized]",
                    server.onOneConnection(upgrade("/ws-routed")).toString());
            assertEquals("[HTTP/1.1 401 Unauthorized / Unauthorized]", server.onOneConnection(
                    upgrade("/ws", "Authorization: " + basic("ada", "wrong"))).toString());
            // Signed in but not allowed this endpoint.
            assertEquals("[HTTP/1.1 403 Forbidden / Forbidden]",
                    server.onOneConnection(upgrade("/ws-routed", ada)).toString());
            // A path with no endpoint is refused like the rest, not revealed.
            assertEquals("[HTTP/1.1 401 Unauthorized / Unauthorized]",
                    server.onOneConnection(upgrade("/ws-nowhere")).toString());
            synchronized (opened) {
                assertTrue(opened.isEmpty(), "a refused handshake opened " + opened);
            }

            assertEquals("[HTTP/1.1 101 Switching Protocols / ]",
                    server.onOneConnection(upgrade("/ws", ada)).toString());
            assertEquals("[HTTP/1.1 101 Switching Protocols / ]",
                    server.onOneConnection(upgrade("/ws-routed", root)).toString());
            assertEquals("[HTTP/1.1 404 Not Found / not found]",
                    server.onOneConnection(upgrade("/ws-nowhere", ada)).toString());

            // And through the session a browser signed in with.
            String token = field(server.get("/login").body, "_csrf");
            assertEquals(302, server.post("/login", "username=ada&password=ada-pw&_csrf="
                    + token).status);
            assertEquals("[HTTP/1.1 101 Switching Protocols / ]", server.onOneConnection(
                    upgrade("/ws", "Cookie: CN1SESSION=" + server.cookies.get("CN1SESSION")))
                    .toString());
        }
    }

    // ------------------------------------------------------- filters and users

    @Test
    @DisplayName("an application's filters run where they were put, and its configurer takes part")
    void customFiltersAndConfigurers() throws Exception {
        final List<String> order = new ArrayList<String>();
        final class Mark implements SecurityFilter {
            private final String name;

            Mark(String name) {
                this.name = name;
            }

            @Override
            public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
                    throws Exception {
                Authentication who = SecurityContextHolder.getContext().getAuthentication();
                order.add(name + ":" + (who == null ? "none" : who.getName()));
                return chain.doFilter(request);
            }
        }
        final class Extra extends SecurityConfigurer {
            String greeting = "hello";

            @Override
            public void configure(HttpSecurity http) {
                final String text = greeting;
                http.addFilterBefore((request, chain) -> "/hi".equals(request.pathFrom(0))
                        ? HttpServer.Response.text(200, text) : chain.doFilter(request),
                        AuthorizationFilter.class);
            }
        }
        try (SecuredServer server = SecuredServer.start(dev(), "test", beans(), APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .httpBasic(Customizer.withDefaults())
                        .csrf(csrf -> csrf.disable())
                        .addFilterAfter(new Mark("afterBasic"), BasicAuthenticationFilter.class)
                        .addFilterBefore(new Mark("first"), SecurityContextHolderFilter.class)
                        .addFilterBefore(new Mark("beforeBasic"), BasicAuthenticationFilter.class)
                        .addFilterAfter(new Mark("afterAnonymous"), AnonymousAuthenticationFilter.class)
                        .with(new Extra(), extra -> extra.greeting = "good day")
                        .build())) {
            assertEquals(200, server.get("/x", "Authorization", basic("ada", "ada-pw")).status);
            assertEquals("[first:none, beforeBasic:none, afterBasic:ada, afterAnonymous:ada]",
                    order.toString());
            order.clear();
            assertEquals(401, server.get("/x").status);
            assertEquals("[first:none, beforeBasic:none, afterBasic:none, "
                    + "afterAnonymous:anonymousUser]", order.toString());
            // The configurer's filter answers before the rules are asked.
            assertEquals("good day", server.get("/hi").body);
        }
        assertTrue(assertThrows(IllegalArgumentException.class, () -> SecuredServer.start(APP,
                http -> http.addFilterBefore(new Mark("x"), Mark.class).build())).getMessage()
                .contains("does not have a registered order"));
    }

    @Test
    @DisplayName("without a user store, cn1.security.user.* describes the one user")
    void theConfiguredUser() throws Exception {
        Properties settings = dev();
        settings.setProperty("cn1.security.user.name", "ops");
        settings.setProperty("cn1.security.user.password", "{bcrypt}" + ENCODER.encode("ops-pw"));
        settings.setProperty("cn1.security.user.roles", "OPS, ADMIN");
        SecuredServer.Chain chain = http -> http
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("OPS"))
                .httpBasic(Customizer.withDefaults()).csrf(csrf -> csrf.disable()).build();
        try (SecuredServer server = SecuredServer.start(settings, "test", new Object[0], APP, chain)) {
            assertEquals("ops [ROLE_OPS, ROLE_ADMIN] UserDetails",
                    server.get("/me", "Authorization", basic("ops", "ops-pw")).body);
            assertEquals(401, server.get("/me", "Authorization", basic("ops", "nope")).status);
        }
        // A clear-text password is a development convenience and works only there.
        settings.setProperty("cn1.security.user.password", "plain-pw");
        try (SecuredServer server = SecuredServer.start(settings, "production", new Object[0], APP,
                chain)) {
            assertEquals(401, server.get("/me", "Authorization", basic("ops", "plain-pw")).status);
        }
        try (SecuredServer server = SecuredServer.start(settings, "dev", new Object[0], APP, chain)) {
            assertEquals(200, server.get("/me", "Authorization", basic("ops", "plain-pw")).status);
        }
    }

    @Test
    @DisplayName("who is signed in survives the session store as plain values")
    void theSessionHoldsPlainValues() throws Exception {
        final Object[] stored = new Object[1];
        try (SecuredServer server = SecuredServer.start(dev(), "test", beans(),
                request -> {
                    stored[0] = request.getSession(true).getAttribute("SPRING_SECURITY_CONTEXT");
                    return APP.handle(request);
                },
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .formLogin(Customizer.withDefaults())
                        .csrf(csrf -> csrf.disable()).build())) {
            assertEquals(302, server.post("/login", "username=ray&password=ray-pw").status);
            assertEquals("ray [ROLE_USER, notes:read] UserDetails", server.get("/me").body);
            // Exactly what Json can write and read back: a map of strings, a list
            // of strings and a boolean. The database session store keeps nothing else.
            assertEquals("{name=ray, authorities=[ROLE_USER, notes:read], authenticated=true}",
                    String.valueOf(stored[0]));
            assertEquals(stored[0], com.codename1.backend.Json.parse(
                    com.codename1.backend.Json.write(stored[0])));
        }
    }
}
