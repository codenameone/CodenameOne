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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static com.codename1.backend.test.MockMvcRequestBuilders.delete;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.content;
import static com.codename1.backend.test.MockMvcResultMatchers.header;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static com.codename1.backend.test.SecurityMockMvcRequestPostProcessors.jwt;

/// The routes under `/api/secure`: what each asks of a caller, with the token
/// stated by the test rather than issued, so a failure here is the chain's rules
/// or the route and not the sign-in. [SignInFlowTest] is the same routes with a
/// token the server really issued.
@BackendTest
class SecureApiTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void withoutATokenEveryRouteIsChallenged() throws Exception {
        String[] routes = {"/api/secure/whoami", "/api/secure/notes", "/api/secure/admin",
                "/api/secure/summary/ada", "/api/secure/nothing-here"};
        for (String route : routes) {
            mvc.perform(get(route))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate", "Bearer"));
        }
        mvc.perform(post("/api/secure/notes").contentType("application/json")
                        .content("{\"title\":\"t\",\"body\":\"b\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }

    @Test
    void aTokenThatIsNotOneIsRefusedWithTheReason() throws Exception {
        String challenge = mvc.perform(get("/api/secure/notes")
                        .header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getHeader("WWW-Authenticate");
        assertTrue(challenge.startsWith("Bearer error=\"invalid_token\""), challenge);
        // The legacy probe's fixed token is not one of this server's either: it is
        // only ever good at /api/auth/bearer, which no chain guards.
        mvc.perform(get("/api/secure/notes").header("Authorization", "Bearer token-123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theReadScopeReadsTheRowsTheMigrationsSeeded() throws Exception {
        // Nothing but migration V1 creates this table and nothing but V2 fills it,
        // so these three rows are start-up migration having run.
        mvc.perform(get("/api/secure/notes").with(jwt().subject("ada").scopes("notes:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(Long.valueOf(3)))
                .andExpect(jsonPath("$[0].id").value(Long.valueOf(1)))
                .andExpect(jsonPath("$[0].title").value("first"))
                .andExpect(jsonPath("$[0].body").value("seeded by migration V2"))
                .andExpect(jsonPath("$[1].title").value("second"))
                .andExpect(jsonPath("$[2].title").value("third"));
    }

    @Test
    void aTokenWithoutTheScopeIsForbidden() throws Exception {
        // The 403 only: jwt() says a token was accepted without sending one, and
        // the insufficient_scope challenge is written for a request that carries
        // a bearer token. SignInFlowTest reads it off a real one.
        mvc.perform(get("/api/secure/notes").with(jwt().subject("ada").scopes("openid", "profile")))
                .andExpect(status().isForbidden());
        // The read scope is not the write scope.
        mvc.perform(get("/api/secure/admin").with(jwt().subject("ada").scopes("notes:read")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/secure/notes").with(jwt().subject("ada").scopes("notes:read"))
                        .contentType("application/json")
                        .content("{\"title\":\"t\",\"body\":\"b\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/secure/admin").with(jwt().subject("ada").scopes("notes:write")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admin").value("ada"));
    }

    @Test
    void anyValidTokenIsToldWhoItIs() throws Exception {
        mvc.perform(get("/api/secure/whoami").with(jwt().subject("ada").scopes("openid")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ada"))
                .andExpect(jsonPath("$.authorities[0]").value("SCOPE_openid"))
                .andExpect(jsonPath("$.scope").value("openid"));
    }

    @Test
    void theMethodsOwnRuleIsAskedAfterTheChainsRule() throws Exception {
        // The chain only wants a signed-in caller here; NoteService.summaryFor
        // wants the read scope and the caller to be the owner.
        mvc.perform(get("/api/secure/summary/ada")
                        .with(jwt().subject("ada").scopes("notes:read")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"owner\":\"ada\",\"notes\":3}"));
        mvc.perform(get("/api/secure/summary/grace")
                        .with(jwt().subject("ada").scopes("notes:read")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/secure/summary/ada").with(jwt().subject("ada").scopes("openid")))
                .andExpect(status().isForbidden());
    }

    @Test
    void theWriteScopeAddsANoteAndTakesItBack() throws Exception {
        mvc.perform(post("/api/secure/notes").with(jwt().subject("ada").scopes("notes:write"))
                        .contentType("application/json")
                        .content("{\"title\":\"fourth\",\"body\":\"added by a test\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(Long.valueOf(4)))
                .andExpect(jsonPath("$.title").value("fourth"));
        mvc.perform(delete("/api/secure/notes/4").with(jwt().subject("ada").scopes("notes:read")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/secure/notes/4").with(jwt().subject("ada").scopes("notes:write")))
                .andExpect(status().isNoContent());
        // A seeded row is not a test's to remove.
        mvc.perform(delete("/api/secure/notes/1").with(jwt().subject("ada").scopes("notes:write")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/secure/notes").with(jwt().subject("ada").scopes("notes:read")))
                .andExpect(jsonPath("$.length()").value(Long.valueOf(3)));
    }
}
