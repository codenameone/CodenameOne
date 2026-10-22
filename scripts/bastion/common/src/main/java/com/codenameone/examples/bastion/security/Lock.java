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

import com.codename1.security.Biometrics;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.util.UITimer;
import com.codename1.util.SuccessCallback;
import com.codenameone.examples.bastion.AppConfig;
import com.codenameone.examples.bastion.net.Session;
import com.codenameone.examples.bastion.ui.Lang;

/// The app lock: when the app starts with a stored session, when it comes
/// back from the background after [AppConfig#BACKGROUND_LOCK_MILLIS], and
/// after [AppConfig#IDLE_LOCK_MILLIS] without a touch.
///
/// Locked, nothing of the account is on screen (the lock screen replaces the
/// form), and the app key ([Keys]) and the unsealed device key are dropped
/// from memory, so nothing can sign a payment or read the stored refresh
/// token until a copy of the key is opened again: by a biometric the OS
/// checks, else by the app PIN, else not at all -- the lock then signs the
/// session out, rather than letting the device passcode stand in for the
/// bank's.
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
    }

    /// Called by every form the app shows: a touch anywhere is activity, and
    /// the idle check moves to the form.
    ///
    /// A UITimer runs only while the form it is bound to is showing, so the
    /// check is bound again to each new form rather than once at start --
    /// where, before the first form, there is nothing to bind it to (the
    /// browser has no current form at that point at all).
    public static void watch(Form form) {
        form.addPointerPressedListener(evt -> touched());
        form.addPointerDraggedListener(evt -> touched());
        if (idle == null) {
            idle = new UITimer(Lock::checkIdle);
        } else {
            idle.cancel();
        }
        idle.schedule(5000, true, form);
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
        Keys.lock();
        // The tokens go too: nothing renews behind the lock (the rotated
        // refresh token could not be sealed while the key is closed, and
        // would be lost), every answer still on its way is dropped (the
        // session epoch moves), and the unlock reads them back from the
        // sealed store (Session#restore).
        Session.suspend();
        hidden = CN.getCurrentForm();
        screens.showLocked();
    }

    /// A form something asked to show while the app is locked -- an answer
    /// that arrived after the lock. It is not shown; it becomes the form the
    /// unlock goes back to.
    public static void defer(Form form) {
        hidden = form;
    }

    /// Locks at start, before the stored session is read: its refresh token
    /// is sealed under the key the unlock opens.
    public static void lockAtStart() {
        if (screens == null) {
            return;
        }
        locked = true;
        hidden = null;
        screens.showLocked();
    }

    /// The app went to the background.
    public static void paused() {
        backgroundSince = System.currentTimeMillis();
        Protect.paused();
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

    /// Asks the OS for the biometric copy of the app key: the prompt is the
    /// OS's, and only a match releases the key. `failed` gets true when that
    /// copy is gone for good and the PIN (or a sign-in) is the way in.
    public static void unlockWithBiometric(final SuccessCallback<Boolean> failed) {
        Keys.unlockWithBiometric(Lang.tr("Unlock Bastion and the keys that sign your payments"),
                Lock::unlocked, failed);
    }

    /// Tries the app PIN; see [Keys#unlockWithPin].
    public static void unlockWithPin(String pin, final Keys.PinResult result) {
        Keys.unlockWithPin(pin, new Keys.PinResult() {
            @Override
            public void unlocked() {
                result.unlocked();
                Lock.unlocked();
            }

            @Override
            public void wrong(int left) {
                result.wrong(left);
            }

            @Override
            public void wiped() {
                result.wiped();
            }

            @Override
            public void failed(String why) {
                result.failed(why);
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
