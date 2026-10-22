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
package com.codenameone.examples.skirmish;

import com.codenameone.examples.skirmish.net.Session;
import com.codenameone.examples.skirmish.rules.Deck;

/// Registering with a styled battle name, building a deck, the settings and
/// signing out.
public class AccountTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        shot("signin");
        click("registerButton");
        waitForForm("register");
        String stamp = String.valueOf(System.currentTimeMillis() % 100000);
        type("displayName", "Ironclad");
        type("email", "new" + stamp + "@skirmish.example");
        type("password", "a-long-password");
        shot("register-name");
        click("createButton");
        waitForForm("home");
        // A new player is taken straight into the training battle.
        waitForForm("battle");
        until(() -> has("coachSkip"), 20000, "the mentor");
        shot("tutorial-first-visit");
        click("coachSkip");
        waitForForm("home");
        assertEqual("Ironclad", Session.user().displayName());
        assertEqual(Deck.SIZE, Session.user().deck().size(), "a new player starts with a full deck");
        shot("home");

        // Swapping a card out and back keeps the deck valid and saves it.
        click("deckButton");
        waitForForm("deck");
        final String first = Session.user().deck().get(0);
        click("deck-" + first);
        assertFalse(((com.codename1.ui.Button) findByName("saveDeck")).isEnabled(),
                "a deck of seven cards cannot be saved");
        click("deck-" + first);
        shot("deck");
        click("saveDeck");
        waitFor(1500);

        onEdt(() -> com.codenameone.examples.skirmish.ui.SettingsForm.show());
        waitForForm("settings");
        shot("settings");
        click("signOut");
        waitForForm("signin");
        assertNull(Session.user());
        return true;
    }
}
