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
import com.codenameone.examples.parley.chat.Conversations;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.delete;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Groups and channels: who may change them, invite links, and what happens
/// when the owner leaves.
@BackendTest
class GroupTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    @Test
    void onlyAdminsChangeAGroup() throws Exception {
        String owner = People.user(accounts, "g-owner");
        String member = People.user(accounts, "g-member");
        String other = People.user(accounts, "g-other");
        String group = Chat.conversation(mvc, owner, Conversations.GROUP, "Climbers", member);
        Map seen = Chat.json(mvc, People.asUser(get("/api/conversations/" + group), member));
        assertEquals("MEMBER", seen.get("myRole"));
        assertEquals(2, ((List) seen.get("members")).size());

        assertEquals(403, Chat.statusOf(mvc, People.asUser(Chat.body(
                post("/api/conversations/" + group + "/members"),
                Chat.map("username", other, "role", "MEMBER")), member)));
        Chat.json(mvc, People.asUser(Chat.body(put("/api/conversations/" + group + "/members/"
                + member), Chat.map("username", member, "role", "ADMIN")), owner));
        Chat.json(mvc, People.asUser(Chat.body(post("/api/conversations/" + group + "/members"),
                Chat.map("username", other, "role", "MEMBER")), member));
        assertEquals(3, ((List) Chat.json(mvc, People.asUser(get("/api/conversations/" + group),
                owner)).get("members")).size());
        // The owner stays the owner.
        assertEquals(409, Chat.statusOf(mvc, People.asUser(
                delete("/api/conversations/" + group + "/members/" + owner), member)));
        // A timer has to be one the app offers.
        assertEquals(400, Chat.statusOf(mvc, People.asUser(Chat.body(
                put("/api/conversations/" + group), Chat.map("title", "Climbers", "about", "",
                        "disappearingSeconds", 17, "adminsOnly", false)), owner)));
        Map edited = Chat.json(mvc, People.asUser(Chat.body(put("/api/conversations/" + group),
                Chat.map("title", "Climbers", "about", "Tuesdays", "disappearingSeconds", 3600,
                        "adminsOnly", false)), owner));
        assertEquals(3600L, Chat.number(edited, "disappearingSeconds"));
    }

    @Test
    void anInviteLinkLetsSomeoneJoin() throws Exception {
        String owner = People.user(accounts, "i-owner");
        String guest = People.user(accounts, "i-guest");
        String group = Chat.conversation(mvc, owner, Conversations.GROUP, "Book club");
        Map invite = Chat.json(mvc, People.asUser(post("/api/conversations/" + group + "/invite"),
                owner));
        String code = (String) invite.get("code");
        Map preview = Chat.json(mvc, People.asUser(get("/api/invites/" + code), guest));
        assertEquals("Book club", preview.get("title"));
        Map joined = Chat.json(mvc, People.asUser(post("/api/invites/" + code + "/join"), guest));
        assertEquals(group, joined.get("id"));
        assertEquals(404, Chat.statusOf(mvc, People.asUser(get("/api/invites/not-a-code"), guest)));
    }

    @Test
    void anOwnerWhoLeavesHandsTheGroupOn() throws Exception {
        String owner = People.user(accounts, "l-owner");
        String member = People.user(accounts, "l-member");
        String group = Chat.conversation(mvc, owner, Conversations.GROUP, "Choir", member);
        Chat.statusOf(mvc, People.asUser(post("/api/conversations/" + group + "/leave"), owner));
        assertEquals(404, Chat.statusOf(mvc, People.asUser(get("/api/conversations/" + group),
                owner)));
        assertEquals("OWNER", Chat.json(mvc, People.asUser(get("/api/conversations/" + group),
                member)).get("myRole"));
    }

    @Test
    void onlyAdminsPostInAChannel() throws Exception {
        String owner = People.user(accounts, "c-owner");
        String reader = People.user(accounts, "c-reader");
        String readers = Chat.device(mvc, reader);
        String channel = Chat.conversation(mvc, owner, Conversations.CHANNEL, "News", reader);
        assertTrue(Boolean.TRUE.equals(Chat.json(mvc, People.asUser(
                get("/api/conversations/" + channel), reader)).get("adminsOnly")));
        assertEquals(403, Chat.statusOf(mvc, People.asUser(Chat.body(post("/api/messages"),
                Chat.map("conversationId", channel, "senderDevice", readers, "clientId", "n-1",
                        "kind", "MESSAGE", "copies", Chat.copies(), "plaintext", "")), reader)));
    }
}
