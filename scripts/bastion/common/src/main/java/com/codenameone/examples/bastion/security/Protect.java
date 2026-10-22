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

import com.codename1.io.Preferences;
import com.codename1.security.DeviceIntegrity;
import com.codename1.security.Hash;
import com.codename1.security.TapjackingPolicy;
import com.codename1.security.shield.ShieldSignal;
import com.codename1.security.shield.ShieldSignals;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.util.Base64;
import com.codenameone.examples.bastion.AppConfig;

/// Screen and input protection.
///
/// - **Sensitive screens** (balances, card numbers, codes) set the platform's
///   secure-window flag while they are showing: no screenshots, no screen
///   recording, and a blank card in the recent-apps list (Android
///   `FLAG_SECURE`; on iOS, `ios.blockScreenshotsOnEnterBackground` covers the
///   app switcher).
/// - **Tapjacking**: a touch that arrives through another app's overlay is
///   dropped ([TapjackingPolicy#BLOCK]), and overlay windows are hidden while
///   the app is in front where the OS allows it (Android 12+).
/// - **Clipboard**: an account number, address or authenticator key the user
///   copies is wiped from the clipboard after
///   [AppConfig#CLIPBOARD_CLEAR_MILLIS], as soon as the app goes to the
///   background, and at the next start if the app was killed first -- unless
///   something else was copied in the meantime. Codename One has no portable
///   way to mark a clip as sensitive (Android 13's `EXTRA_IS_SENSITIVE`, which
///   keeps it out of the clipboard preview), so the clip is ordinary until it
///   is wiped; recovery codes are therefore shown, never copied.
public final class Protect {
    private static boolean secure;
    private static String copied;

    private Protect() {
    }

    public static void install() {
        clearLeftover();
        DeviceIntegrity.setTapjackingProtection(TapjackingPolicy.BLOCK);
        if (DeviceIntegrity.isHideOverlayWindowsSupported()) {
            DeviceIntegrity.setHideOverlayWindows(true);
        }
        DeviceIntegrity.addTapjackingListener(evt ->
                ShieldSignals.add(ShieldSignal.TAPJACK, 2, "a touch through an overlay was blocked"));
    }

    /// Called as each form shows: `sensitive` forms block screenshots.
    public static void screen(boolean sensitive) {
        if (sensitive != secure) {
            secure = sensitive;
            DeviceIntegrity.setSecureScreen(sensitive);
        }
    }

    public static boolean isSecureScreen() {
        return secure;
    }

    /// Copies `text`, and clears it again later.
    public static void copy(final String text) {
        Display.getInstance().copyToClipboard(text);
        copied = text;
        // A digest, never the text: what a killed app left on the clipboard
        // is recognised at the next start without keeping it anywhere.
        Preferences.set(LEFT, digest(text));
        // CN.setTimeout, not a UITimer: a UITimer runs only while the form it
        // is bound to is showing, and the user moves on after copying.
        CN.setTimeout((int) AppConfig.CLIPBOARD_CLEAR_MILLIS, () -> {
            Object now = Display.getInstance().getPasteDataFromClipboard();
            // Only what this app put there: never wipe something the user
            // copied from elsewhere since.
            if (now instanceof String && now.equals(text) && text.equals(copied)) {
                clear();
            }
        });
    }

    /// The app went to the background: what it copied does not wait for
    /// the timer.
    public static void paused() {
        Object now = Display.getInstance().getPasteDataFromClipboard();
        if (copied != null && copied.equals(now)) {
            clear();
        }
    }

    /// At start: the clip a killed app could not clear, if it is still there.
    static void clearLeftover() {
        String left = Preferences.get(LEFT, "");
        if (left.length() == 0) {
            return;
        }
        Object now = Display.getInstance().getPasteDataFromClipboard();
        if (now instanceof String && left.equals(digest((String) now))) {
            Display.getInstance().copyToClipboard("");
        }
        Preferences.delete(LEFT);
    }

    private static void clear() {
        Display.getInstance().copyToClipboard("");
        copied = null;
        Preferences.delete(LEFT);
    }

    static String digest(String text) {
        try {
            return Base64.encodeUrlSafe(Hash.sha256(text.getBytes("UTF-8")));
        } catch (java.io.UnsupportedEncodingException impossible) {
            return "";
        }
    }

    private static final String LEFT = "bastion.clipboard.left";
}
