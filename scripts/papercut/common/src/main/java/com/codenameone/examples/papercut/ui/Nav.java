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
package com.codenameone.examples.papercut.ui;

import com.codename1.ui.CN;
import com.codenameone.examples.papercut.AppConfig;
import com.codenameone.examples.papercut.admin.AdminConsole;
import com.codenameone.examples.papercut.engage.Notifications;
import com.codenameone.examples.papercut.engage.Store;
import com.codenameone.examples.papercut.net.Session;
import com.codenameone.examples.papercut.play.Arena;

/// Where the game goes between screens, in one place.
public final class Nav {
    private static boolean wantDaily;

    private Nav() {
    }

    /// The first screen is always the page with the menu on it: nobody has to
    /// sign in to play. A stored session is picked up behind it, quietly.
    public static void launch() {
        wantDaily = AppConfig.openedForDaily();
        Session.onSignedOut(() -> CN.callSerially(Nav::home));
        Session.restore(() -> CN.callSerially(Nav::enter), () -> CN.callSerially(Nav::home),
                () -> CN.callSerially(Nav::home));
    }

    /// Signed in, from a stored session or the sign-in screen. An admin who
    /// was last in the console goes back there.
    public static void enter() {
        Notifications.signedIn();
        Store.resend();
        if (wantDaily) {
            wantDaily = false;
            daily();
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
        enter();
    }

    public static void home() {
        if (Session.user() != null) {
            AppConfig.setMode("player", Session.user().username());
        }
        new HomeForm().show();
    }

    /// Today's sheet: the server deals the seed when it can, so every player
    /// meets the same creatures.
    public static void daily() {
        Runs.play(Arena.DAILY);
    }

    public static void adminConsole() {
        AppConfig.setMode("admin", Session.user().username());
        AdminConsole.show();
    }

    public static void signOut() {
        Notifications.signingOut(() -> Session.signOut(() -> CN.callSerially(Nav::home)));
    }
}
