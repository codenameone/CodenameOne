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
package com.codenameone.examples.reel.ui;

import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.reel.AppConfig;
import com.codenameone.examples.reel.Prefs;
import com.codenameone.examples.reel.Telemetry;
import com.codenameone.examples.reel.admin.AdminForm;
import com.codenameone.examples.reel.api.AccountDto;
import com.codenameone.examples.reel.moderation.ModerationForm;
import com.codenameone.examples.reel.net.Account;
import com.codenameone.examples.reel.net.Device;
import com.codenameone.examples.reel.net.Session;
import com.codenameone.examples.reel.notify.Notifications;
import com.codenameone.examples.reel.studio.StudioForm;
import com.codenameone.examples.reel.viewer.Downloads;
import com.codenameone.examples.reel.viewer.HomeForm;
import com.codenameone.examples.reel.viewer.ProfilesForm;

/// Decides which screen the app is on.
///
/// The app is four apps in one -- for watching, for making (the studio), for
/// moderating and for administering -- and which of them a user may enter is
/// the server's decision: it is the roles on their account, as `/api/me`
/// reports them. Each has a home screen of its own, with its own menu, and the
/// user is in one of them at a time. Hiding a mode here is a courtesy and not
/// the protection; the server refuses a creator's, a moderator's or an
/// admin's call from anyone without the role, whatever the app shows.
public final class Nav {
    public static final String VIEWER = "viewer";
    public static final String STUDIO = "studio";
    public static final String MODERATION = "moderation";
    public static final String ADMIN = "admin";

    public static final String ROLE_CREATOR = "CREATOR";
    public static final String ROLE_MODERATOR = "MODERATOR";
    public static final String ROLE_ADMIN = "ADMIN";

    /// The home screen of a mode.
    public interface Home {
        /// Fills the screen in and starts it talking to the server. Called
        /// once, before the screen is first shown.
        void start();

        Form form();
    }

    private static Form homeForm;
    private static String homeMode = "";

    private Nav() {
    }

    /// Whether `form` is still the home screen, for an answer that arrives
    /// after the user has gone elsewhere and should then change nothing.
    public static boolean isHome(Form form) {
        return form != null && form == homeForm;
    }

    /// The first screen: wherever the stored session leads.
    public static void launch() {
        Session.onSignedOut(() -> {
            Telemetry.signedOut("expired");
            Device.setProfileKey("");
            WelcomeForm.show();
        });
        Form starting = Ui.form("", "Starting");
        starting.getToolbar().hideToolbar();
        // On a desktop the first form decides how big the window opens;
        // without this it opens the size of a phone, and the catalog's wide
        // layout is never seen until someone drags the corner.
        starting.putClientProperty(Display.WINDOW_SIZE_HINT_PERCENT, new Dimension(80, 85));
        starting.show();
        Session.restore(Nav::enter, WelcomeForm::show, Nav::unreachable);
    }

    /// Someone has just signed in, or been found still signed in: the
    /// service's switches and the account's profiles are fetched, and then
    /// their home is shown in the language and colours they chose.
    public static void enter() {
        Notifications.signedIn();
        Account.load(() -> {
            Look.apply();
            Lang.apply();
            if (Telemetry.asked()) {
                home();
            } else {
                ConsentForm.show(Nav::home);
            }
        });
    }

    /// Shows the home screen of the mode the user is in. A viewer with more
    /// than one profile, or a protected one, picks who is watching first.
    public static void home() {
        AccountDto user = Session.user();
        if (user == null) {
            WelcomeForm.show();
            return;
        }
        if (VIEWER.equals(mode(user)) && ProfilesForm.needed()) {
            ProfilesForm.show(Nav::showHome);
            return;
        }
        showHome();
    }

    private static void showHome() {
        Form home = build();
        if (home != null) {
            home.show();
        }
    }

    /// The account was asked about again: back to the home screen only if
    /// the mode it opens in is no longer the one on screen.
    public static void refreshed() {
        AccountDto user = Session.user();
        if (user == null || !mode(user).equals(homeMode)) {
            home();
        }
    }

    /// Builds the home screen of the mode the user is in, without showing it.
    /// Null, and the welcome screen showing, when nobody is signed in.
    public static Form build() {
        AccountDto user = Session.user();
        if (user == null) {
            WelcomeForm.show();
            return null;
        }
        String mode = mode(user);
        Telemetry.role(mode);
        Home home = ADMIN.equals(mode) ? new AdminForm()
                : MODERATION.equals(mode) ? new ModerationForm()
                : STUDIO.equals(mode) ? new StudioForm() : new HomeForm();
        home.start();
        homeForm = home.form();
        homeMode = mode;
        return homeForm;
    }

    /// Whether the account may enter `mode`.
    /// Whether the app is in the viewer's mode: the catalog's own dark look,
    /// which a desktop leaves alone, where the consoles take the platform's.
    public static boolean viewing() {
        return VIEWER.equals(homeMode);
    }

    public static boolean allowed(AccountDto user, String mode) {
        if (VIEWER.equals(mode)) {
            return true;
        }
        boolean admin = has(user, ROLE_ADMIN);
        if (STUDIO.equals(mode)) {
            return admin || has(user, ROLE_CREATOR) || Account.enabled(com.codenameone.examples.reel.api.Features.OPEN_UPLOADS);
        }
        if (MODERATION.equals(mode)) {
            return admin || has(user, ROLE_MODERATOR);
        }
        return ADMIN.equals(mode) && admin;
    }

    static boolean has(AccountDto user, String role) {
        return user != null && user.roles() != null && user.roles().contains(role);
    }

    /// The mode to open for `user`: the one they chose last on this device,
    /// while their account still allows it, and otherwise watching. Staff
    /// watch too; their tools are one tap away in the menu.
    public static String mode(AccountDto user) {
        String saved = AppConfig.mode(Session.owner(user.username()));
        if (saved != null && saved.length() > 0 && allowed(user, saved)) {
            return saved;
        }
        return VIEWER;
    }

    /// The user chose `mode` from the menu.
    public static void switchTo(String mode) {
        AccountDto user = Session.user();
        AppConfig.setMode(mode, user == null ? "" : Session.owner(user.username()));
        home();
    }

    /// Picks another profile to watch as.
    public static void switchProfile() {
        Prefs.setProfile("");
        Device.setProfileKey("");
        ProfilesForm.show(Nav::showHome);
    }

    public static void signOut() {
        leave("user", true);
    }

    /// The account was closed: signed out, and what the device kept for it
    /// forgotten with it.
    public static void deleted() {
        leave("deleted", false);
        Telemetry.forget();
        Prefs.ownedBy("");
    }

    private static void leave(final String reason, boolean forgetDevice) {
        // Downloads are licensed to this account on this device; the next
        // person to sign in must not find them.
        Downloads.forgetAll();
        homeForm = null;
        homeMode = "";
        Device.setProfileKey("");
        Runnable out = () -> {
            Telemetry.signedOut(reason);
            Session.signOut(WelcomeForm::show);
        };
        if (forgetDevice) {
            Notifications.signingOut(out);
        } else {
            out.run();
        }
    }

    /// Points the app at another server, which ends the session with this one.
    public static void changeServer(final String url) {
        Runnable switchOver = () -> {
            if (!AppConfig.sameServer(url, Session.serverUrl())) {
                // Their licenses belong to the server being left.
                Downloads.forgetAll();
            }
            AppConfig.setServerUrl(url);
            Session.start(AppConfig.serverUrl());
            launch();
        };
        if (AppConfig.sameServer(url, Session.serverUrl()) || Session.user() == null) {
            switchOver.run();
            return;
        }
        // Signed in: sign out against the server still in use first, so it
        // does not keep a live refresh token and this phone's push row.
        homeForm = null;
        homeMode = "";
        Device.setProfileKey("");
        Notifications.signingOut(() -> {
            Telemetry.signedOut("server");
            Session.signOut(switchOver);
        });
    }

    /// There is a session, and no server to use it with.
    private static void unreachable() {
        Form form = Ui.form("Reel", "Unreachable");
        Container page = Ui.page();
        page.add(Ui.label("No connection", "SvTitle"));
        page.add(Ui.text(Lang.tr("The server at {0} did not answer.", Session.serverUrl()), "SvText"));
        page.add(Ui.primary("Try again", "retry", e -> launch()));
        if (AppConfig.serverSwitchable()) {
            page.add(Ui.secondary("Server address", "server", e -> ServerForm.show(form)));
        }
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
