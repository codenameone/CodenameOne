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
import com.codenameone.examples.parley.push.PushDevices;
import com.codenameone.examples.parley.push.PushSender;
import com.codenameone.examples.parley.push.RecordingPushSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The pushes a message and a call send. A push never carries what was
/// said: it wakes the app, which fetches the message itself.
@BackendTest
class PushTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private PushSender sender;

    private RecordingPushSender sent;

    @BeforeEach
    void recording() {
        assertTrue(sender instanceof RecordingPushSender,
                "the test profile records notifications: " + sender);
        sent = (RecordingPushSender) sender;
    }

    private void register(String username, String token, String platform) throws Exception {
        Chat.json(mvc, People.asUser(Chat.body(post("/api/account/push"),
                Chat.map("provider", "fcm", "token", token, "platform", platform)), username));
    }

    @Test
    void anOfflineRecipientIsWokenWithNothingInThePush() throws Exception {
        String alice = People.user(accounts, "push-a");
        String bob = People.user(accounts, "push-b");
        String alices = Chat.device(mvc, alice);
        String bobs = Chat.device(mvc, bob);
        register(bob, "push-b-phone", "and");
        String chat = Chat.conversation(mvc, alice, Conversations.DIRECT, "", bob);
        Chat.send(mvc, alice, alices, chat, "p-1", Chat.copies(Chat.copy(bob, bobs)));
        List<RecordingPushSender.Sent> pushes = sent.sentTo("push-b-phone");
        assertFalse(pushes.isEmpty());
        Map message = pushes.get(pushes.size() - 1).message();
        assertEquals("sync", ((Map) message.get("data")).get("type"));
        assertFalse(String.valueOf(message).contains(Chat.sealed(bobs)), String.valueOf(message));
    }

    @Test
    void turningMessageNotificationsOffStopsTheWake() throws Exception {
        String alice = People.user(accounts, "quiet-a");
        String bob = People.user(accounts, "quiet-b");
        String alices = Chat.device(mvc, alice);
        String bobs = Chat.device(mvc, bob);
        register(bob, "quiet-b-phone", "and");
        Map prefs = Chat.json(mvc, People.asUser(com.codename1.backend.test.MockMvcRequestBuilders
                .get("/api/account/preferences"), bob));
        prefs.put("notifyMessages", Boolean.FALSE);
        Chat.json(mvc, People.asUser(Chat.body(put("/api/account/preferences"), prefs), bob));
        String chat = Chat.conversation(mvc, alice, Conversations.DIRECT, "", bob);
        Chat.send(mvc, alice, alices, chat, "q-1", Chat.copies(Chat.copy(bob, bobs)));
        assertTrue(sent.sentTo("quiet-b-phone").isEmpty());
    }

    @Test
    void aCallRingsTheVoipTokenAndNotTheOthers() throws Exception {
        String alice = People.user(accounts, "ring-a");
        String bob = People.user(accounts, "ring-b");
        register(bob, "ring-b-voip", PushDevices.VOIP);
        register(bob, "ring-b-phone", "and");
        String chat = Chat.conversation(mvc, alice, Conversations.DIRECT, "", bob);
        Chat.json(mvc, People.asUser(Chat.body(post("/api/calls"),
                Chat.map("conversationId", chat, "video", false)), alice));
        List<RecordingPushSender.Sent> voip = sent.sentTo("ring-b-voip");
        assertEquals(1, voip.size());
        Map call = (Map) ((Map) voip.get(0).message().get("data")).get("cn1call");
        assertEquals("ring-a", String.valueOf(call.get("displayName")));
        List<RecordingPushSender.Sent> phone = sent.sentTo("ring-b-phone");
        assertEquals(1, phone.size());
        assertEquals("call", ((Map) phone.get(0).message().get("data")).get("type"));
        assertNull(((Map) phone.get(0).message().get("data")).get("cn1call"));
    }
}
