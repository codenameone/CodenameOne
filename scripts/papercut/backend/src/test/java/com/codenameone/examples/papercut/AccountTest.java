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
package com.codenameone.examples.papercut;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.papercut.account.Accounts;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Registration, the signed-in player and the profile.
@BackendTest
class AccountTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    @Test
    void aNewPlayerStartsWithTheFreePaperAndCanRenameThemselves() throws Exception {
        String who = Api.player(accounts, "newcomer");
        Map me = Api.json(mvc, Api.as(get("/api/me"), who));
        assertEquals("newcomer", me.get("displayName"));
        assertEquals((long) Accounts.START_COINS, Api.number(me, "inkCoins"));
        assertEquals("quill-white", me.get("quill"));
        assertFalse((Boolean) me.get("admin"));
        Map renamed = Api.json(mvc, Api.as(Api.body(post("/api/me/profile"),
                "{\"displayName\":\"Fresh Sheet\"}"), who));
        assertEquals("Fresh Sheet", renamed.get("displayName"));
        // A paper nobody bought cannot be equipped through the profile either.
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/me/profile"),
                "{\"quill\":\"quill-gold-foil\"}"), who)));
    }

    @Test
    void anAddressRegistersOnceAndBadInputIsRefused() throws Exception {
        String body = "{\"email\":\"Twice@Test.example\",\"password\":\"long-enough\",\"displayName\":\"Twice\"}";
        Map first = Api.json(mvc, Api.body(post("/api/account/register"), body));
        assertEquals("twice@test.example", first.get("username"));
        assertEquals(409, Api.statusOf(mvc, Api.body(post("/api/account/register"), body)));
        assertEquals(400, Api.statusOf(mvc, Api.body(post("/api/account/register"),
                "{\"email\":\"nope\",\"password\":\"long-enough\",\"displayName\":\"Nope\"}")));
        assertEquals(400, Api.statusOf(mvc, Api.body(post("/api/account/register"),
                "{\"email\":\"short@test.example\",\"password\":\"x\",\"displayName\":\"Short\"}")));
    }

    @Test
    void theDemoFieldIsSeeded() throws Exception {
        assertTrue(accounts.exists("player@papercut.example"));
        assertTrue(accounts.isAdmin("admin@papercut.example"));
        Map trouble = Api.json(mvc, Api.as(get("/api/me"), "trouble@papercut.example"));
        assertTrue((Boolean) trouble.get("flagged"));
        List board = Api.list(mvc, Api.as(get("/api/leaderboard?mode=FOLD&period=TODAY"),
                "player@papercut.example"));
        assertFalse(board.isEmpty());
    }
}
