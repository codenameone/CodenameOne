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
package com.codenameone.examples.parley;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.parley.account.Accounts;
import com.codenameone.examples.parley.account.DemoAccounts;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// What someone changes about their own account: how the app behaves for them,
/// who they are, their password, and whether they have an account at all.
@BackendTest
class SettingsTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    @Test
    void preferencesAreSavedAndReadBack() throws Exception {
        String who = People.user(accounts, "prefers");
        mvc.perform(People.asUser(get("/api/account/preferences"), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("system"))
                .andExpect(jsonPath("$.readReceipts").value(true))
                .andExpect(jsonPath("$.lastSeen").value("everyone"))
                .andExpect(jsonPath("$.disappearingSeconds").value(0));

        String wanted = "{\"language\":\"he\",\"theme\":\"dark\",\"notifyMessages\":true,"
                + "\"notifyGroups\":false,\"notifyCalls\":true,\"readReceipts\":false,"
                + "\"typingIndicators\":false,\"lastSeen\":\"contacts\",\"profilePhoto\":\"nobody\","
                + "\"disappearingSeconds\":86400,\"screenSecurity\":true,\"appLock\":true}";
        mvc.perform(People.asUser(Chat.body(put("/api/account/preferences"), wanted), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("he"));
        Map saved = Chat.json(mvc, People.asUser(get("/api/account/preferences"), who));
        assertEquals("dark", saved.get("theme"));
        assertEquals(Boolean.FALSE, saved.get("notifyGroups"));
        assertEquals(Boolean.FALSE, saved.get("readReceipts"));
        assertEquals("contacts", saved.get("lastSeen"));
        assertEquals("nobody", saved.get("profilePhoto"));
        assertEquals(86400L, Chat.number(saved, "disappearingSeconds"));
        assertEquals(Boolean.TRUE, saved.get("appLock"));
        mvc.perform(People.asUser(get("/api/me"), who))
                .andExpect(jsonPath("$.language").value("he"));

        // Each is one of the values it can take, or it is refused whole.
        String[] wrong = {"{\"language\":\"xx\"}", "{\"theme\":\"neon\"}",
            "{\"lastSeen\":\"friends\"}", "{\"disappearingSeconds\":12}"};
        for (String each : wrong) {
            assertEquals(400, Chat.statusOf(mvc, People.asUser(Chat.body(
                    put("/api/account/preferences"), each), who)), each);
        }
        mvc.perform(People.asUser(get("/api/account/preferences"), who))
                .andExpect(jsonPath("$.theme").value("dark"));
    }

    @Test
    void aProfileIsSavedAndReadBack() throws Exception {
        String who = People.user(accounts, "profiled");
        mvc.perform(People.asUser(get("/api/account/profile"), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("profiled"));
        mvc.perform(People.asUser(Chat.body(put("/api/account/profile"),
                        "{\"name\":\"Pat Profiled\"}"), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Pat Profiled"));
        mvc.perform(People.asUser(get("/api/account/profile"), who))
                .andExpect(jsonPath("$.name").value("Pat Profiled"));
        mvc.perform(People.asUser(Chat.body(put("/api/account/profile"),
                        "{\"name\":\"\"}"), who))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aPasswordIsChangedByWhoeverKnowsTheOldOne() throws Exception {
        String who = People.user(accounts, "rekeyed");
        mvc.perform(People.asUser(Chat.body(post("/api/account/password"),
                        "{\"current\":\"not-the-password\",\"replacement\":\"another-long-one\"}"),
                        who))
                .andExpect(status().isForbidden());
        mvc.perform(People.asUser(Chat.body(post("/api/account/password"),
                        "{\"current\":\"" + People.PASSWORD + "\",\"replacement\":\"short\"}"), who))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asUser(Chat.body(post("/api/account/password"),
                        "{\"current\":\"" + People.PASSWORD
                                + "\",\"replacement\":\"another-long-one\"}"), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(who));
        assertEquals(null, SignIn.session(mvc, who, People.PASSWORD));
        assertEquals(false, SignIn.session(mvc, who, "another-long-one") == null);
    }

    @Test
    void aChangedPasswordSignsTheOtherDevicesOut() throws Exception {
        String who = People.user(accounts, "two-devices");
        Map phone = SignIn.tokens(mvc, who, People.PASSWORD);
        Map tablet = SignIn.tokens(mvc, who, People.PASSWORD);

        mvc.perform(Chat.body(post("/api/account/password"),
                        "{\"current\":\"" + People.PASSWORD
                                + "\",\"replacement\":\"another-long-one\"}")
                        .header("Host", SignIn.HOST)
                        .header("Authorization", "Bearer " + phone.get("access_token")))
                .andExpect(status().isOk());

        // The tablet's refresh token asked for no password, and would have
        // gone on working for whoever held the tablet.
        mvc.perform(SignIn.formPost("/oauth2/token", "grant_type", "refresh_token",
                        "refresh_token", (String) tablet.get("refresh_token"),
                        "client_id", SecurityConfig.CLIENT_ID).header("Host", SignIn.HOST))
                .andExpect(status().isBadRequest());
        // The device the password was changed on stays signed in.
        mvc.perform(SignIn.formPost("/oauth2/token", "grant_type", "refresh_token",
                        "refresh_token", (String) phone.get("refresh_token"),
                        "client_id", SecurityConfig.CLIENT_ID).header("Host", SignIn.HOST))
                .andExpect(status().isOk());
    }

    @Test
    void anAccountIsClosedForGood() throws Exception {
        String who = People.user(accounts, "leaving");
        String friend = People.user(accounts, "left-behind");
        String token = SignIn.token(mvc, who, People.PASSWORD);
        Chat.device(mvc, who);
        String group = Chat.conversation(mvc, who, "GROUP", "Farewell", friend);
        String direct = Chat.conversation(mvc, who, "DIRECT", "", friend);

        // Not on the strength of a token: the password is asked for again.
        mvc.perform(People.asUser(post("/api/account/delete"), who))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asUser(Chat.body(post("/api/account/delete"), "{}"), who))
                .andExpect(status().isForbidden());
        mvc.perform(People.asUser(Chat.body(post("/api/account/delete"),
                        "{\"password\":\"not-the-password\"}"), who))
                .andExpect(status().isForbidden());
        mvc.perform(People.asUser(get("/api/me"), who)).andExpect(status().isOk());

        String closure = "{\"password\":\"" + People.PASSWORD + "\"}";
        mvc.perform(People.asUser(Chat.body(post("/api/account/delete"), closure), who))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(who));
        // Gone: the token, the password, and the account an admin would look up.
        mvc.perform(get("/api/me").header("Host", SignIn.HOST)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        assertEquals(null, SignIn.session(mvc, who, People.PASSWORD));
        mvc.perform(People.asAdmin(get("/api/admin/users/" + who), DemoAccounts.ADMIN))
                .andExpect(status().isNotFound());
        // The group goes on without them, handed to the one who is left.
        mvc.perform(People.asUser(get("/api/conversations/" + group), friend))
                .andExpect(jsonPath("$.myRole").value("OWNER"));
        // A direct chat with nobody on the other side is not left behind.
        mvc.perform(People.asUser(get("/api/conversations/" + direct), friend))
                .andExpect(status().isNotFound());
        // And the address is retired with it: whoever registered it next
        // would be taken for them by everyone who knew it.
        mvc.perform(post("/api/account/register").contentType("application/json")
                        .content("{\"email\":\"" + who + "\",\"password\":\"" + People.PASSWORD
                                + "\",\"displayName\":\"Somebody else\","
                                + "\"phone\":\"+15550112\"}"))
                .andExpect(status().isConflict());

        // An admin's account is not closed this way.
        mvc.perform(People.asAdmin(Chat.body(post("/api/account/delete"),
                        "{\"password\":\"" + DemoAccounts.PASSWORD + "\"}"), DemoAccounts.ADMIN))
                .andExpect(status().isConflict());
    }
}
