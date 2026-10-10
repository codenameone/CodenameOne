/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline;

import com.codenameone.examples.wayline.net.Account;
import java.util.Map;

/// The settings: a language and a theme that change the app on the spot, and
/// a ride preference that reaches the server and the next ride asked for.
public class SettingsTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        final Actor rider = Actor.signIn(RIDER);
        signIn(RIDER, "Rider");
        click("menu-open");
        click("menu-settings");
        waitForForm("Settings");
        untilText("signOut", "Sign out");
        shot("settings");

        // Another language, at once and on the account.
        click("language-he");
        until(() -> text("signOut").length() > 0 && !"Sign out".equals(text("signOut")), 20000,
                "the screen to be in Hebrew");
        until(() -> "he".equals(Actor.text(rider.get("/api/account/preferences"), "language")),
                20000, "the language to be kept with the account");
        shot("settings-hebrew");
        click("language-en");
        untilText("signOut", "Sign out");

        // Dark, and the screen behind is dark as well.
        click("theme-dark");
        until(() -> "dark".equals(Actor.text(rider.get("/api/account/preferences"), "theme")),
                20000, "the theme to be kept with the account");
        shot("settings-dark");
        back();
        waitForForm("Rider");
        shot("rider-home-dark");
        click("menu-open");
        click("menu-settings");
        waitForForm("Settings");
        click("theme-system");

        // A woman at the wheel: kept with the account, and sent with the ride.
        click("driverGender-women");
        until(() -> Account.lastSent() != null
                && "women".equals(Account.lastSent().driverGender), 20000,
                "the preference to be sent");
        until(() -> {
            Map<String, Object> kept = rider.get("/api/account/preferences");
            return "women".equals(Actor.text(kept, "driverGender"))
                    && "system".equals(Actor.text(kept, "theme"))
                    && "en".equals(Actor.text(kept, "language"));
        }, 20000, "the account to hold what was chosen");
        back();
        waitForForm("Rider");
        quote();
        click("request");
        untilText("rideState", "Finding a driver");
        Map<String, Object> ride = rider.get("/api/rides/active");
        assertEqual("women", Actor.text(ride, "driverGender"), "the ride asks for a woman");
        // With no woman driving nearby the server may give the ride up before
        // the rider does; either way the screen ends where it began.
        until(() -> {
            clickIfThere("dismiss");
            clickIfThere("cancel");
            return findByName("whereTo") != null;
        }, 30000, "the ride to be cancelled");

        // Back as it was, for whoever uses the account next.
        click("menu-open");
        click("menu-settings");
        waitForForm("Settings");
        click("driverGender-any");
        until(() -> "any".equals(Actor.text(rider.get("/api/account/preferences"),
                "driverGender")), 20000, "the preference to be put back");
        click("signOut");
        waitForForm("Welcome");
        return true;
    }
}
