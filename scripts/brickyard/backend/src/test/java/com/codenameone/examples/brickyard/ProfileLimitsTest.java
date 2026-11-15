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

import com.codename1.appkit.server.push.PushSender;
import com.codename1.appkit.server.push.RecordingPushSender;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.brickyard.account.Accounts;
import com.codenameone.examples.brickyard.account.Limits;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Server-side profile and parental controls, push devices and the pushes
/// they receive, a creator's own experiences and an experience's stats.
@BackendTest
class ProfileLimitsTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private PushSender pushes;
    @Autowired
    private Limits limits;

    @Test
    void avatarColoursAreKeptOnTheServer() throws Exception {
        String u = Api.player(accounts, "avatarer");
        Api.json(mvc, Api.as(Api.body(post("/api/me/profile"), "{\"avatar\":\"#ff0000,#00ff00\"}"), u));
        Map p = Api.json(mvc, Api.as(get("/api/me/profile"), u));
        assertEquals("#ff0000,#00ff00", p.get("avatar"));
        assertEquals("avatarer", p.get("displayName"), "an avatar-only save keeps the name");
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/me/profile"),
                "{\"avatar\":\"red; drop\"}"), u)));
    }

    @Test
    void theChosenBodyIsWhatThePlayServersDress() throws Exception {
        String u = Api.player(accounts, "bodied");
        assertEquals(null, limits.avatarBody(u), "no choice yet: the default figure");
        Api.json(mvc, Api.as(Api.body(post("/api/me/profile"), "{\"avatar\":\"ff0000,00ff00;mage\"}"), u));
        assertEquals("mage", limits.avatarBody(u));
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/me/profile"),
                "{\"avatar\":\"ff0000;dragon\"}"), u)), "only bodies the client can draw");
        assertEquals("mage", limits.avatarBody(u), "a refused save keeps the last body");
    }

    @Test
    void theSpendingLimitAndChatSwitchAreEnforced() throws Exception {
        String admin = Api.player(accounts, "limadmin");
        String u = Api.player(accounts, "limited");
        Map l = Api.json(mvc, Api.as(get("/api/me/limits"), u));
        assertEquals(-1L, Api.number(l, "spendLimit"));
        assertEquals(Boolean.TRUE, l.get("chatEnabled"));
        Api.json(mvc, Api.as(Api.body(post("/api/me/limits"),
                "{\"spendLimit\":5,\"spent\":0,\"chatEnabled\":false}"), u));
        Map hat = Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/catalog"),
                "{\"name\":\"Cap\",\"slot\":\"HEAD\",\"price\":10}"), admin, Roles.ADMIN));
        assertEquals(402, Api.statusOf(mvc, Api.as(post("/api/catalog/" + hat.get("id") + "/buy"), u)),
                "a purchase past the limit is refused by the server");
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/chat/lobby"), "{\"body\":\"hi\"}"), u)),
                "chat off is enforced by the server");
        Api.json(mvc, Api.as(Api.body(post("/api/me/limits"),
                "{\"spendLimit\":100,\"spent\":0,\"chatEnabled\":true}"), u));
        Api.json(mvc, Api.as(post("/api/catalog/" + hat.get("id") + "/buy"), u));
        assertEquals(10L, Api.number(Api.json(mvc, Api.as(get("/api/me/limits"), u)), "spent"));
        Api.json(mvc, Api.as(Api.body(post("/api/chat/lobby"), "{\"body\":\"hi\"}"), u));
    }

    @Test
    void aFriendRequestIsPushedToTheRegisteredDevice() throws Exception {
        String a = Api.player(accounts, "pusha");
        String b = Api.player(accounts, "pushb");
        Api.json(mvc, Api.as(Api.body(post("/api/me/devices"),
                "{\"provider\":\"fcm\",\"token\":\"tok-pushb\",\"platform\":\"android\"}"), b));
        Api.json(mvc, Api.as(Api.body(post("/api/me/friends"), "{\"username\":\"" + b + "\"}"), a));
        assertTrue(pushes instanceof RecordingPushSender);
        assertEquals(1, ((RecordingPushSender) pushes).sentTo("tok-pushb").size());
    }

    @Test
    void creatorsListTheirDraftsAndPlayersPickAStat() throws Exception {
        String owner = Api.player(accounts, "draftowner");
        String id = Api.experience(mvc, owner, "PUBLIC", "ALL");
        List mine = Api.list(mvc, Api.as(get("/api/me/experiences"), owner));
        assertEquals(1, mine.size(), "an unpublished draft is listed for its creator");
        assertEquals(id, ((Map) mine.get(0)).get("id"));
        Api.publishSample(mvc, owner, id);
        Api.json(mvc, Api.asEngine(Api.body(post("/api/leaderboards/" + id + "/scores"),
                "{\"stat\":\"Coins\",\"userId\":\"" + owner + "\",\"delta\":3}")));
        Api.json(mvc, Api.asEngine(Api.body(post("/api/leaderboards/" + id + "/scores"),
                "{\"stat\":\"Wins\",\"userId\":\"" + owner + "\",\"delta\":1}")));
        List stats = Api.list(mvc, Api.as(get("/api/experiences/" + id + "/stats"), owner));
        assertEquals("[Coins, Wins]", stats.toString());
        assertFalse(stats.contains("score"));
    }
}
