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
package com.codenameone.examples.skirmish.ui;

import com.codename1.ui.CN;
import com.codenameone.examples.skirmish.AppConfig;
import com.codenameone.examples.skirmish.admin.AdminConsole;
import com.codenameone.examples.skirmish.engage.Notifications;
import com.codenameone.examples.skirmish.engage.Store;
import com.codenameone.examples.skirmish.engage.Telemetry;
import com.codenameone.examples.skirmish.forge.PackScreen;
import com.codenameone.examples.skirmish.net.Session;

/// Where the app goes. Every screen change that is not a plain "back" comes
/// through here, so a push notification, a challenge link and a button all
/// land on the same screen the same way.
public final class Nav {
    private static String pendingChallenge;
    private static String pendingPack;

    private Nav() {
    }

    /// The first screen: the stored session decides between home and sign-in.
    public static void launch() {
        pendingChallenge = AppConfig.challengeCode();
        pendingPack = AppConfig.packId();
        Session.onSignedOut(() -> CN.callSerially(SignInForm::show));
        Session.restore(() -> CN.callSerially(Nav::enter),
                () -> CN.callSerially(SignInForm::show),
                () -> CN.callSerially(() -> {
                    SignInForm.show();
                    Ui.toast("The server did not answer. Is it running?");
                }));
    }

    /// Signed in: a challenge or pack link opened the app wins, then the admin
    /// console for an administrator who was last there, then home.
    public static void enter() {
        Notifications.signedIn();
        Store.resend();
        if (pendingChallenge != null) {
            String code = pendingChallenge;
            pendingChallenge = null;
            challenge(code);
            return;
        }
        if (pendingPack != null) {
            String id = pendingPack;
            pendingPack = null;
            PackScreen.show(id);
            return;
        }
        if (Session.user() != null && Session.user().admin()
                && "admin".equals(AppConfig.mode(Session.user().username()))) {
            adminConsole();
            return;
        }
        home();
    }

    public static void signedIn() {
        Telemetry.signedIn();
        enter();
    }

    public static void home() {
        AppConfig.setMode("player", Session.user() == null ? "" : Session.user().username());
        HomeForm.show();
    }

    public static void challenge(String code) {
        MatchmakingForm.join(code);
    }

    public static void adminConsole() {
        AppConfig.setMode("admin", Session.user().username());
        AdminConsole.show();
    }

    public static void shop() {
        ShopForm.show();
    }

    public static void signOut() {
        Notifications.signingOut(() -> Session.signOut(() -> CN.callSerially(SignInForm::show)));
    }
}
