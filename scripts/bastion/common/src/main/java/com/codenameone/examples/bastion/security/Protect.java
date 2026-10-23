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
import com.codename1.security.Hmac;
import com.codename1.security.SecureRandom;
import com.codename1.security.TapjackingPolicy;
import com.codename1.security.shield.ShieldSignal;
import com.codename1.security.shield.ShieldSignals;
import com.codename1.ui.CN;
import com.codename1.ui.ClipboardContent;
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
///   [AppConfig#CLIPBOARD_CLEAR_MILLIS], whether or not the app is in the
///   foreground (the background is not a trigger: that would need a read),
///   and when the app next shows a screen if the clear could not run in time
///   -- unless something else was copied in the meantime. The clip is marked sensitive
///   ([ClipboardContent#setSensitive(boolean)]): Android 13 and newer keep it
///   out of the clipboard preview and the keyboard's suggestions, and iOS
///   keeps it off Universal Clipboard and lets the pasteboard expire it even
///   if the app is killed first.
public final class Protect {
    /// The leftover check's verdicts ([#decide]).
    static final int KEEP = 0;
    static final int CLEAR = 1;
    static final int FORGET = 2;

    private static final String LEFT = "bastion.clipboard.left";
    private static final String KEY_ACCOUNT = "bastion.clipboard.key";
    private static boolean secure;

    private Protect() {
    }

    public static void install() {
        DeviceIntegrity.setTapjackingProtection(TapjackingPolicy.BLOCK);
        if (DeviceIntegrity.isHideOverlayWindowsSupported()) {
            DeviceIntegrity.setHideOverlayWindows(true);
        }
        DeviceIntegrity.addTapjackingListener(evt ->
                ShieldSignals.add(ShieldSignal.TAPJACK, 2, "a touch through an overlay was blocked"));
        // Not now: runApp has not shown a form yet, and Android 10+ answers
        // a clipboard read with null until a window of ours has focus. The
        // first screen() call and every resume look again.
        CN.callSerially(Protect::checkLeftover);
    }

    /// Called as each form shows: `sensitive` forms block screenshots.
    public static void screen(boolean sensitive) {
        if (sensitive != secure) {
            secure = sensitive;
            DeviceIntegrity.setSecureScreen(sensitive);
        }
        if (Preferences.get(LEFT, "").length() > 0) {
            // After the show, so the window has focus by the time it reads.
            CN.callSerially(Protect::checkLeftover);
        }
    }

    public static boolean isSecureScreen() {
        return secure;
    }

    /// Copies `text`, and clears it again later.
    public static void copy(final String text) {
        Display.getInstance().copyToClipboard(new ClipboardContent()
                .setData(ClipboardContent.MIME_TEXT, text)
                .setSensitive(true)
                .setClearAfter((int) AppConfig.CLIPBOARD_CLEAR_MILLIS));
        if (isIos()) {
            // The pasteboard expires a sensitive clip itself, even in a
            // killed app; reading it back later would only show the paste
            // banner.
            return;
        }
        // A keyed digest, never the text: what was left on the clipboard is
        // recognised later without keeping it anywhere, and an account
        // number has too few possible values for a plain hash to hide it.
        byte[] key = digestKey();
        if (key == null) {
            return;
        }
        long due = System.currentTimeMillis() + AppConfig.CLIPBOARD_CLEAR_MILLIS;
        Preferences.set(LEFT, due + ":" + digest(key, text));
        // The content clears itself (setClearAfter): one mechanism writes the
        // clipboard when the time is up, never two racing each other. The
        // digest stays until a read says the clip is gone or was replaced,
        // because the port defers a clear that falls due in the background
        // and a process killed meanwhile never runs it. This timer only
        // looks; CN.setTimeout, not a UITimer, which runs only while its
        // form is showing.
        CN.setTimeout((int) AppConfig.CLIPBOARD_CLEAR_MILLIS + 1000, Protect::checkLeftover);
    }

    /// The app came back to the foreground.
    public static void resumed() {
        CN.callSerially(Protect::checkLeftover);
    }

    /// The app went to the background. Nothing is read or written here:
    /// reading the clipboard to see whether our copy is still on it shows the
    /// iOS paste banner and reports a read to Android 12+, and writing without
    /// reading could wipe what the user copied in another app since. The
    /// clip's own clear (setClearAfter, which clears only while nothing newer
    /// was copied, and the pasteboard expiry on iOS) keeps running, and the
    /// digest left by copy() lets the next look clear it if the app is
    /// killed first.
    public static void paused() {
        // Deliberately empty; kept so the lifecycle call site documents it.
    }

    /// A copy whose clear may not have run: cleared if it is still there.
    static void checkLeftover() {
        String left = Preferences.get(LEFT, "");
        if (left.length() == 0) {
            return;
        }
        if (isIos()) {
            Preferences.delete(LEFT);
            return;
        }
        if (!due(left, System.currentTimeMillis())) {
            return;
        }
        byte[] key = digestKey();
        String now = clipText();
        int verdict = key == null ? FORGET : decide(left, now == null ? null : digest(key, now));
        if (verdict == CLEAR) {
            Display.getInstance().copyToClipboard("");
        }
        if (verdict != KEEP) {
            Preferences.delete(LEFT);
        }
    }

    /// Whether the clear recorded in `left` has fallen due by `now`.
    static boolean due(String left, long now) {
        int colon = left.indexOf(':');
        if (colon <= 0) {
            return true;
        }
        try {
            return now >= Long.parseLong(left.substring(0, colon));
        } catch (NumberFormatException bad) {
            return true;
        }
    }

    /// What to do with the record `left` given the digest of the clipboard
    /// now (null when the read answered nothing, which on Android 10+ means
    /// "not allowed yet", not "empty"): keep it for the next look, clear the
    /// clipboard, or forget it because something else was copied since.
    static int decide(String left, String nowDigest) {
        if (nowDigest == null) {
            return KEEP;
        }
        String stored = left.substring(left.indexOf(':') + 1);
        return stored.equals(nowDigest) ? CLEAR : FORGET;
    }

    /// The text on the clipboard, whichever form the port answers it in.
    private static String clipText() {
        Object now = Display.getInstance().getPasteDataFromClipboard();
        if (now instanceof ClipboardContent) {
            return ((ClipboardContent) now).getText(ClipboardContent.MIME_TEXT);
        }
        return now instanceof String ? (String) now : null;
    }

    /// The HMAC key, made once and kept in secure storage, never in
    /// Preferences beside the digest; null if secure storage refuses it.
    static byte[] digestKey() {
        String stored = Keys.storage().get(KEY_ACCOUNT);
        if (stored != null && stored.length() > 0) {
            return Base64.decode(stored.getBytes());
        }
        byte[] made = SecureRandom.bytes(32);
        return Keys.storage().set(KEY_ACCOUNT, Base64.encodeNoNewline(made)) ? made : null;
    }

    static String digest(byte[] key, String text) {
        try {
            return Base64.encodeUrlSafe(Hmac.sha256(key, text.getBytes("UTF-8")));
        } catch (java.io.UnsupportedEncodingException impossible) {
            return "";
        }
    }

    private static boolean isIos() {
        return "ios".equals(CN.getPlatformName());
    }
}
