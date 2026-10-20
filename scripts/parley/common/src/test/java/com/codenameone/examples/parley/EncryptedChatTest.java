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

import com.codename1.ui.Button;
import com.codenameone.examples.parley.net.Session;
import com.codenameone.examples.parley.ui.Nav;

import com.codenameone.examples.parley.chat.Messenger;
import com.codenameone.examples.parley.store.Message;
import com.codenameone.examples.parley.store.Store;
import com.codenameone.examples.parley.ui.Nav;
import java.util.List;
import java.util.Map;

/// Two people on one simulator, one after the other: Bob signs in so his
/// device exists and has published its keys, Alice writes to him, and Bob
/// reads it. The server is asked what it holds, and it holds no plaintext.
public class EncryptedChatTest extends E2e {
    private static final String SECRET = "the trailhead is at the north gate";

    @Override
    protected boolean run() throws Exception {
        // Bob's device registers and publishes its prekeys on sign in.
        signIn(BOB, "Chats");
        until(() -> Messenger.running(), 20000, "bob's keys");
        onEdt(Nav::signOut);
        waitForForm("Welcome");

        signIn(ALICE, "Chats");
        click("newChat");
        waitForForm("NewChat");
        type("contactSearch", "Bob");
        click("contact-" + BOB);
        waitForForm("Chat");
        say(SECRET);
        untilShown(SECRET, "alice's own message");
        // Sent: the server accepted a copy for every one of Bob's devices.
        until(() -> sentStatus() >= Message.SENT, 30000, "the message to be sent");
        shot("direct-chat-sent");

        // What the server keeps for Bob is ciphertext: nothing of the text.
        Actor bob = Actor.signIn(BOB);
        List<Object> devices = bob.list("/api/devices");
        assertTrue(!devices.isEmpty(), "bob has a device");
        for (Object device : devices) {
            String id = Actor.text((Map<String, Object>) device, "id");
            // Fetched and not acknowledged: still waiting for Bob afterwards.
            Map<String, Object> waiting = bob.get("/api/messages/" + id + "?limit=50");
            assertEqual(200, bob.status, "bob's queue");
            assertTrue(String.valueOf(waiting).indexOf("trailhead") < 0,
                    "the server holds the message in the clear");
        }
        back();
        waitForForm("Chats");
        onEdt(Nav::signOut);
        waitForForm("Welcome");

        // Bob's device decrypts it.
        signIn(BOB, "Chats");
        until(() -> has("chat-Alice Avery"), 30000, "the chat with alice");
        openChat("Alice Avery");
        untilShown(SECRET, "bob reading alice's message");
        say("see you there");
        untilShown("see you there", "bob's answer");
        shot("direct-chat-received");

        // Safety numbers exist for the session both now share.
        click("info");
        waitForForm("ConversationInfo");
        click("safety");
        waitForForm("Safety");
        until(this::hasSafety, 20000, "a safety number");
        back();
        waitForForm("ConversationInfo");
        back();
        waitForForm("Chat");
        back();
        waitForForm("Chats");
        onEdt(Nav::signOut);
        waitForForm("Welcome");
        return true;
    }

    private int sentStatus() {
        final int[] status = {-1};
        onEdt(() -> {
            for (String id : Store.ids(false)) {
                List<Message> all = Store.messages(id);
                for (Message each : all) {
                    if (SECRET.equals(each.text)) {
                        status[0] = each.status;
                    }
                }
            }
        });
        return status[0];
    }

    private boolean hasSafety() {
        final boolean[] found = new boolean[1];
        onEdt(() -> found[0] = any(com.codename1.ui.Display.getInstance().getCurrent()));
        return found[0];
    }

    private static boolean any(com.codename1.ui.Container in) {
        for (int iter = 0; in != null && iter < in.getComponentCount(); iter++) {
            com.codename1.ui.Component each = in.getComponentAt(iter);
            String name = each.getName();
            if (name != null && name.startsWith("safety-")) {
                return true;
            }
            if (each instanceof com.codename1.ui.Container
                    && any((com.codename1.ui.Container) each)) {
                return true;
            }
        }
        return false;
    }
}
