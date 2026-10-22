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
package com.codenameone.examples.parley.chat;

import com.codename1.backend.Config;
import com.codename1.backend.Tasks;
import com.codename1.backend.annotations.Component;

/// The demo bot, which answers whatever is written to it, so that one
/// person trying the app alone has someone to talk to.
///
/// It is the server, so it cannot take part in end-to-end encryption, and
/// its chats are plain text: the app marks them so. It runs only on a
/// development profile.
@Component
public class DemoBot {
    private final Messages messages;
    private final Conversations conversations;

    public DemoBot(Messages messages, Conversations conversations, Relays relays,
            Config config) {
        this.messages = messages;
        this.conversations = conversations;
        if (config.isDevelopmentProfile()) {
            relays.register(this::posted);
        }
    }

    private void posted(String conversationId, String sender, String senderName, String text) {
        if (Conversations.BOT.equals(sender)
                || !"BOT".equals(conversations.bridge(conversationId))) {
            return;
        }
        String reply = reply(senderName, text);
        // Answered after the sender's own transaction committed, and a moment
        // later, as a person would.
        Tasks.platform(() -> {
            try {
                Thread.sleep(800);
                messages.post(conversationId, Conversations.BOT, reply);
            } catch (Exception failed) {
                System.err.println("[bot] could not reply: " + failed);
            }
        });
    }

    /// What the bot says back. Deliberately simple: a few words it knows,
    /// and an echo for the rest.
    static String reply(String name, String text) {
        String said = text.trim();
        if (said.equalsIgnoreCase("hi") || said.equalsIgnoreCase("hello")
                || said.equalsIgnoreCase("hey")) {
            return "Hi " + first(name) + "! I am the Parley demo bot. Ask me for \"help\".";
        }
        if (said.equalsIgnoreCase("help")) {
            return "Try a group: start one from the chats list and add a demo account,"
                    + " alice, bob, carol or dan. Or call one of them. Anything else you"
                    + " write to me, I repeat.";
        }
        if (said.equalsIgnoreCase("privacy")) {
            return "This chat is not end-to-end encrypted: I am the server, so I can read it."
                    + " Your chats with people are, and the server cannot read those.";
        }
        return "You said: " + (said.length() > 200 ? said.substring(0, 200) + "..." : said);
    }

    private static String first(String name) {
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name;
    }
}
