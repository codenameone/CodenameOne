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
package com.codenameone.examples.savor;

import com.codename1.ui.RadioButton;
import java.util.Map;

/// The settings: a language and a theme that change the app on the spot, and
/// a default tip that reaches the server and the next checkout.
public class SettingsTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        final Actor customer = Actor.signIn(CUSTOMER);
        signIn(CUSTOMER, "Customer");
        click("menu-open");
        click("menu-settings");
        waitForForm("Settings");
        untilText("signOut", "Sign out");
        shot("settings");

        // Another language, at once and on the account.
        click("language-he");
        until(() -> text("signOut").length() > 0 && !"Sign out".equals(text("signOut")), 20000,
                "the screen to be in Hebrew");
        until(() -> "he".equals(Actor.text(customer.get("/api/account/preferences"), "language")),
                20000, "the language to be kept with the account");
        shot("settings-hebrew");
        click("language-en");
        untilText("signOut", "Sign out");

        // Dark, and the screen behind is dark as well.
        click("theme-dark");
        until(() -> "dark".equals(Actor.text(customer.get("/api/account/preferences"), "theme")),
                20000, "the theme to be kept with the account");
        shot("settings-dark");
        back();
        waitForForm("Customer");
        shot("customer-home-dark");
        click("menu-open");
        click("menu-settings");
        waitForForm("Settings");
        click("theme-system");

        // A tip chosen here is the one the next checkout starts from, and is
        // kept with the account.
        click("tip-15");
        until(() -> {
            Object kept = customer.get("/api/account/preferences").get("defaultTipPercent");
            return kept instanceof Number && ((Number) kept).intValue() == 15;
        }, 20000, "the tip to be kept with the account");
        until(() -> {
            Map<String, Object> kept = customer.get("/api/account/preferences");
            return "system".equals(Actor.text(kept, "theme"))
                    && "en".equals(Actor.text(kept, "language"));
        }, 20000, "the account to hold what was chosen");
        back();
        waitForForm("Customer");
        cart();
        assertTrue(findByName("tip-15") instanceof RadioButton
                && ((RadioButton) findByName("tip-15")).isSelected(),
                "the checkout starts from the chosen tip");
        back();
        waitForForm("restaurant");
        back();
        waitForForm("Customer");

        // Back as it was, for whoever uses the account next.
        click("menu-open");
        click("menu-settings");
        waitForForm("Settings");
        click("tip-0");
        until(() -> {
            Object kept = customer.get("/api/account/preferences").get("defaultTipPercent");
            return kept instanceof Number && ((Number) kept).intValue() == 0;
        }, 20000, "the tip to be put back");
        click("signOut");
        waitForForm("Welcome");
        return true;
    }
}
