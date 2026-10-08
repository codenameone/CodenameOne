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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.HttpServer;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// What the request matchers match. A matcher is asked about a real request, so
/// the cases are put to a server whose chain answers with the names of the
/// matchers that matched it.
class RequestMatcherTest {
    /// {name, matcher}: every one is asked about every request.
    private static final Object[][] MATCHERS = {
        {"exact", AntPathRequestMatcher.antMatcher("/files")},
        {"star", AntPathRequestMatcher.antMatcher("/files/*")},
        {"suffix", AntPathRequestMatcher.antMatcher("/files/*.txt")},
        {"question", AntPathRequestMatcher.antMatcher("/files/?.txt")},
        {"deep", AntPathRequestMatcher.antMatcher("/api/**")},
        {"middle", AntPathRequestMatcher.antMatcher("/api/**/edit")},
        {"var", AntPathRequestMatcher.antMatcher("/users/{id}")},
        {"vars", AntPathRequestMatcher.antMatcher("/users/{id}/notes/{note}")},
        {"partvar", AntPathRequestMatcher.antMatcher("/img/{name}.png")},
        {"post", AntPathRequestMatcher.antMatcher("POST", "/api/**")},
        {"all", AntPathRequestMatcher.antMatcher("/**")},
        {"root", AntPathRequestMatcher.antMatcher("/")},
        {"method", RequestMatchers.method("DELETE")},
        {"header", RequestMatchers.header("X-Kind", null)},
        {"headerValue", RequestMatchers.header("X-Kind", "b")},
        {"and", RequestMatchers.allOf(AntPathRequestMatcher.antMatcher("/api/**"),
                RequestMatchers.method("DELETE"))},
        {"or", RequestMatchers.anyOf(AntPathRequestMatcher.antMatcher("/files"),
                AntPathRequestMatcher.antMatcher("/users/{id}"))},
        {"not", RequestMatchers.not(AntPathRequestMatcher.antMatcher("/api/**"))},
    };

    private static SecuredServer server;

    @BeforeAll
    static void start() throws Exception {
        server = SecuredServer.start(request -> HttpServer.Response.text(200, "unreached"),
                http -> {
                    http.csrf(csrf -> csrf.disable());
                    http.addFilterBefore((request, chain) -> {
                        StringBuilder sb = new StringBuilder();
                        for (Object[] named : MATCHERS) {
                            RequestMatcher matcher = (RequestMatcher) named[1];
                            RequestMatcher.MatchResult result = matcher.matcher(request);
                            if (result.isMatch() != matcher.matches(request)) {
                                throw new IllegalStateException(named[0] + " disagrees with itself");
                            }
                            if (result.isMatch()) {
                                Map<String, String> variables = result.getVariables();
                                sb.append(' ').append(named[0])
                                        .append(variables.isEmpty() ? "" : variables.toString());
                            }
                        }
                        return HttpServer.Response.text(200, sb.toString().trim());
                    }, AuthorizationFilter.class);
                    return http.build();
                });
    }

    @AfterAll
    static void stop() {
        server.close();
    }

    private static String matched(String method, String target, String... headers) throws Exception {
        return server.call(method, target, null, null, headers).body;
    }

    @Test
    @DisplayName("* and ? stay within a segment; ** spans segments, none included")
    void wildcards() throws Exception {
        assertEquals("exact all or not", matched("GET", "/files"));
        // A trailing slash is part of the path: /files/ is not /files.
        assertEquals("star all not", matched("GET", "/files/"));
        assertEquals("star suffix all not", matched("GET", "/files/report.txt"));
        assertEquals("star suffix question all not", matched("GET", "/files/a.txt"));
        assertEquals("star all not", matched("GET", "/files/a.pdf"));
        assertEquals("all not", matched("GET", "/files/a/b.txt"));
        assertEquals("deep all", matched("GET", "/api"));
        assertEquals("deep all", matched("GET", "/api/"));
        assertEquals("deep all", matched("GET", "/api/users/7"));
        assertEquals("deep middle all", matched("GET", "/api/users/7/edit"));
        assertEquals("deep middle all", matched("GET", "/api/edit"));
        // A prefix that is not a whole segment is another path.
        assertEquals("all not", matched("GET", "/apiary"));
        assertEquals("all root not", matched("GET", "/"));
        // Matching is case sensitive, as routing is.
        assertEquals("all not", matched("GET", "/FILES"));
    }

    @Test
    @DisplayName("the query takes no part, and an escaped unreserved character is the character")
    void theCanonicalPathIsCompared() throws Exception {
        assertEquals("exact all or not", matched("GET", "/files?x=/api/secret"));
        assertEquals("deep all", matched("GET", "/%61pi/users"));
    }

    @Test
    @DisplayName("{name} binds one segment, or the rest of one")
    void variables() throws Exception {
        assertEquals("var{id=42} all or{id=42} not", matched("GET", "/users/42"));
        assertEquals("vars{id=42, note=n-1} all not", matched("GET", "/users/42/notes/n-1"));
        assertEquals("all not", matched("GET", "/users/42/notes"));
        assertEquals("all not", matched("GET", "/users/"));
        assertEquals("partvar{name=logo} all not", matched("GET", "/img/logo.png"));
        assertEquals("all not", matched("GET", "/img/.png"));
    }

    @Test
    @DisplayName("a method matcher compares the method; and, or and not combine")
    void methodsAndCombinators() throws Exception {
        assertEquals("deep post all", matched("POST", "/api/x"));
        assertEquals("deep all method and", matched("DELETE", "/api/x"));
        assertEquals("all method not", matched("DELETE", "/other"));
        assertEquals("all header not", matched("GET", "/other", "X-Kind", "a"));
        assertEquals("all header headerValue not", matched("GET", "/other", "X-Kind", "b"));
    }

    @Test
    @DisplayName("a pattern that could never mean what it says is refused when it is written")
    void badPatterns() {
        assertEquals("The pattern \"files\" must start with /: a request path always does",
                assertThrows(IllegalArgumentException.class,
                        () -> new AntPathRequestMatcher("files")).getMessage());
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new AntPathRequestMatcher("/a/{id")).getMessage()
                .contains("opens a { it never closes"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new AntPathRequestMatcher("/a/{id:[0-9]+}")).getMessage()
                .contains("regular expression, which is not supported"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new AntPathRequestMatcher("/a/**b")).getMessage()
                .contains("** must be a whole segment"));
        assertEquals("matchers cannot contain null values",
                assertThrows(IllegalArgumentException.class,
                        () -> RequestMatchers.anyOf((RequestMatcher) null)).getMessage());
    }

    @Test
    @DisplayName("a path spelled to slip past a rule is refused before any rule sees it")
    void malformedPathsAreRefused() throws Exception {
        for (String target : new String[] {"/api;v=1/users", "/api//users", "/api/./users",
            "/api/../admin", "/api/..", "/api/%2e%2e/admin", "/api%2Fusers", "/api%5Cusers",
            "/api/%252e", "/api\\users", "/api/%00", "/api/%3Bx"}) {
            SecuredServer.Reply reply = server.call("GET", target, null, null);
            assertEquals(400, reply.status, target + " -> " + reply);
            assertEquals("Bad Request", reply.body);
        }
        // What merely looks like one of those is an ordinary path.
        assertEquals("all not", matched("GET", "/.well-known/security.txt"));
        assertEquals("deep all", matched("GET", "/api/..hidden"));
        assertEquals("deep all", matched("GET", "/api/a.b"));
    }
}
