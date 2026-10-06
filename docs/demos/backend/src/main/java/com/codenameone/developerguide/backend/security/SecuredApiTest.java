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
package com.codenameone.developerguide.backend.security;

// tag::backend-security-test[]
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.WithMockUser;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static com.codename1.backend.test.SecurityMockMvcRequestPostProcessors.apiKey;
import static com.codename1.backend.test.SecurityMockMvcRequestPostProcessors.csrf;
import static com.codename1.backend.test.SecurityMockMvcRequestPostProcessors.httpBasic;
import static com.codename1.backend.test.SecurityMockMvcRequestPostProcessors.jwt;
import static com.codename1.backend.test.SecurityMockMvcRequestPostProcessors.user;

@BackendTest
class SecuredApiTest {
    @Autowired
    private MockMvc mvc;

    @Test
    @WithMockUser(username = "ada", roles = "ADMIN")
    void anAdminSeesTheReport() throws Exception {
        mvc.perform(get("/admin/report")).andExpect(status().isOk());
    }

    @Test
    void aStrangerIsSentToSignIn() throws Exception {
        mvc.perform(get("/admin/report")).andExpect(status().isFound());
    }

    @Test
    void aTokenNeedsTheScope() throws Exception {
        mvc.perform(get("/api/orders/7").with(jwt().subject("svc").scopes("orders:read")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/orders/7").with(jwt().scopes("profile")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/orders/7").with(apiKey("billing").scopes("orders:read")))
                .andExpect(status().isOk());
    }

    @Test
    void aPostNeedsTheCsrfToken() throws Exception {
        mvc.perform(post("/notes").with(user("ada").roles("EDITOR")).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/notes").with(user("ada").roles("EDITOR")).with(csrf())
                        .content("{}"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void realCredentialsAreChecked() throws Exception {
        mvc.perform(get("/internal/health").with(httpBasic("svc", "wrong")))
                .andExpect(status().isUnauthorized());
    }
}
// end::backend-security-test[]
