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
package com.codenameone.examples.parley.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.parley.AppConfig;
import com.codenameone.examples.parley.Prefs;
import com.codenameone.examples.parley.Telemetry;
import com.codenameone.examples.parley.admin.AdminForm;
import com.codenameone.examples.parley.api.UserDto;
import com.codenameone.examples.parley.chat.Media;
import com.codenameone.examples.parley.chat.Messenger;
import com.codenameone.examples.parley.net.Account;
import com.codenameone.examples.parley.net.Session;
import com.codenameone.examples.parley.notify.Notifications;

/// Decides which screen the app is on.
///
/// The app has two homes: the chats, which everyone has, and the console,
/// for an account with the admin role. Which of them a user may enter is the
/// server's decision -- the roles on their account, as `/api/me` reports them
/// -- and hiding the console here is a courtesy, not the protection; the
/// server refuses an admin call from anyone without the role.
public final class Nav {
    public static final String CHATS = "chats";
    public static final String ADMIN = "admin";

    /// The home screen of a mode.
    public interface Home {
        /// Fills the screen in and starts it talking to the server. Called
        /// once, before the screen is first shown.
        void start();

        Form form();
    }

    /// The home screen last built, and the mode it is the home of.
    private static Form homeForm;
    private static String homeMode = "";

    private Nav() {
    }

    /// Whether `form` is still the home screen, for an answer that arrives
    /// after the user has gone to another mode or signed out and should then
    /// change nothing.
    public static boolean isHome(Form form) {
        return form != null && form == homeForm;
    }

    /// The first screen: wherever the stored session leads.
    public static void launch() {
        Session.onSignedOut(() -> {
            Messenger.stop();
            Telemetry.signedOut("expired");
            WelcomeForm.show();
        });
        Form starting = Ui.form("", "Starting");
        starting.getToolbar().hideToolbar();
        starting.show();
        Session.restore(Nav::enter, WelcomeForm::show, Nav::unreachable);
    }

    /// Someone has just signed in, or been found still signed in: their
    /// account's settings are fetched and applied, this device's keys are
    /// opened (or made, the first time), and then their home is shown.
    public static void enter() {
        // Where this phone is to be sent notifications, now that there is
        // someone to send them to.
        Notifications.signedIn();
        Account.load(() -> {
            Look.apply();
            Lang.apply();
            Media.cleanUp();
            Messenger.start(() -> {
                if (Telemetry.asked()) {
                    home();
                } else {
                    // Once on a device, before the first screen that would be
                    // counted: whether it may be.
                    ConsentForm.show(Nav::home);
                }
            }, (status, message) -> {
                Ui.say(message);
                unreachable();
            });
        });
    }

    /// Shows the home screen of the mode the user is in.
    public static void home() {
        Form home = build();
        if (home != null) {
            home.show();
        }
    }

    /// The account was asked about again: back to the home screen only if
    /// the mode it opens in is no longer the one on screen.
    public static void refreshed() {
        UserDto user = Session.user();
        if (user == null || !mode(user).equals(homeMode)) {
            home();
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
        String mode = mode(user);
        Telemetry.role(mode);
        Home home = ADMIN.equals(mode) ? new AdminForm() : new ChatsForm();
        home.start();
        homeForm = home.form();
        homeMode = mode;
        return homeForm;
    }

    /// The mode to open for `user`: the one they chose last while their
    /// account still allows it, else the chats.
    public static String mode(UserDto user) {
        String saved = AppConfig.mode(Session.owner(user.username()));
        if (ADMIN.equals(saved) && user.admin()) {
            return ADMIN;
        }
        return CHATS;
    }

    /// The user chose `mode` from the menu.
    public static void switchTo(String mode) {
        UserDto user = Session.user();
        AppConfig.setMode(mode, user == null ? "" : Session.owner(user.username()));
        home();
    }

    /// A notification was opened: the chat it is about, over the home.
    public static void showConversation(String conversationId) {
        UserDto user = Session.user();
        if (user == null || conversationId == null || conversationId.length() == 0) {
            return;
        }
        Form current = CN.getCurrentForm();
        if (current instanceof ChatForm && ((ChatForm) current).conversationId().equals(conversationId)) {
            return;
        }
        if (!CHATS.equals(homeMode) || homeForm == null) {
            AppConfig.setMode(CHATS, Session.owner(user.username()));
            build();
        }
        ChatForm.show(conversationId, homeForm);
    }

    public static void signOut() {
        leave("user", true);
    }

    /// The account was closed: signed out, and what the device kept for the
    /// usage reports forgotten with it.
    public static void deleted() {
        String owner = Session.owner();
        // Closing the account already made the server forget its devices.
        leave("deleted", false);
        Messenger.wipe(owner);
        Telemetry.forget();
        // Nobody is coming back for where that account went.
        Prefs.ownedBy("");
    }

    private static void leave(final String reason, boolean forgetDevice) {
        Account.flush();
        Messenger.stop();
        homeForm = null;
        homeMode = "";
        // The server forgets this phone for the account first, while there is
        // still a session to ask with. The device's keys and history stay in
        // its vault, so signing back in on this phone finds them again; the
        // delete-account path wipes them.
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
            AppConfig.setServerUrl(url);
            Session.start(AppConfig.serverUrl());
            launch();
        };
        if (AppConfig.sameServer(url, Session.serverUrl()) || Session.user() == null) {
            Messenger.stop();
            switchOver.run();
            return;
        }
        // Signed in: sign out the way the Sign out button does, against the
        // server still in use, before pointing anywhere else.
        Account.flush();
        Messenger.stop();
        homeForm = null;
        homeMode = "";
        Notifications.signingOut(() -> {
            Telemetry.signedOut("server");
            Session.signOut(switchOver);
        });
    }

    /// There is a session, and no server to use it with.
    private static void unreachable() {
        Form form = Ui.form("Parley", "Unreachable");
        Container page = Ui.page();
        page.add(Ui.label("No connection", "PlTitle"));
        page.add(Ui.text(Lang.tr("The server at {0} did not answer.", Session.serverUrl()),
                "PlText"));
        page.add(Ui.primary("Try again", "retry", e -> launch()));
        if (AppConfig.serverSwitchable()) {
            page.add(Ui.secondary("Server address", "server", e -> ServerForm.show(form)));
        }
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
