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

/// The chat with the demo bot: plain text, said so, and answered. Then what
/// can be done to a message once it is sent: react, edit, delete.
public class BotChatTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        signIn(ALICE, "Chats");
        openChat("Parley Bot");
        until(() -> findByName("notEncrypted") != null, 20000, "the not-encrypted banner");
        say("hello");
        untilShown("hello", "the message sent");
        untilShown("Hi Alice! I am the Parley demo bot. Ask me for \"help\".", "the bot's answer");
        shot("bot-chat");
        say("an echo");
        untilShown("an echo", "the echo");
        back();
        waitForForm("Chats");
        onEdt(() -> com.codenameone.examples.parley.ui.Nav.signOut());
        waitForForm("Welcome");
        return true;
    }
}
