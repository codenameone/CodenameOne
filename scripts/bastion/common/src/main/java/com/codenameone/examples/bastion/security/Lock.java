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
package com.codenameone.examples.bastion.security;

import com.codename1.security.AuthenticationOptions;
import com.codename1.security.Biometrics;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.util.UITimer;
import com.codename1.util.SuccessCallback;
import com.codenameone.examples.bastion.AppConfig;
import com.codenameone.examples.bastion.net.Session;
import com.codenameone.examples.bastion.ui.Lang;

/// The app lock: a biometric check when the app starts with a stored
/// session, when it comes back from the background after
/// [AppConfig#BACKGROUND_LOCK_MILLIS], and after [AppConfig#IDLE_LOCK_MILLIS]
/// without a touch.
///
/// Locked, nothing of the account is on screen (the lock screen replaces the
/// form) and the unsealed device key is dropped from memory, so nothing can
/// sign a payment until [DeviceKey] unseals it again after the check. Where the device has no biometrics, or the user has none
/// enrolled, the fallback is the password: the lock signs the session out
/// rather than letting the device passcode stand in for the bank's.
public final class Lock {
    /// What to show while locked, and what to show once unlocked.
    public interface Screens {
        void showLocked();

        void showUnlocked(Form previous);
    }

    private static Screens screens;
    private static boolean locked;
    private static long lastTouch = System.currentTimeMillis();
    private static long backgroundSince;
    private static Form hidden;
    private static UITimer idle;

    private Lock() {
    }

    public static void install(Screens app) {
        screens = app;
        if (idle == null) {
            idle = UITimer.timer(5000, true, Lock::checkIdle);
        }
    }

    /// Called by every form the app shows: a touch anywhere is activity.
    public static void watch(Form form) {
        form.addPointerPressedListener(evt -> touched());
        form.addPointerDraggedListener(evt -> touched());
    }

    public static void touched() {
        lastTouch = System.currentTimeMillis();
    }

    public static boolean isLocked() {
        return locked;
    }

    /// Whether this device can unlock with a fingerprint or face.
    public static boolean biometricsAvailable() {
        Biometrics bio = Biometrics.getInstance();
        return bio.isSupported() && bio.canAuthenticate();
    }

    /// Locks now, if there is a session to protect.
    public static void lock() {
        if (locked || !Session.hasTokens() || screens == null) {
            return;
        }
        locked = true;
        DeviceKey.dropFromMemory();
        hidden = CN.getCurrentForm();
        screens.showLocked();
    }

    /// The app went to the background.
    public static void paused() {
        backgroundSince = System.currentTimeMillis();
    }

    /// The app is in front again.
    public static void resumed() {
        if (backgroundSince > 0
                && System.currentTimeMillis() - backgroundSince >= AppConfig.BACKGROUND_LOCK_MILLIS) {
            lock();
        }
        backgroundSince = 0;
        touched();
    }

    private static void checkIdle() {
        if (!locked && System.currentTimeMillis() - lastTouch >= AppConfig.IDLE_LOCK_MILLIS) {
            lock();
        }
    }

    /// Asks for the biometric check. `fallback` runs when there is none to
    /// ask for; `failed` when the user cancelled or failed it.
    public static void unlock(final Runnable fallback, final Runnable failed) {
        if (!biometricsAvailable()) {
            fallback.run();
            return;
        }
        AuthenticationOptions options = new AuthenticationOptions()
                .setTitle(Lang.tr("Unlock Bastion"))
                .setReason(Lang.tr("Unlock Bastion and the keys that sign your payments"))
                .setNegativeButtonText(Lang.tr("Cancel"))
                // Only a biometric: the device passcode is a second way in for
                // anyone who watched it typed.
                .setBiometricOnly(true)
                .setSensitiveTransaction(true);
        Biometrics.getInstance().authenticate(options).ready(new SuccessCallback<Boolean>() {
            @Override
            public void onSucess(Boolean ok) {
                if (ok != null && ok.booleanValue()) {
                    unlocked();
                } else {
                    failed.run();
                }
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable err) {
                failed.run();
            }
        });
    }

    /// Ends the lock without showing anything: after a fresh sign-in.
    public static void reset() {
        locked = false;
        hidden = null;
        touched();
    }

    static void unlocked() {
        locked = false;
        touched();
        Form previous = hidden;
        hidden = null;
        if (screens != null) {
            screens.showUnlocked(previous);
        }
    }
}
