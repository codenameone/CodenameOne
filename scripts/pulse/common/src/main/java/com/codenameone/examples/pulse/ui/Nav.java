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
package com.codenameone.examples.pulse.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.pulse.AppConfig;
import com.codenameone.examples.pulse.Prefs;
import com.codenameone.examples.pulse.Telemetry;
import com.codenameone.examples.pulse.api.UserDto;
import com.codenameone.examples.pulse.desk.DeskForm;
import com.codenameone.examples.pulse.live.LiveChannel;
import com.codenameone.examples.pulse.member.MemberForm;
import com.codenameone.examples.pulse.member.NudgesForm;
import com.codenameone.examples.pulse.member.Reminders;
import com.codenameone.examples.pulse.net.Account;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.net.Session;
import com.codenameone.examples.pulse.notify.Notifications;
import com.codenameone.examples.pulse.owner.OwnerForm;
import com.codenameone.examples.pulse.trainer.TrainerForm;

/// Decides which screen the app is on.
///
/// The app is four apps in one -- for training, for the front desk, for
/// coaching and for running the studio -- and which of them a user may enter is
/// the server's decision: it is the roles on their account, as `/api/me`
/// reports them. Each has a home screen of its own, with its own menu, and the
/// user is in one of them at a time. Hiding a mode here is a courtesy and not
/// the protection; the server refuses a desk, trainer or owner call from anyone
/// without the role, whatever the app shows.
public final class Nav {
    public static final String MEMBER = "member";
    public static final String DESK = "desk";
    public static final String TRAINER = "trainer";
    public static final String OWNER = "owner";

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
        // Where this phone is to be sent notifications, now that there is
        // someone to send them to.
        Notifications.signedIn();
        // Plan prices are shown in the server's currency, which only the
        // payment config says.
        Api.payments().config(Net.to(config -> Ui.setCurrency(config.currency()), (status, message) -> { }));
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

    /// The account was asked about again: back to the home screen only if
    /// the mode it opens in is no longer the one on screen, so somebody whose
    /// roles did not change is not pulled out of what they were doing.
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
        LiveChannel.start();
        String mode = mode(user);
        Telemetry.role(mode);
        Home home = OWNER.equals(mode) ? new OwnerForm()
                : TRAINER.equals(mode) ? new TrainerForm()
                : DESK.equals(mode) ? new DeskForm()
                : new MemberForm();
        home.start();
        homeForm = home.form();
        homeMode = mode;
        return homeForm;
    }

    /// The mode to open for `user`.
    ///
    /// The one they chose last, while their account still allows it. With no
    /// choice made, the one their account is for: an owner runs the studio, a
    /// desk account is a kiosk, a trainer coaches, and everyone else trains.
    /// A choice is remembered for the account that made it, so the next person
    /// to sign in on the same device starts where their own account does.
    public static String mode(UserDto user) {
        String saved = AppConfig.mode(Session.owner(user.username()));
        if (OWNER.equals(saved) && user.owner()) {
            return OWNER;
        }
        if (TRAINER.equals(saved) && user.trainer()) {
            return TRAINER;
        }
        if (DESK.equals(saved) && user.desk()) {
            return DESK;
        }
        if (MEMBER.equals(saved)) {
            return MEMBER;
        }
        return user.owner() ? OWNER : user.desk() ? DESK
                : user.trainer() ? TRAINER : MEMBER;
    }

    /// The user chose `mode` from the menu.
    public static void switchTo(String mode) {
        UserDto user = Session.user();
        AppConfig.setMode(mode, user == null ? "" : Session.owner(user.username()));
        home();
    }

    /// A push notification was opened: the member's inbox, over the member's
    /// home, whatever mode the app was in.
    public static void showNudges() {
        UserDto user = Session.user();
        if (user == null) {
            return;
        }
        if (!MEMBER.equals(homeMode) || homeForm == null) {
            AppConfig.setMode(MEMBER, Session.owner(user.username()));
            Form home = build();
            if (home == null) {
                return;
            }
            NudgesForm.show(home);
            return;
        }
        NudgesForm.show(homeForm);
    }

    public static void signOut() {
        leave("user", true);
    }

    /// The account was closed: signed out, and what the device kept for the
    /// usage reports forgotten with it.
    public static void deleted() {
        // Closing the account already made the server forget its devices.
        leave("deleted", false);
        Telemetry.forget();
        // Nobody is coming back for where that account went.
        Prefs.ownedBy("");
    }

    private static void leave(final String reason, boolean forgetDevice) {
        Account.flush();
        Reminders.clear();
        LiveChannel.stop();
        homeForm = null;
        homeMode = "";
        // The server forgets this phone for the account first, while there is
        // still a session to ask with.
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
            LiveChannel.stop();
            switchOver.run();
            return;
        }
        // Signed in: sign out the way the Sign out button does, against the
        // server still in use, before pointing anywhere else. Session.start
        // only forgets the tokens on this phone, so the old server would keep
        // a live refresh token and this phone's push registration, and go on
        // notifying it about that account's classes.
        Account.flush();
        Reminders.clear();
        LiveChannel.stop();
        homeForm = null;
        homeMode = "";
        Notifications.signingOut(() -> {
            Telemetry.signedOut("server");
            Session.signOut(switchOver);
        });
    }

    /// There is a session, and no server to use it with.
    private static void unreachable() {
        Form form = Ui.form("Pulse", "Unreachable");
        Container page = Ui.page();
        page.add(Ui.label("No connection", "SvTitle"));
        page.add(Ui.text(Lang.tr("The server at {0} did not answer.", Session.serverUrl()),
                "SvText"));
        page.add(Ui.primary("Try again", "retry", e -> launch()));
        if (AppConfig.serverSwitchable()) {
            page.add(Ui.secondary("Server address", "server", e -> ServerForm.show(form)));
        }
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
