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

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The call state machine and the signalling relay.
@BackendTest
class CallTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    private Map start(String caller, String chat, boolean video) throws Exception {
        return Chat.json(mvc, People.asUser(Chat.body(post("/api/calls"),
                Chat.map("conversationId", chat, "video", video)), caller));
    }

    private Map signal(String who, String call, String type, String device) throws Exception {
        return Chat.json(mvc, People.asUser(Chat.body(post("/api/calls/" + call + "/signal"),
                Chat.map("callId", call, "type", type, "payload", "v=0", "fromDevice", device)),
                who));
    }

    @Test
    void aCallRingsIsAnsweredAndEnds() throws Exception {
        String alice = People.user(accounts, "call-a");
        String bob = People.user(accounts, "call-b");
        String alices = Chat.device(mvc, alice);
        String bobs = Chat.device(mvc, bob);
        String chat = Chat.conversation(mvc, alice, Conversations.DIRECT, "", bob);
        Map call = start(alice, chat, true);
        String id = (String) call.get("id");
        assertEquals("RINGING", call.get("state"));
        assertEquals(bob, call.get("callee"));
        signal(alice, id, "OFFER", alices);
        assertEquals("ACCEPTED", signal(bob, id, "ACCEPT", bobs).get("state"));
        signal(bob, id, "ANSWER", bobs);
        assertEquals("ENDED", signal(alice, id, "END", alices).get("state"));
        List history = Chat.list(mvc, People.asUser(get("/api/calls"), bob));
        assertEquals(id, ((Map) history.get(0)).get("id"));
        // Somebody else's call is not there to signal.
        String eve = People.user(accounts, "call-e");
        assertEquals(404, Chat.statusOf(mvc, People.asUser(Chat.body(
                post("/api/calls/" + id + "/signal"), Chat.map("callId", id, "type", "END",
                        "payload", "", "fromDevice", "")), eve)));
    }

    @Test
    void aCallerWhoHangsUpFirstLeavesAMissedCall() throws Exception {
        String alice = People.user(accounts, "miss-a");
        String bob = People.user(accounts, "miss-b");
        String chat = Chat.conversation(mvc, alice, Conversations.DIRECT, "", bob);
        String id = (String) start(alice, chat, false).get("id");
        assertEquals("MISSED", signal(alice, id, "END", "").get("state"));
    }

    @Test
    void someoneAlreadyInACallIsBusy() throws Exception {
        String alice = People.user(accounts, "busy-a");
        String bob = People.user(accounts, "busy-b");
        String carol = People.user(accounts, "busy-c");
        start(alice, Chat.conversation(mvc, alice, Conversations.DIRECT, "", bob), false);
        Map second = start(carol, Chat.conversation(mvc, carol, Conversations.DIRECT, "", bob),
                false);
        assertEquals("BUSY", second.get("state"));
        assertTrue(Chat.number(second, "endedAt") > 0);
    }

    @Test
    void groupsAreNotCalled() throws Exception {
        String alice = People.user(accounts, "gcall-a");
        String bob = People.user(accounts, "gcall-b");
        String group = Chat.conversation(mvc, alice, Conversations.GROUP, "Band", bob);
        assertEquals(400, Chat.statusOf(mvc, People.asUser(Chat.body(post("/api/calls"),
                Chat.map("conversationId", group, "video", false)), alice)));
    }

    @Test
    void theIceServersComeFromTheConfiguration() throws Exception {
        String alice = People.user(accounts, "ice-a");
        Map ice = Chat.json(mvc, People.asUser(get("/api/calls/ice"), alice));
        // The test profile offers none, so nothing reaches outside the machine.
        assertTrue(((List) ice.get("servers")).isEmpty());
    }
}
