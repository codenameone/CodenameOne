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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.HttpServer;
import com.codename1.backend.security.SecuredServer.Reply;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// The check on how a path is written belongs to the chain that claims the
/// request. A request no chain claims is answered as a server with no security
/// answers it, and a request cannot leave its chain by being misspelled.
class PathScopeTest {
    private static final HttpServer.Handler APP = request ->
            HttpServer.Response.text(200, "app " + request.pathFrom(0));

    private static SecuredServer guarded() throws Exception {
        return SecuredServer.start(APP,
                http -> http.securityMatcher("/api/secure/**", "/vault")
                        .authorizeHttpRequests(auth -> auth.anyRequest().denyAll())
                        .csrf(csrf -> csrf.disable()).build());
    }

    private static String describe(Reply reply) {
        StringBuilder sb = new StringBuilder().append(reply.status).append(' ')
                .append(reply.body);
        for (String[] header : reply.headers) {
            sb.append(" [").append(header[0]).append(": ").append(header[1]).append(']');
        }
        return sb.toString();
    }

    @Test
    @DisplayName("a path under no chain is answered as it is with no security at all")
    void anUnclaimedPathIsNotTheLayersToJudge() throws Exception {
        String[] targets = {"/open", "/open;v=1/x", "/open//x", "/open/./x", "/open/../other",
            "/open%2Fx", "/open%5Cx", "/open/%252e", "/open\\x", "/open/%3Bx", "/open/x;jsessionid=1",
            "/a/..", "/", "/OPEN", "/api/%2e%2e/secure", "/apiary/secure/x", "/API/SECURE/x", "/Vault"};
        // One process runs one backend: first the answers with no layer, then
        // the same requests with a chain that claims none of them.
        java.util.List<String> without = new java.util.ArrayList<String>();
        java.util.List<String> reachedWithout;
        try (SecuredServer plain = SecuredServer.startUnsecured(APP)) {
            for (String target : targets) {
                without.add(describe(plain.get(target)));
            }
            reachedWithout = plain.reached();
        }
        try (SecuredServer secured = guarded()) {
            for (int iter = 0 ; iter < targets.length ; iter++) {
                String target = targets[iter];
                Reply with = secured.get(target);
                assertEquals(without.get(iter), describe(with), target);
                // And it was the application that answered, not the layer.
                assertEquals(200, with.status, target);
                assertTrue(with.body.startsWith("app "), target + " -> " + with.body);
                assertNull(with.header("X-Frame-Options"), target);
            }
            assertEquals(reachedWithout, secured.reached());
        }
    }

    @Test
    @DisplayName("a path cannot leave its chain by being misspelled: the chain refuses it")
    void aMisspelledPathStaysWithItsChain() throws Exception {
        String[] targets = {
            // Claimed as sent.
            "/api/secure/..;/x", "/api/secure/x;jsessionid=1", "/api/secure//x",
            "/api/secure/%2e%2e/x", "/api/secure/x%00",
            // Claimed as something further on could read it.
            "//api/secure/x", "/api//secure/x", "/api/%2e%2e/api/secure/x",
            "/x/%2e%2e/api/secure/y", "/public/..%2Fapi/secure/x", "/public/..%2fapi%2Fsecure%2Fx",
            "/public/..;/api/secure/x", "/public/%2e%2e;x=1/api/secure/x",
            "/api;v=1/secure/x", "/api/secure;jsessionid=1", "/api\\secure\\x",
            "/api%5Csecure%5Cx", "/api/secure%2Fx", "/api%252Fsecure%252Fx",
            "/public/%252e%252e/api/secure/x", "/api/./secure/x", "/x/../api/secure",
            "/vault;jsessionid=1", "/vault/.", "/vault/x/..", "/x/..%2Fvault", "/vault%00.png",
            "/vault%3Bx"};
        try (SecuredServer server = guarded()) {
            for (String target : targets) {
                Reply reply = server.get(target);
                assertEquals(400, reply.status, target + " -> " + reply);
                assertEquals("Bad Request", reply.body, target);
            }
            // None of them reached the application.
            assertEquals("[]", server.reached().toString());
            // Spelled properly, the chain judges them: here, denies them.
            assertEquals(403, server.get("/api/secure/x").status);
            assertEquals(403, server.get("/api/secure").status);
            assertEquals(403, server.get("/vault").status);
        }
    }

    @Test
    @DisplayName("matching is case sensitive, as routing is: another case is another route")
    void anotherCaseIsAnotherRoute() throws Exception {
        try (SecuredServer server = guarded()) {
            // No route of the application is spelled this way either: what
            // serves /api/secure/x does not serve /API/secure/x, so there is
            // nothing behind the chain to reach. The handler here answers
            // every path, which is how the test sees the request arrive.
            assertEquals("app /API/secure/x", server.get("/API/secure/x").body);
            assertEquals("app /Api/Secure/x", server.get("/Api/Secure/x").body);
            // An escaped letter is the letter, and stays with the chain.
            assertEquals(403, server.get("/%61pi/secure/x").status);
            assertEquals(403, server.get("/api/s%65cure/x").status);
        }
    }

    @Test
    @DisplayName("the readings of a misspelled path: decoded, and with its segments resolved")
    void readings() {
        assertEquals(Arrays.asList("/public/../api/secure/x", "/api/secure/x"),
                FilterChainEngine.readings("/public/..%2Fapi/secure/x"));
        assertEquals(Arrays.asList("/public/../api/secure", "/api/secure"),
                FilterChainEngine.readings("/public/%252e%252e/api;v=1/secure"));
        assertEquals(Arrays.asList("/api/secure/x/..", "/api/secure", "/api/secure/"),
                FilterChainEngine.readings("/api/secure/x/.."));
        assertEquals(Arrays.asList("//api/secure", "/api/secure"),
                FilterChainEngine.readings("//api\\secure"));
        assertEquals(Arrays.asList("/vault.png", "/vault"),
                FilterChainEngine.readings("/vault%00.png"));
        assertEquals(Arrays.asList("/../..", "/"), FilterChainEngine.readings("/../.."));
    }
}
