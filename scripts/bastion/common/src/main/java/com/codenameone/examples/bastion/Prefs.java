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

import com.codename1.io.Preferences;

/// The few choices the user makes on this device. Nothing here is a secret:
/// a choice is a preference, and every secret is in secure storage.
public final class Prefs {
    private static final String BIOMETRIC = "bastion.biometricUnlock";
    private static final String ONBOARDED = "bastion.onboarded.";

    private Prefs() {
    }

    /// Whether the user chose to unlock with a fingerprint or face. Off, the
    /// lock asks for the password again: a stronger default than a
    /// biometric nobody agreed to.
    public static boolean biometricUnlock() {
        return Preferences.get(BIOMETRIC, false);
    }

    public static void setBiometricUnlock(boolean on) {
        Preferences.set(BIOMETRIC, on);
    }

    /// Whether `user` has been through onboarding on this device; the steps
    /// they skipped are offered again until then.
    public static boolean onboarded(String user) {
        return user != null && Preferences.get(ONBOARDED + user, false);
    }

    public static void setOnboarded(String user) {
        if (user != null) {
            Preferences.set(ONBOARDED + user, true);
        }
    }
}
