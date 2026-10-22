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

import com.codename1.components.Switch;
import com.codename1.ui.Button;
import com.codenameone.examples.parley.net.Session;
import com.codenameone.examples.parley.ui.Nav;

import com.codenameone.examples.parley.ui.SettingsForm;
import com.codename1.ui.Display;
import java.util.Map;

/// The settings that live with the account: language, theme, and the privacy
/// switches the other side of a chat sees the effect of.
public class SettingsTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        final Actor carol = Actor.signIn(CAROL);
        signIn(CAROL, "Chats");
        onEdt(() -> SettingsForm.show(Display.getInstance().getCurrent()));
        waitForForm("Settings");
        untilText("signOut", "Sign out");
        shot("settings");

        click("language-he");
        until(() -> text("signOut").length() > 0 && !"Sign out".equals(text("signOut")), 20000,
                "the screen to be in Hebrew");
        until(() -> "he".equals(Actor.text(carol.get("/api/account/preferences"), "language")),
                20000, "the language to be kept with the account");
        shot("settings-hebrew");
        click("language-en");
        untilText("signOut", "Sign out");

        click("theme-dark");
        until(() -> "dark".equals(Actor.text(carol.get("/api/account/preferences"), "theme")),
                20000, "the theme to be kept with the account");
        shot("settings-dark");
        click("theme-system");

        click("privacySettings");
        waitForForm("Privacy");
        shot("privacy");
        flip("readReceipts");
        until(() -> "false".equals(String.valueOf(carol.get("/api/account/preferences")
                .get("readReceipts"))), 20000, "read receipts off on the account");
        flip("readReceipts");
        until(() -> "true".equals(String.valueOf(carol.get("/api/account/preferences")
                .get("readReceipts"))), 20000, "read receipts back on");
        back();
        waitForForm("Settings");
        until(() -> {
            Map<String, Object> kept = carol.get("/api/account/preferences");
            return "system".equals(Actor.text(kept, "theme"))
                    && "en".equals(Actor.text(kept, "language"));
        }, 20000, "the account to hold what was chosen");
        click("signOut");
        waitForForm("Welcome");
        return true;
    }

    /// A switch is not a button: flip it the way a tap would.
    private void flip(String name) {
        until(() -> findByName(name) instanceof Switch, 20000, "a switch called " + name);
        onEdt(() -> {
            Switch toggle = (Switch) findByName(name);
            toggle.setValue(!toggle.isValue());
        });
    }
}
