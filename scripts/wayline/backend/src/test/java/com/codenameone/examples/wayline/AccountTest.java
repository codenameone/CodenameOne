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
package com.codenameone.examples.wayline;

import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.wayline.account.DemoAccounts;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Opening an account, signing in to it and proving its phone number, through
/// the same requests the app makes.
@BackendTest
class AccountTest {
    @Autowired
    private MockMvc mvc;

    private static String registration(String email, String password, boolean driver) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password
                + "\",\"displayName\":\"Test Person\",\"phone\":\"+1 (555) 010-9999\",\"driver\":"
                + driver + ",\"vehicle\":\"Grey Honda Civic\",\"plate\":\"TST 1\"}";
    }

    private String bearer(String username, String password) throws Exception {
        return "Bearer " + SignIn.token(mvc, username, password);
    }

    @Test
    void anAccountIsOpenedThenSignedInTo() throws Exception {
        mvc.perform(post("/api/account/register").contentType("application/json")
                        .content(registration("New.Rider@Example.com", "a-long-password", false)))
                .andExpect(status().isOk())
                // The name is the address in lower case, and the phone number is
                // stored in one form however it was typed.
                .andExpect(jsonPath("$.username").value("new.rider@example.com"))
                .andExpect(jsonPath("$.phone").value("+15550109999"))
                .andExpect(jsonPath("$.rider").value(true))
                .andExpect(jsonPath("$.driver").value(false))
                .andExpect(jsonPath("$.admin").value(false))
                .andExpect(jsonPath("$.phoneVerified").value(false));

        // Signing in is not case sensitive about the address either.
        mvc.perform(get("/api/me").header("Host", SignIn.HOST).header("Authorization",
                        bearer("NEW.RIDER@example.com", "a-long-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new.rider@example.com"))
                .andExpect(jsonPath("$.displayName").value("Test Person"));
    }

    @Test
    void anAccountThatCannotBeOpenedSaysWhy() throws Exception {
        mvc.perform(post("/api/account/register").contentType("application/json")
                        .content(registration("short@example.com", "short", false)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/account/register").contentType("application/json")
                        .content(registration("not-an-address", "a-long-password", false)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/account/register").contentType("application/json")
                        .content(registration(DemoAccounts.RIDER, "a-long-password", false)))
                .andExpect(status().isConflict());
    }

    @Test
    void aWrongPasswordSignsNobodyIn() throws Exception {
        assertNull(SignIn.session(mvc, DemoAccounts.RIDER, "not-the-password"));
        assertNotNull(SignIn.session(mvc, DemoAccounts.RIDER, DemoAccounts.PASSWORD));
    }

    @Test
    void theApiWantsAToken() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());
        // A session is for signing in. It is not a way into the API.
        mvc.perform(get("/api/me").cookie("CN1SESSION",
                        SignIn.session(mvc, DemoAccounts.RIDER, DemoAccounts.PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aPhoneNumberIsProvedWithTheCodeThatWasSent() throws Exception {
        mvc.perform(post("/api/account/register").contentType("application/json")
                        .content(registration("phone@example.com", "a-long-password", true)))
                .andExpect(status().isOk())
                // Asking to drive opens an application. It does not make a driver.
                .andExpect(jsonPath("$.driver").value(false))
                .andExpect(jsonPath("$.driverStatus").value("draft"));
        String bearer = bearer("phone@example.com", "a-long-password");

        String sent = mvc.perform(post("/api/account/phone/start").header("Host", SignIn.HOST)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map challenge = Json.parseObject(sent);
        // A test server sends no text message, so it says what the code was.
        String code = (String) challenge.get("demoCode");
        assertNotNull(code);
        assertEquals(6, code.length());

        // A second code straight away is refused: each is a text message.
        mvc.perform(post("/api/account/phone/start").header("Host", SignIn.HOST)
                        .header("Authorization", bearer))
                .andExpect(status().isTooManyRequests());

        String wrong = "000000".equals(code) ? "000001" : "000000";
        mvc.perform(post("/api/account/phone/verify").header("Host", SignIn.HOST)
                        .header("Authorization", bearer).contentType("application/json")
                        .content("{\"code\":\"" + wrong + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/account/phone/verify").header("Host", SignIn.HOST)
                        .header("Authorization", bearer).contentType("application/json")
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneVerified").value(true));
        // Used up: the same code is not good twice.
        mvc.perform(post("/api/account/phone/verify").header("Host", SignIn.HOST)
                        .header("Authorization", bearer).contentType("application/json")
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
