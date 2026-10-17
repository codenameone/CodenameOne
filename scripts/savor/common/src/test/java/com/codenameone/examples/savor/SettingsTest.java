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

import com.codenameone.examples.savor.net.Account;
import java.util.Map;

/// The settings: a language and a theme that change the app on the spot, and
/// a delivery preference that reaches the server and the next delivery asked for.
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

        // A woman at the wheel: kept with the account, and sent with the delivery.
        click("courierGender-women");
        until(() -> Account.lastSent() != null
                && "women".equals(Account.lastSent().courierGender()), 20000,
                "the preference to be sent");
        until(() -> {
            Map<String, Object> kept = customer.get("/api/account/preferences");
            return "women".equals(Actor.text(kept, "courierGender"))
                    && "system".equals(Actor.text(kept, "theme"))
                    && "en".equals(Actor.text(kept, "language"));
        }, 20000, "the account to hold what was chosen");
        back();
        waitForForm("Customer");
        quote();
        click("request");
        untilText("deliveryState", "Finding a courier");
        Map<String, Object> delivery = customer.get("/api/deliveries/active");
        assertEqual("women", Actor.text(delivery, "courierGender"), "the delivery asks for a woman");
        // With no woman onboarding nearby the server may give the delivery up before
        // the customer does; either way the screen ends where it began.
        until(() -> {
            clickIfThere("dismiss");
            clickIfThere("cancel");
            return findByName("whereTo") != null;
        }, 30000, "the delivery to be cancelled");

        // Back as it was, for whoever uses the account next.
        click("menu-open");
        click("menu-settings");
        waitForForm("Settings");
        click("courierGender-any");
        until(() -> "any".equals(Actor.text(customer.get("/api/account/preferences"),
                "courierGender")), 20000, "the preference to be put back");
        click("signOut");
        waitForForm("Welcome");
        return true;
    }
}
