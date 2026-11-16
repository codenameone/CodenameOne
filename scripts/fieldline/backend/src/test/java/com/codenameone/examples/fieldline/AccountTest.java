/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.fieldline;

import com.codename1.backend.DataSource;
import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.fieldline.account.DemoAccounts;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Opening an account, and signing in to it, through
/// the same requests the app makes.
@BackendTest
class AccountTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private DataSource db;

    private static String registration(String email, String password, boolean ignored) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password
                + "\",\"displayName\":\"Test Person\",\"phone\":\"+1 (555) 010-9999\"}";
    }

    private String bearer(String username, String password) throws Exception {
        return "Bearer " + SignIn.token(mvc, username, password);
    }

    @Test
    void anAccountIsOpenedThenSignedInTo() throws Exception {
        mvc.perform(post("/api/account/register").contentType("application/json")
                        .content(registration("New.Tech@Example.com", "a-long-password", false)))
                .andExpect(status().isOk())
                // The name is the address in lower case, and the phone number is
                // stored in one form however it was typed.
                .andExpect(jsonPath("$.username").value("new.tech@example.com"))
                .andExpect(jsonPath("$.phone").value("+15550109999"))
                // A new account is a technician on the crew, and no more.
                .andExpect(jsonPath("$.technician").value(true))
                .andExpect(jsonPath("$.dispatcher").value(false))
                .andExpect(jsonPath("$.admin").value(false));

        // Signing in is not case sensitive about the address either.
        mvc.perform(get("/api/me").header("Host", SignIn.HOST).header("Authorization",
                        bearer("NEW.TECH@example.com", "a-long-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new.tech@example.com"))
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
                        .content(registration(DemoAccounts.TECHNICIAN, "a-long-password", false)))
                .andExpect(status().isConflict());
    }

    @Test
    void aWrongPasswordSignsNobodyIn() throws Exception {
        assertNull(SignIn.session(mvc, DemoAccounts.TECHNICIAN, "not-the-password"));
        assertNotNull(SignIn.session(mvc, DemoAccounts.TECHNICIAN, DemoAccounts.PASSWORD));
    }

    @Test
    void theApiWantsAToken() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());
        // A session is for signing in. It is not a way into the API.
        mvc.perform(get("/api/me").cookie("CN1SESSION",
                        SignIn.session(mvc, DemoAccounts.TECHNICIAN, DemoAccounts.PASSWORD)))
                .andExpect(status().isUnauthorized());
    }
}
