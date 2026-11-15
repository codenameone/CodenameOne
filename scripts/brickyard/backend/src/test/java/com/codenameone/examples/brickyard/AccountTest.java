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
package com.codenameone.examples.brickyard;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.brickyard.account.Accounts;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Opening an account, the age gate, naming yourself, becoming a creator and friends.
@BackendTest
class AccountTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private Accounts accounts;

    private static int yearsAgo(int years) {
        return Clock.yearOf(Clock.now()) - years;
    }

    @Test
    void aNewAccountIsAUserWithAPurseOfBricks() throws Exception {
        Map me = Api.json(mvc, Api.body(post("/api/account/register"),
                "{\"email\":\"Newcomer@Test.example\",\"password\":\"" + Api.PASSWORD
                        + "\",\"displayName\":\"Newcomer\",\"birthYear\":" + yearsAgo(20) + "}"));
        String who = (String) me.get("username");
        assertEquals("newcomer@test.example", who);
        assertEquals("Newcomer", me.get("displayName"));
        assertEquals((long) Accounts.START_BRICKS, Api.number(me, "bricks"));
        List roles = (List) me.get("roles");
        assertTrue(roles.contains(Roles.USER));
        assertFalse(roles.contains(Roles.CREATOR));
        assertEquals(who, Api.json(mvc, Api.as(get("/api/me"), who)).get("username"));
        assertEquals(409, Api.statusOf(mvc, Api.body(post("/api/account/register"),
                "{\"email\":\"newcomer@test.example\",\"password\":\"" + Api.PASSWORD
                        + "\",\"displayName\":\"Again\",\"birthYear\":" + yearsAgo(20) + "}")));
    }

    @Test
    void theAgeGateRefusesChildrenAndImpossibleYears() throws Exception {
        assertEquals(403, Api.statusOf(mvc, Api.body(post("/api/account/register"),
                "{\"email\":\"kid@test.example\",\"password\":\"" + Api.PASSWORD
                        + "\",\"displayName\":\"Kiddo\",\"birthYear\":" + yearsAgo(10) + "}")));
        assertEquals(400, Api.statusOf(mvc, Api.body(post("/api/account/register"),
                "{\"email\":\"old@test.example\",\"password\":\"" + Api.PASSWORD
                        + "\",\"displayName\":\"Ancient\",\"birthYear\":1700}")));
        assertFalse(accounts.exists("kid@test.example"));
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
    void becomingACreatorAddsTheRole() throws Exception {
        String who = Api.player(accounts, "builder");
        Map me = Api.json(mvc, Api.as(post("/api/me/creator"), who));
        List roles = (List) me.get("roles");
        assertTrue(roles.contains(Roles.CREATOR));
        assertTrue(roles.contains(Roles.USER));
    }

    @Test
    void friendsAreMutual() throws Exception {
        String a = Api.player(accounts, "alice");
        String b = Api.player(accounts, "bobby");
        Api.json(mvc, Api.as(Api.body(post("/api/me/friends"), "{\"username\":\"" + b + "\"}"), a));
        List ofB = Api.list(mvc, Api.as(get("/api/me/friends"), b));
        assertEquals(1, ofB.size());
        assertEquals(a, ((Map) ofB.get(0)).get("username"));
        assertEquals(404, Api.statusOf(mvc, Api.as(Api.body(post("/api/me/friends"),
                "{\"username\":\"nobody@test.example\"}"), a)));
    }

    @Test
    void theApiNeedsAToken() throws Exception {
        assertEquals(401, Api.statusOf(mvc, get("/api/me")));
        assertEquals(401, Api.statusOf(mvc, get("/api/experiences")));
    }
}
