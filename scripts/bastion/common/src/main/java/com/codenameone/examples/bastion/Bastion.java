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
package com.codenameone.examples.bastion;

import com.codename1.system.Lifecycle;
import com.codename1.ui.Form;
import com.codenameone.examples.bastion.net.Guard;
import com.codenameone.examples.bastion.net.Session;
import com.codenameone.examples.bastion.security.Keys;
import com.codenameone.examples.bastion.security.Lock;
import com.codenameone.examples.bastion.security.Protect;
import com.codenameone.examples.bastion.ui.Lang;
import com.codenameone.examples.bastion.ui.LockForm;
import com.codenameone.examples.bastion.ui.Nav;
import com.codenameone.examples.bastion.ui.SignInForm;
import com.codenameone.examples.bastion.ui.Ui;

/// The app. Before anything touches the network the guard goes in (no
/// cleartext, a host allow-list, certificate pins, AppShield); then the
/// stored session is picked up behind the lock.
///
/// Where things are:
///
/// - `net` -- the guard, the session (password, second factor, passkey) and
///   the typed clients generated from the `shared` contracts.
/// - `security` -- the device signing key, request signing, the app lock,
///   screen and clipboard protection, and the Security Center's status.
/// - `ui` -- the screens.
/// - `AppConfig` -- the server, the allowed hosts, the pins and the timeouts.
public class Bastion extends Lifecycle {
    @Override
    public void runApp() {
        Lang.apply();
        Guard.install();
        Protect.install();
        Session.start(AppConfig.serverUrl());
        Session.onSignedOut(Bastion::signInScreen);
        Nav.install(Bastion::signInScreen);
        Lock.install(new Lock.Screens() {
            @Override
            public void showLocked() {
                LockForm.show(() -> Session.signOut(Bastion::signInScreen));
            }

            @Override
            public void showUnlocked(Form previous) {
                if (Session.user() == null) {
                    // Unlocked at start: the session's tokens could not be
                    // read until now.
                    Session.restore(Bastion::home, Bastion::signInScreen,
                            Bastion::signInScreen);
                } else if (previous != null) {
                    Ui.show(previous, true);
                } else {
                    home();
                }
            }
        });
        if (Session.hasStoredSession() && Keys.exists() && Keys.protectedByUser()) {
            // The stored session is sealed under the app key: the biometric
            // or PIN that opens the key comes first (LockForm), then the
            // session is read.
            Lock.lockAtStart();
        } else if (Session.hasStoredSession() && Keys.openPlain()) {
            // No biometric or PIN set up: the session opens behind the lock,
            // whose only way out is the password (LockForm).
            Session.restore(Lock::lock, Bastion::signInScreen, Bastion::signInScreen);
        } else {
            signInScreen();
        }
    }

    @Override
    public void start() {
        super.start();
        Lock.resumed();
    }

    @Override
    public void stop() {
        super.stop();
        Lock.paused();
    }

    static void signInScreen() {
        // A key the user did not open is given up here, before the sign-in
        // stores new tokens under whatever key is current (Keys#signedIn).
        Keys.signedIn();
        SignInForm.show(() -> {
            // A fresh sign-in is the strongest unlock there is; whatever was
            // on screen before belonged to the session that ended.
            Lock.reset();
            home();
        });
    }

    static void home() {
        Nav.home();
    }
}
