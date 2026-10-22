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

import com.codenameone.examples.parley.ui.Nav;

/// A group made from the app: members picked, a message to all of them, the
/// disappearing timer set, and the info screen with its members.
public class GroupTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        // Carol's device must exist for a copy to be made for it.
        signIn(CAROL, "Chats");
        onEdt(Nav::signOut);
        waitForForm("Welcome");

        signIn(ALICE, "Chats");
        onEdt(() -> com.codenameone.examples.parley.ui.NewChatForm.group(
                com.codename1.ui.Display.getInstance().getCurrent()));
        waitForForm("NewGroup");
        type("groupName", "Book club");
        type("contactSearch", "Carol");
        click("pick-" + CAROL);
        until(() -> findByName("chosen") != null, 20000, "the chosen members");
        shot("new-group");
        click("createGroup");
        waitForForm("Chat");
        say("first book: Dune");
        untilShown("first book: Dune", "the group message");
        click("info");
        waitForForm("ConversationInfo");
        until(() -> has("timer-3600"), 20000, "the timer choice");
        click("timer-3600");
        shot("group-info");
        back();
        waitForForm("Chat");
        back();
        waitForForm("Chats");
        onEdt(Nav::signOut);
        waitForForm("Welcome");

        signIn(CAROL, "Chats");
        openChat("Book club");
        untilShown("first book: Dune", "carol reading the group message");
        back();
        waitForForm("Chats");
        onEdt(Nav::signOut);
        waitForForm("Welcome");
        return true;
    }
}
