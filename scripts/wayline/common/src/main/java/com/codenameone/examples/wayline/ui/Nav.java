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
package com.codenameone.examples.wayline.ui;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.wayline.AppConfig;
import com.codenameone.examples.wayline.Telemetry;
import com.codenameone.examples.wayline.admin.AdminForm;
import com.codenameone.examples.wayline.api.UserDto;
import com.codenameone.examples.wayline.driver.ApplyForm;
import com.codenameone.examples.wayline.driver.DriverForm;
import com.codenameone.examples.wayline.live.LiveChannel;
import com.codenameone.examples.wayline.net.Account;
import com.codenameone.examples.wayline.net.Session;
import com.codenameone.examples.wayline.rider.RiderForm;

/// Decides which screen the app is on.
///
/// The app is three apps in one -- for riding, for driving and for
/// administering -- and which of them a user may enter is the server's
/// decision: it is the roles on their account, as `/api/me` reports them. Each
/// has a home screen of its own, with its own menu, and the user is in one of
/// them at a time. Hiding a mode here is a courtesy and not the protection; the
/// server refuses a driver's or an admin's call from anyone without the role,
/// whatever the app shows.
public final class Nav {
    public static final String RIDER = "rider";
    public static final String DRIVER = "driver";
    public static final String ADMIN = "admin";

    /// The home screen of a mode.
    public interface Home {
        /// Fills the screen in and starts it talking to the server. Called
        /// once, before the screen is first shown.
        void start();

        Form form();
    }

    private Nav() {
    }

    /// The first screen: wherever the stored session leads.
    public static void launch() {
        Session.onSignedOut(() -> {
            LiveChannel.stop();
            Telemetry.signedOut("expired");
            WelcomeForm.show();
        });
        Form starting = Ui.form("", "Starting");
        starting.getToolbar().hideToolbar();
        starting.show();
        Session.restore(Nav::enter, WelcomeForm::show, Nav::unreachable);
    }

    /// Someone has just signed in, or been found still signed in: their
    /// account's settings are fetched, applied, and then their home is shown in
    /// the language and the colours they chose.
    public static void enter() {
        Account.load(() -> {
            Look.apply();
            Lang.apply();
            if (Telemetry.asked()) {
                home();
            } else {
                // Once on a device, before the first screen that would be
                // counted: whether it may be.
                ConsentForm.show(Nav::home);
            }
        });
    }

    /// Shows the home screen of the mode the user is in.
    public static void home() {
        Form home = build();
        if (home != null) {
            home.show();
        }
    }

    /// Builds the home screen of the mode the user is in, without showing it:
    /// for a caller that is about to show something on top of it. Null, and
    /// the welcome screen showing, when nobody is signed in.
    public static Form build() {
        UserDto user = Session.user();
        if (user == null) {
            WelcomeForm.show();
            return null;
        }
        LiveChannel.start();
        String mode = mode(user);
        Telemetry.role(mode);
        // Driving is for the approved. Anyone else who chooses it gets the
        // way there: the application, and what has become of it.
        Home home = ADMIN.equals(mode) ? new AdminForm()
                : !DRIVER.equals(mode) ? new RiderForm()
                : user.driver ? new DriverForm() : new ApplyForm();
        home.start();
        return home.form();
    }

    /// The mode to open for `user`.
    ///
    /// The one they chose last, while their account still allows it. With no
    /// choice made, the one their account is for: an admin administers, a
    /// driver drives, and everyone else rides. A choice is remembered for the
    /// account that made it, so the next person to sign in on the same device
    /// starts where their own account does.
    public static String mode(UserDto user) {
        String saved = AppConfig.mode(user.username);
        if (ADMIN.equals(saved) && user.admin) {
            return ADMIN;
        }
        // Not only for a driver: someone who is applying to be one is in the
        // driver's side of the app too, on the screen that says how it stands.
        if (DRIVER.equals(saved)) {
            return DRIVER;
        }
        if (RIDER.equals(saved)) {
            return RIDER;
        }
        return user.admin ? ADMIN : user.driver ? DRIVER : RIDER;
    }

    /// The user chose `mode` from the menu.
    public static void switchTo(String mode) {
        UserDto user = Session.user();
        AppConfig.setMode(mode, user == null ? "" : user.username);
        home();
    }

    public static void signOut() {
        leave("user");
    }

    /// The account was closed: signed out, and what the device kept for the
    /// usage reports forgotten with it.
    public static void deleted() {
        leave("deleted");
        Telemetry.forget();
    }

    private static void leave(String reason) {
        Account.flush();
        LiveChannel.stop();
        Telemetry.signedOut(reason);
        Session.signOut(WelcomeForm::show);
    }

    /// Points the app at another server, which ends the session with this one.
    public static void changeServer(String url) {
        LiveChannel.stop();
        AppConfig.setServerUrl(url);
        Session.start(AppConfig.serverUrl());
        launch();
    }

    /// There is a session, and no server to use it with.
    private static void unreachable() {
        Form form = Ui.form("Wayline", "Unreachable");
        Container page = Ui.page();
        page.add(Ui.label("No connection", "WlTitle"));
        page.add(Ui.text(Lang.tr("The server at {0} did not answer.", Session.serverUrl()),
                "WlText"));
        page.add(Ui.primary("Try again", "retry", e -> launch()));
        page.add(Ui.secondary("Server address", "server", e -> ServerForm.show(form)));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
