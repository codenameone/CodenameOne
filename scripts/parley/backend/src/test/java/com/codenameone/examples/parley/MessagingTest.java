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
import com.codename1.backend.Base64;
import com.codename1.backend.DataSource;
import com.codenameone.examples.parley.chat.Conversations;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Devices, their keys, and messages from one device to others: the server
/// routes ciphertext it cannot read, to exactly the devices it was meant for.
@BackendTest
class MessagingTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private DataSource db;

    @Test
    void aDeviceMustSignItsPreKeyWithItsIdentityKey() throws Exception {
        String who = People.user(accounts, "signer");
        // The signature is over a different key from the one offered.
        assertEquals(400, Chat.statusOf(mvc, People.asUser(Chat.body(post("/api/devices"),
                Chat.registration(Chat.PRE[0], Chat.PRE[1])), who)));
        String device = Chat.device(mvc, who);
        List devices = Chat.list(mvc, People.asUser(get("/api/devices"), who));
        assertEquals(1, devices.size());
        assertEquals(device, ((Map) devices.get(0)).get("id"));
    }

    @Test
    void eachOneTimePreKeyIsHandedOutOnce() throws Exception {
        String asker = People.user(accounts, "asker");
        String owner = People.user(accounts, "keyholder");
        String device = Chat.device(mvc, owner);
        Map first = Chat.json(mvc, People.asUser(get("/api/keys/" + owner + "/" + device), asker));
        Map second = Chat.json(mvc, People.asUser(get("/api/keys/" + owner + "/" + device), asker));
        Map third = Chat.json(mvc, People.asUser(get("/api/keys/" + owner + "/" + device), asker));
        assertEquals(1L, Chat.number((Map) first.get("oneTimePreKey"), "id"));
        assertEquals(2L, Chat.number((Map) second.get("oneTimePreKey"), "id"));
        // Out of one-time keys, the signed prekey alone still starts a session.
        assertNull(third.get("oneTimePreKey"));
        assertEquals(Chat.PRE[0], ((Map) third.get("signedPreKey")).get("publicKey"));
        assertEquals(Chat.ID_PUBLIC, third.get("identityKey"));
        assertEquals(0L, Chat.number(Chat.json(mvc, People.asUser(
                get("/api/devices/" + device + "/prekeys/count"), owner)), "remaining"));
    }

    @Test
    void aMessageGoesToEveryOtherDeviceAndNoneOther() throws Exception {
        String alice = People.user(accounts, "router-a");
        String bob = People.user(accounts, "router-b");
        String phone = Chat.device(mvc, alice);
        String laptop = Chat.device(mvc, alice);
        String bobs = Chat.device(mvc, bob);
        String chat = Chat.conversation(mvc, alice, Conversations.DIRECT, "", bob);

        // Alice's laptop is left out: nothing is stored, and she is told.
        Map missing = Chat.send(mvc, alice, phone, chat, "m-1",
                Chat.copies(Chat.copy(bob, bobs)));
        assertEquals("", missing.get("messageId"));
        assertEquals(laptop, ((Map) ((List) missing.get("missing")).get(0)).get("deviceId"));
        assertTrue(Chat.envelopes(mvc, bob, bobs).isEmpty());

        Map sent = Chat.send(mvc, alice, phone, chat, "m-1",
                Chat.copies(Chat.copy(bob, bobs), Chat.copy(alice, laptop)));
        String messageId = (String) sent.get("messageId");
        assertTrue(messageId.length() > 0);
        // A retry of the same message is the same message.
        assertEquals(messageId, Chat.send(mvc, alice, phone, chat, "m-1",
                Chat.copies(Chat.copy(bob, bobs), Chat.copy(alice, laptop))).get("messageId"));

        List waiting = Chat.envelopes(mvc, bob, bobs);
        assertEquals(1, waiting.size());
        Map envelope = (Map) waiting.get(0);
        assertEquals(alice, envelope.get("sender"));
        assertEquals(phone, envelope.get("senderDevice"));
        assertEquals(Chat.sealed(bobs), envelope.get("ciphertext"));
        assertEquals("h:" + bobs, envelope.get("header"));
        // What is stored is the bytes the sender sent and nothing else.
        Map stored = db.queryOne("SELECT payload FROM envelope WHERE id = ?",
                new Object[] {envelope.get("id")});
        assertArrayEquals(Base64.decode(Chat.sealed(bobs)), (byte[]) stored.get("payload"));
        assertEquals(1, Chat.envelopes(mvc, alice, laptop).size());
        assertTrue(Chat.envelopes(mvc, alice, phone).isEmpty());

        Chat.json(mvc, People.asUser(Chat.body(post("/api/messages/" + bobs + "/ack"),
                Chat.map("ids", Arrays.asList(envelope.get("id")))), bob));
        assertTrue(Chat.envelopes(mvc, bob, bobs).isEmpty());
        // A device reads only its own queue.
        assertEquals(404, Chat.statusOf(mvc, People.asUser(get("/api/messages/" + laptop + "?limit=100"), bob)));
    }

    @Test
    void aStrangerCannotSeeOrWriteToAConversation() throws Exception {
        String alice = People.user(accounts, "private-a");
        String bob = People.user(accounts, "private-b");
        String eve = People.user(accounts, "private-e");
        String eves = Chat.device(mvc, eve);
        String chat = Chat.conversation(mvc, alice, Conversations.DIRECT, "", bob);
        assertEquals(404, Chat.statusOf(mvc, People.asUser(get("/api/conversations/" + chat), eve)));
        assertEquals(404, Chat.statusOf(mvc, People.asUser(Chat.body(post("/api/messages"),
                Chat.map("conversationId", chat, "senderDevice", eves, "clientId", "x",
                        "kind", "MESSAGE", "copies", Chat.copies(), "plaintext", "")), eve)));
        // And a direct chat is made once: asking again answers the same one.
        assertEquals(chat, Chat.conversation(mvc, bob, Conversations.DIRECT, "", alice));
    }

    @Test
    void aBlockedSendersMessagesAreDroppedWithoutSayingSo() throws Exception {
        String alice = People.user(accounts, "blocked-a");
        String bob = People.user(accounts, "blocker-b");
        String alices = Chat.device(mvc, alice);
        String bobs = Chat.device(mvc, bob);
        String chat = Chat.conversation(mvc, alice, Conversations.DIRECT, "", bob);
        Chat.json(mvc, People.asUser(put("/api/blocks/" + alice), bob));
        Map sent = Chat.send(mvc, alice, alices, chat, "b-1", Chat.copies(Chat.copy(bob, bobs)));
        assertTrue(((String) sent.get("messageId")).length() > 0);
        assertTrue(Chat.envelopes(mvc, bob, bobs).isEmpty());
    }

    @Test
    void theDemoBotAnswersInPlainText() throws Exception {
        String alice = People.user(accounts, "bot-friend");
        String alices = Chat.device(mvc, alice);
        String chat = Chat.conversation(mvc, alice, Conversations.DIRECT, "", Conversations.BOT);
        assertEquals("BOT", Chat.json(mvc, People.asUser(get("/api/conversations/" + chat),
                alice)).get("bridge"));
        Chat.say(mvc, alice, alices, chat, "hi-1", "hello");
        Map reply = Chat.await(mvc, alice, alices, Conversations.BOT);
        assertNotNull(reply, "the bot did not answer");
        assertTrue(Chat.text(reply).startsWith("Hi "), Chat.text(reply));
    }
}
