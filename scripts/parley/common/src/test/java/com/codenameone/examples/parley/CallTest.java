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

import java.util.List;
import java.util.Map;

/// A voice call from the app, answered by the other side through the
/// signalling relay: ringing, connected through the simulator's WebRTC,
/// muted, hung up, and the server's record of it.
public class CallTest extends E2e {
    private static final String ANSWER_SDP =
            "v=0\\r\\no=- 1 2 IN IP4 127.0.0.1\\r\\ns=-\\r\\nt=0 0\\r\\n"
            + "m=audio 9 UDP/TLS/RTP/SAVPF 111\\r\\na=mid:0\\r\\na=sendrecv\\r\\n";

    @Override
    protected boolean run() throws Exception {
        signIn(ALICE, "Chats");
        click("newChat");
        waitForForm("NewChat");
        type("contactSearch", "Bob");
        click("contact-" + BOB);
        waitForForm("Chat");
        click("voiceCall");
        waitForForm("Call");
        untilText("callState", "Calling...");
        shot("call-outgoing");

        final Actor bob = Actor.signIn(BOB);
        final String[] id = new String[1];
        until(() -> (id[0] = ringing(bob)) != null, 20000, "the call ringing for bob");
        signal(bob, id[0], "ACCEPT", "");
        signal(bob, id[0], "ANSWER", ANSWER_SDP);
        untilText("callState", "End-to-end encrypted");
        shot("call-connected");

        click("mute");
        until(() -> has("mute"), 20000, "the mute button again");
        click("hangUp");
        waitForForm("Chat");
        until(() -> "ENDED".equals(state(bob, id[0])), 20000, "the call ended on the server");
        back();
        waitForForm("Chats");
        return true;
    }

    private void signal(Actor actor, String call, String type, String payload) {
        actor.post("/api/calls/" + call + "/signal", "{\"callId\":\"" + call + "\",\"type\":\""
                + type + "\",\"payload\":\"" + payload + "\",\"fromDevice\":\"e2e-bob\"}");
        assertEqual(200, actor.status, "the " + type + " signal");
    }

    private static String ringing(Actor bob) {
        for (Object each : bob.list("/api/calls")) {
            if (each instanceof Map && "RINGING".equals(Actor.text(asMap(each), "state"))) {
                return Actor.text(asMap(each), "id");
            }
        }
        return null;
    }

    private static String state(Actor bob, String call) {
        List<Object> calls = bob.list("/api/calls");
        for (Object each : calls) {
            if (each instanceof Map && call.equals(Actor.text(asMap(each), "id"))) {
                return Actor.text(asMap(each), "state");
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object each) {
        return (Map<String, Object>) each;
    }
}
