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
package com.codenameone.examples.pulse;

import com.codename1.components.Switch;
import java.util.Map;

/// The settings: a language and a theme that change the app on the spot, and
/// class reminders that are kept with the account.
public class SettingsTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        final Actor member = Actor.signIn(MEMBER);
        signIn(MEMBER, "Member");
        click("menu-open");
        click("menu-settings");
        waitForForm("Settings");
        untilText("signOut", "Sign out");
        shot("settings");

        // Another language, at once and on the account.
        click("language-he");
        until(() -> text("signOut").length() > 0 && !"Sign out".equals(text("signOut")), 20000,
                "the screen to be in Hebrew");
        until(() -> "he".equals(Actor.text(member.get("/api/account/preferences"), "language")),
                20000, "the language to be kept with the account");
        shot("settings-hebrew");
        click("language-en");
        untilText("signOut", "Sign out");

        // Dark, and the screen behind is dark as well.
        click("theme-dark");
        until(() -> "dark".equals(Actor.text(member.get("/api/account/preferences"), "theme")),
                20000, "the theme to be kept with the account");
        shot("settings-dark");
        back();
        waitForForm("Member");
        shot("member-home-dark");
        click("menu-open");
        click("menu-settings");
        waitForForm("Settings");
        click("theme-system");

        // Class reminders off, on the account, and back on.
        flip("notifyClasses");
        until(() -> "false".equals(String.valueOf(member.get("/api/account/preferences")
                .get("notifyClasses"))), 20000, "the reminders to be off on the account");
        flip("notifyClasses");
        until(() -> {
            Map<String, Object> kept = member.get("/api/account/preferences");
            return "true".equals(String.valueOf(kept.get("notifyClasses")))
                    && "system".equals(Actor.text(kept, "theme"))
                    && "en".equals(Actor.text(kept, "language"));
        }, 20000, "the account to hold what was chosen");
        click("signOut");
        waitForForm("Welcome");
        return true;
    }

    /// Turns the switch called `name` the other way, as a tap on it does.
    private void flip(final String name) {
        until(() -> findByName(name) instanceof Switch, 20000, "a switch called " + name);
        onEdt(() -> {
            Switch toggle = (Switch) findByName(name);
            toggle.setValue(!toggle.isValue());
        });
    }
}
