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
package com.codenameone.examples.hellocodenameone.backend;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.options;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.content;
import static com.codename1.backend.test.MockMvcResultMatchers.header;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;

/// What every device leg relied on before the server had a sign-in half, held
/// still: none of it is under a security chain, so none of it asks who is calling
/// and none of it carries a header a chain would add. The probe and pet tests
/// assert the answers themselves; this asserts that the chains stayed out.
@BackendTest
class LegacySurfaceTest {
    /// Headers a chain writes on everything it guards.
    private static final String[] CHAIN_HEADERS = {"X-Frame-Options", "X-Content-Type-Options",
            "X-XSS-Protection", "Pragma", "Expires", "WWW-Authenticate"};

    @Autowired
    private MockMvc mvc;

    private void assertNoChain(MockResponse response, String route) {
        for (String name : CHAIN_HEADERS) {
            assertNull(response.getHeader(name), name + " on " + route);
        }
    }

    @Test
    void theOpenRoutesAnswerAnyoneAndCarryNothingOfAChain() throws Exception {
        String[] routes = {"/api/health", "/api/hello/Ada", "/api/pets", "/api/json/map",
                "/api/echo?a=1", "/api/big?size=16", "/api/redirect/0", "/api/session/count"};
        for (String route : routes) {
            MockResponse response = mvc.perform(get(route))
                    .andExpect(status().isOk())
                    .andReturn().getResponse();
            assertNoChain(response, route);
        }
        // A cache directive of the route's own is not replaced by a chain's.
        mvc.perform(get("/api/headers"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("ETag", "\"v1\""));
    }

    @Test
    void aPostNeedsNoCsrfTokenOutsideAChain() throws Exception {
        // Routes that keep nothing: a compiled run shares one server between the
        // test classes, and a pet left behind here would be PetApiTest's to trip on.
        mvc.perform(post("/api/echo").contentType("text/plain").content("x"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.body").value("x"));
        mvc.perform(post("/api/raw").contentType("application/octet-stream").content("abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(Long.valueOf(3)));
    }

    @Test
    void theProbesOwnCredentialsAreStillTheProbes() throws Exception {
        // These two routes check a fixed header themselves; neither the Basic
        // sign-in nor the resource server's bearer tokens reach them.
        MockResponse challenged = mvc.perform(get("/api/auth/basic"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"probe\""))
                .andExpect(content().string("who are you?"))
                .andReturn().getResponse();
        assertNull(challenged.getHeader("X-Frame-Options"));
        mvc.perform(get("/api/auth/basic").header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isOk())
                .andExpect(content().string("user"));
        mvc.perform(get("/api/auth/bearer").header("Authorization", "Bearer token-123"))
                .andExpect(status().isOk())
                .andExpect(content().string("token accepted"));
        mvc.perform(get("/api/auth/bearer"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void aPathUnderNoChainIsNotJudgedForHowItIsWritten() throws Exception {
        // Each of these was answered 400 by the security layer while its path
        // check ran for every request. None is under a chain, so each is the
        // routers' to answer again: a route that is not there is a 404.
        String[] spelled = {"/api/pets;v=1", "/api//pets", "/api/./pets", "/api/nothing/..",
            "/nothing%2F1", "/nothing/%3B", "/nothing/a;b"};
        for (String path : spelled) {
            MockResponse response = mvc.perform(get(path)).andReturn().getResponse();
            assertTrue(response.getStatus() == 404 || response.getStatus() == 200,
                    path + " answered " + response.getStatus());
            assertEquals(null, response.getHeader("X-Frame-Options"), path);
        }
        // Spelled to slip into a chain's path, they stay the chain's to refuse.
        for (String path : new String[] {"/api/pets/..%2Fsecure/notes", "//api/secure/notes",
            "/api/secure/notes;x=1", "/x/../oauth2/token"}) {
            mvc.perform(get(path)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void theBrowserLegsPreflightIsAnsweredOnBothSidesOfTheChains() throws Exception {
        String[] routes = {"/api/pets", "/api/secure/notes", "/oauth2/token"};
        for (String route : routes) {
            MockResponse response = mvc.perform(options(route)
                            .header("Origin", "http://localhost:9000")
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "authorization, content-type"))
                    .andExpect(status().isNoContent())
                    .andReturn().getResponse();
            assertEquals("*", response.getHeader("Access-Control-Allow-Origin"), route);
            assertEquals("authorization, content-type",
                    response.getHeader("Access-Control-Allow-Headers"), route);
        }
        // And the answer to the request the preflight was for can be read.
        mvc.perform(get("/api/secure/notes").header("Origin", "http://localhost:9000"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }
}
