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
package com.codenameone.examples.dash;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.dash.account.Accounts;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Opening an account, naming yourself and building a deck.
@BackendTest
class AccountTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    @Test
    void aNewPlayerStartsWithTheFreeRunnerAndAPurse() throws Exception {
        Map me = Api.json(mvc, Api.body(post("/api/account/register"),
                "{\"email\":\"newcomer@test.example\",\"password\":\"" + Api.PASSWORD
                        + "\",\"displayName\":\"Newcomer\"}"));
        String who = (String) me.get("username");
        assertEquals("Newcomer", me.get("displayName"));
        assertEquals("runner-dash", me.get("runner"));
        assertEquals(100L, Api.number(me, "coins"));
        assertEquals(20L, Api.number(me, "gems"));
        assertEquals(Boolean.FALSE, me.get("admin"));

        Map again = Api.json(mvc, Api.as(get("/api/me"), who));
        assertEquals(who, again.get("username"));
    }

    @Test
    void aNameHasToBeReadable() throws Exception {
        String who = Api.player(accounts, "renamer");
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/me/profile"),
                "{\"displayName\":\"x\"}"), who)));
        Map me = Api.json(mvc, Api.as(Api.body(post("/api/me/profile"),
                "{\"displayName\":\"Sir Renamer\"}"), who));
        assertEquals("Sir Renamer", me.get("displayName"));
    }

    @Test
    void onlyAnOwnedRunnerCanBePicked() throws Exception {
        String who = Api.player(accounts, "picker");
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/runner"),
                "{\"runner\":\"runner-nova\"}"), who)));
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/runner"),
                "{\"runner\":\"no-such-runner\"}"), who)));
        Api.json(mvc, Api.as(Api.body(post("/api/runner"), "{\"runner\":\"runner-dash\"}"), who));
        assertEquals("runner-dash", Api.json(mvc, Api.as(get("/api/runner"), who)).get("runner"));
    }

    @Test
    void theApiNeedsAToken() throws Exception {
        assertEquals(401, Api.statusOf(mvc, get("/api/me")));
        assertEquals(401, Api.statusOf(mvc, get("/api/runner")));
    }
}
