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

import com.codename1.security.DeviceIntegrity;
import com.codename1.security.TapjackingPolicy;
import com.codename1.security.shield.ShieldSignal;
import com.codename1.security.shield.ShieldSignals;
import com.codename1.ui.Display;
import com.codename1.ui.util.UITimer;
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
/// - **Clipboard**: an account number or address the user copies is wiped
///   from the clipboard after [AppConfig#CLIPBOARD_CLEAR_MILLIS], unless
///   something else was copied in the meantime.
public final class Protect {
    private static boolean secure;
    private static String copied;

    private Protect() {
    }

    public static void install() {
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
        UITimer.timer((int) AppConfig.CLIPBOARD_CLEAR_MILLIS, false, () -> {
            Object now = Display.getInstance().getPasteDataFromClipboard();
            // Only what this app put there: never wipe something the user
            // copied from elsewhere since.
            if (now instanceof String && now.equals(text) && text.equals(copied)) {
                Display.getInstance().copyToClipboard("");
                copied = null;
            }
        });
    }
}
