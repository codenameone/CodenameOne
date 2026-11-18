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
package com.codenameone.examples.brickyard.ui;

import com.codename1.ui.CN;
import com.codenameone.examples.brickyard.AppConfig;
import com.codenameone.examples.brickyard.engage.Friends;
import com.codenameone.examples.brickyard.engage.Store;
import com.codenameone.examples.brickyard.engage.Telemetry;
import com.codenameone.examples.brickyard.net.Session;

/// Where the app goes: sign-in when there is no session, home otherwise,
/// or the experience a deep link or an invite asked for.
public final class Nav {
    private static String pendingExperience;

    private Nav() {
    }

    public static void launch() {
        pendingExperience = AppConfig.launchExperience();
        Session.onSignedOut(() -> CN.callSerially(SignInForm::show));
        Session.restore(() -> CN.callSerially(Nav::enter),
                () -> CN.callSerially(SignInForm::show),
                () -> CN.callSerially(() -> {
                    SignInForm.show();
                    Ui.toast("The server did not answer. Is it running?");
                }));
    }

    public static void enter() {
        String invited = Friends.takePending();
        String id = pendingExperience != null ? pendingExperience : invited;
        pendingExperience = null;
        Store.refresh();
        if (id != null) {
            ExperienceForm.show(id);
            return;
        }
        home();
    }

    public static void signedIn() {
        Telemetry.signedIn();
        enter();
    }

    public static void home() {
        HomeForm.show();
    }

    /// Opens one of the bottom bar's sections, by its index in [Ui#SECTIONS].
    public static void section(int index) {
        switch (index) {
            case 1:
                CreateForm.show();
                break;
            case 2:
                FriendsForm.show();
                break;
            case 3:
                ShopForm.show();
                break;
            case 4:
                AvatarForm.show();
                break;
            default:
                home();
                break;
        }
    }

    public static void signOut() {
        Session.signOut(() -> CN.callSerially(SignInForm::show));
    }
}
