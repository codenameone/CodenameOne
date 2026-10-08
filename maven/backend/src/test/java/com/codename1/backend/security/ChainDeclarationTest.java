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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Base64;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.SecuredServer.Reply;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// A chain has what it declares. What every chain used to be given -- sign-out,
/// the memory of where a request was going -- now comes with the sign-in that
/// needs it, so that a server which declares neither carries neither; these are
/// the behaviours that moved. That the code is really absent from such a
/// server is measured on translated binaries, by BackendLinkingTest.
class ChainDeclarationTest {
    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            return HttpServer.Response.text(200, "app " + request.getMethod() + " "
                    + request.pathFrom(0));
        }
    };

    private static Object[] beans() {
        return new Object[] {new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}ada-pw").roles("USER").build())};
    }

    private static String basic() throws Exception {
        return "Basic " + Base64.encode("ada:ada-pw".getBytes("UTF-8"));
    }

    @Test
    @DisplayName("sign-out comes with the form login, and is absent from a chain without one")
    void logoutComesWithTheFormLogin() throws Exception {
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev", beans(),
                APP, http -> http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                        .httpBasic(Customizer.withDefaults())
                        .csrf(csrf -> csrf.disable()).build())) {
            // Nothing takes POST /logout: it reaches the application like any path.
            Reply reply = server.call("POST", "/logout", null, null);
            assertEquals("200 app POST /logout", reply.status + " " + reply.body);
        }
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev", beans(),
                APP, http -> http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                        .formLogin(Customizer.withDefaults())
                        .csrf(csrf -> csrf.disable()).build())) {
            Reply reply = server.call("POST", "/logout", null, null);
            assertEquals("302 /login?logout", reply.status + " " + reply.header("Location"));
        }
        // Asked for by name, a chain without a form has it; and one with a form
        // that turned it off has not.
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev", beans(),
                APP, http -> http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                        .httpBasic(Customizer.withDefaults())
                        .logout(logout -> logout.logoutSuccessUrl("/bye"))
                        .csrf(csrf -> csrf.disable()).build())) {
            assertEquals("/bye", server.call("POST", "/logout", null, null).header("Location"));
        }
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev", beans(),
                APP, http -> http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                        .logout(logout -> logout.disable())
                        .formLogin(Customizer.withDefaults())
                        .csrf(csrf -> csrf.disable()).build())) {
            assertEquals("200 app POST /logout", server.call("POST", "/logout", null, null).status
                    + " " + server.call("POST", "/logout", null, null).body);
        }
    }

    @Test
    @DisplayName("a chain that keeps a session checks CSRF; a stateless one only when asked")
    void csrfFollowsTheSession() throws Exception {
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev", beans(),
                APP, http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .httpBasic(Customizer.withDefaults()).build())) {
            assertEquals(403, server.call("POST", "/orders", null, null,
                    "Authorization", basic()).status);
        }
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev", beans(),
                APP, http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .httpBasic(Customizer.withDefaults())
                        .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)).build())) {
            Reply reply = server.call("POST", "/orders", null, null, "Authorization", basic());
            assertEquals("200 app POST /orders", reply.status + " " + reply.body);
            assertTrue(reply.headers("Set-Cookie").isEmpty(), reply.toString());
        }
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev", beans(),
                APP, http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .httpBasic(Customizer.withDefaults())
                        .csrf(csrf -> csrf.csrfTokenRepository(new CookieCsrfTokenRepository()))
                        .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)).build())) {
            assertEquals(403, server.call("POST", "/orders", null, null,
                    "Authorization", basic()).status);
        }
    }

    @Test
    @DisplayName("a filter is placed against one of the layer's own by its class, declared or not")
    void filtersArePlacedByClass() throws Exception {
        final List<String> seen = new ArrayList<String>();
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev", beans(),
                APP, http -> http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                        // Neither a form login nor a token filter is in this
                        // chain; their places are known all the same.
                        .addFilterBefore(new Named("before-form", seen),
                                UsernamePasswordAuthenticationFilter.class)
                        .addFilterAfter(new Named("after-bearer", seen),
                                BearerTokenAuthenticationFilter.class)
                        .addFilterBefore(new Named("first", seen),
                                SecurityContextHolderFilter.class)
                        // A class the chain was given is placed against too:
                        // where its first instance went, just before the form.
                        .addFilterAfter(new Named("after-named", seen), Named.class)
                        .build())) {
            assertEquals(200, server.get("/x").status);
            assertEquals("[first, before-form, after-named, after-bearer]", seen.toString());
        }
    }

    private static final class Named implements SecurityFilter {
        private final String name;
        private final List<String> seen;

        Named(String name, List<String> seen) {
            this.name = name;
            this.seen = seen;
        }

        @Override
        public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
                throws Exception {
            seen.add(name);
            return chain.doFilter(request);
        }
    }
}
