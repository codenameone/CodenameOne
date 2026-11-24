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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.codename1.backend.Backend;
import com.codename1.backend.HttpServer;
import com.codename1.backend.HttpSession;
import com.codename1.backend.Tracing;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/// Issue 6310: `Backend.dispatch(Request, Authentication)` answers a request in
/// process as a principal the caller vouches for -- the chain's authorization
/// rules run, its authentication does not.
class DispatchAsUserTest {
    /// Echoes who it is answering as; a route template for the /api paths, as a
    /// generated router reports it; and, on /outer, a nested dispatch.
    private static final HttpServer.Handler APP = request -> {
        String path = request.pathFrom(0);
        if ("/missing".equals(path)) {
            return null;
        }
        if ("/error".equals(path)) {
            // The error route, as its generated router reports itself.
            Tracing.route("/error");
            return HttpServer.Response.text(404, "error page");
        }
        if ("/touch-session".equals(path)) {
            request.getSession(true).setAttribute("seen", "yes");
        }
        if ("/read-session".equals(path)) {
            HttpSession session = request.getSession(false);
            return HttpServer.Response.text(200, session == null ? "no session" : "session " + session.getAttribute("seen"));
        }
        if (path.startsWith("/api/")) {
            Tracing.route(path.startsWith("/api/admin/") ? "/api/admin/{x}" : "/api/apps/{id}");
        }
        if ("/outer".equals(path)) {
            HttpServer.Response inner = Backend.running().dispatch(
                    HttpServer.Request.of("GET", "/api/apps/7", null, null), user("ann", "ROLE_USER"));
            Authentication after = SecurityContextHolder.getContext().getAuthentication();
            return HttpServer.Response.text(200, inner.getStatus() + " " + text(inner) + " | outer "
                    + (after == null ? "none" : after.getName()));
        }
        Authentication who = SecurityContextHolder.getContext().getAuthentication();
        return HttpServer.Response.text(200, request.getMethod() + " " + path + " as "
                + (who == null ? "none" : who.getName()));
    };

    static String text(HttpServer.Response r) {
        return new String(r.getBody(), java.nio.charset.StandardCharsets.UTF_8);
    }

    static Authentication user(String name, String role) {
        return UsernamePasswordAuthenticationToken.authenticated(name, null,
                Collections.singletonList(new SimpleGrantedAuthority(role)));
    }

    private static SecuredServer start() throws Exception {
        return SecuredServer.start(APP,
                http -> http.authorizeHttpRequests(auth -> auth
                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                .requestMatchers("/api/**").authenticated()
                                .anyRequest().permitAll())
                        // Mallory's Basic credentials are VALID over the wire,
                        // which is what makes ignoring them meaningful below.
                        .userDetailsService(new com.codename1.backend.security.core.userdetails
                                .InMemoryUserDetailsManager(com.codename1.backend.security.core
                                        .userdetails.User.withUsername("mallory")
                                        .password("{noop}secret").roles("ADMIN").build()))
                        .httpBasic(Customizer.withDefaults())
                        // The application's own rule, as a tenant check would be.
                        .addFilterBefore((request, chain) -> request.pathFrom(0).startsWith("/api/tenant/")
                                        && !"acme".equals(request.getHeader("X-Tenant"))
                                        ? HttpServer.Response.text(403, "wrong tenant") : chain.doFilter(request),
                                AuthorizationFilter.class)
                        // The application's own sign-in filter, as a JWT one would
                        // be: it refuses a request with no token. Under dispatch it
                        // must not run, or it would undo the vouched principal.
                        .addFilterBefore((request, chain) -> request.getHeader("X-Token") == null
                                        && request.pathFrom(0).startsWith("/api/")
                                        && request.getHeader("Authorization") == null
                                        ? HttpServer.Response.text(401, "no token") : chain.doFilter(request),
                                BasicAuthenticationFilter.class)
                        // A filter after Basic, where a tenant or audit filter
                        // reads the principal: it runs under dispatch too.
                        .addFilterAfter((request, chain) -> "/api/audited".equals(request.pathFrom(0))
                                        ? HttpServer.Response.text(200, "audited") : chain.doFilter(request),
                                BasicAuthenticationFilter.class)
                        .build());
    }

    private static HttpServer.Response as(String method, String target, Authentication who,
                                          Map<String, String> headers) throws Exception {
        return Backend.running().dispatch(HttpServer.Request.of(method, target, headers, null), who);
    }

    @Test
    void runsTheChainsAuthorizationAsTheGivenUserAndReportsTheRoute() throws Exception {
        try (SecuredServer server = start()) {
            HttpServer.Response r = as("GET", "/api/apps/7", user("ann", "ROLE_USER"), null);
            assertEquals(200, r.getStatus());
            assertEquals("GET /api/apps/7 as ann", text(r));
            assertEquals("/api/apps/{id}", r.getMatchedRoute());

            // Authorization still applies: a user without the role is refused,
            // with a plain 403 rather than a login challenge or redirect.
            r = as("GET", "/api/admin/x", user("ann", "ROLE_USER"), null);
            assertEquals(403, r.getStatus());
            assertNull(r.getMatchedRoute());
            r = as("GET", "/api/admin/x", user("root", "ROLE_ADMIN"), null);
            assertEquals("GET /api/admin/x as root", text(r));
            assertEquals("/api/admin/{x}", r.getMatchedRoute());

            // An anonymous principal is told to sign in.
            Authentication anonymous = new AnonymousAuthenticationToken("k", "anonymousUser",
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
            assertEquals(401, as("GET", "/api/apps/7", anonymous, null).getStatus());

            // Nothing authenticates the request itself: Basic credentials in its
            // headers do not replace the principal, and CSRF does not refuse a
            // POST that carries no token.
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Authorization", "Basic " + java.util.Base64.getEncoder()
                    .encodeToString("mallory:secret".getBytes("UTF-8")));
            r = as("POST", "/api/apps/7", user("ann", "ROLE_USER"), headers);
            assertEquals(200, r.getStatus(), text(r));
            assertEquals("POST /api/apps/7 as ann", text(r));

            // The application's own filter still runs under dispatch.
            r = as("GET", "/api/tenant/1", user("ann", "ROLE_USER"), null);
            assertEquals(403, r.getStatus());
            assertEquals("wrong tenant", text(r));
            Map<String, String> tenant = new HashMap<String, String>();
            tenant.put("X-Tenant", "acme");
            assertEquals(200, as("GET", "/api/tenant/1", user("ann", "ROLE_USER"), tenant).getStatus());
            assertEquals("audited", text(as("GET", "/api/audited", user("ann", "ROLE_USER"), null)));

            // No route: a 404, no template.
            r = as("GET", "/nowhere", user("ann", "ROLE_USER"), null);
            assertEquals("GET /nowhere as ann", text(r));
            assertNull(r.getMatchedRoute());

            // Served by the error route: still no template of the request's own
            // -- the error page's is not the route the request matched.
            r = as("GET", "/missing", user("ann", "ROLE_USER"), null);
            assertEquals(404, r.getStatus());
            assertEquals("error page", text(r));
            assertNull(r.getMatchedRoute());

            // A handler may touch a session; the bracket releases it.
            r = as("GET", "/touch-session", user("ann", "ROLE_USER"), null);
            assertEquals(200, r.getStatus());

            // A forwarded Cookie header does not hand the dispatch the session
            // it names, though the same cookie over the wire finds it.
            SecuredServer.Reply touched = server.get("/touch-session");
            String cookie = touched.header("Set-Cookie");
            assertNotNull(cookie, touched.status + " " + touched.body);
            cookie = cookie.substring(0, cookie.indexOf(';'));
            assertEquals("session yes", server.get("/read-session", "Cookie", cookie).body);
            Map<String, String> withCookie = new HashMap<String, String>();
            withCookie.put("Cookie", cookie);
            assertEquals("no session", text(as("GET", "/read-session", user("ann", "ROLE_USER"), withCookie)));

            // Over the wire nothing changed: the same path wants credentials.
            assertEquals(401, server.get("/api/apps/7").status);
            assertEquals("GET /api/apps/7 as mallory",
                    server.get("/api/apps/7", "Authorization", headers.get("Authorization")).body);
            assertThrows(IllegalArgumentException.class, () -> as("GET", "/x", null, null));
        }
    }

    @Test
    void aNestedDispatchPutsTheOuterRequestBack() throws Exception {
        try (SecuredServer server = start()) {
            SecuredServer.Reply reply = server.get("/outer");
            assertEquals(200, reply.status);
            // The outer request was anonymous and is again once the inner one ends.
            assertEquals("200 GET /api/apps/7 as ann | outer anonymousUser", reply.body);
        }
    }
}
