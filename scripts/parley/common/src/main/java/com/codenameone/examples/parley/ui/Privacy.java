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
package com.codenameone.examples.parley.ui;

import com.codename1.security.Biometrics;
import com.codename1.security.DeviceIntegrity;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.Painter;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codenameone.examples.parley.Prefs;
import com.codenameone.examples.parley.net.Session;

/// The two privacy features that are about the phone rather than the
/// network: screen security and the app lock.
///
/// - **Screen security** keeps chats out of screenshots, screen recordings
///   and the picture the app switcher shows: Android's `FLAG_SECURE` on every
///   screen, and on every platform a cover painted over the app as it goes to
///   the background.
/// - **App lock** asks for the fingerprint, the face or the device passcode
///   when the app comes back after a minute away, before anything is shown.
public final class Privacy {
    /// How long the app may be away before the lock asks again.
    private static final long GRACE_MILLIS = 60000;

    private static long pausedAt;
    private static boolean locked;

    private Privacy() {
    }

    /// Called for every screen as it is shown: the flag is per window on
    /// Android, and a screen shown later must carry it too.
    public static void shown() {
        DeviceIntegrity.setSecureScreen(Prefs.screenSecurity());
    }

    /// The app is going to the background.
    public static void paused() {
        pausedAt = System.currentTimeMillis();
        if (Prefs.screenSecurity() || Prefs.appLock()) {
            Form current = CN.getCurrentForm();
            if (current != null) {
                current.setGlassPane(new Cover());
                current.repaint();
            }
        }
    }

    /// The app is back.
    public static void resumed() {
        Form current = CN.getCurrentForm();
        boolean away = pausedAt == 0 || System.currentTimeMillis() - pausedAt > GRACE_MILLIS;
        if (Prefs.appLock() && Session.user() != null && away && !locked
                && Biometrics.getInstance().canAuthenticate()) {
            lock(current);
            return;
        }
        if (current != null && current.getGlassPane() instanceof Cover) {
            current.setGlassPane(null);
            current.repaint();
        }
        shown();
    }

    private static void lock(final Form behind) {
        locked = true;
        final Form form = new Form("", new BorderLayout());
        form.setUIID("PlLock");
        form.getToolbar().hideToolbar();
        form.setName("Locked");
        Label icon = new Label("", "PlLockTitle");
        FontImage.setMaterialIcon(icon, FontImage.MATERIAL_LOCK, 14);
        Container middle = BoxLayout.encloseY(icon, new Label("Parley is locked", "PlLockTitle"),
                Ui.secondary("Unlock", "unlock", e -> ask(form, behind)));
        form.add(BorderLayout.CENTER, BorderLayout.centerAbsolute(middle));
        form.show();
        ask(form, behind);
    }

    private static void ask(final Form lock, final Form behind) {
        Biometrics.getInstance().authenticate("Unlock Parley").ready(ok -> {
            if (Boolean.TRUE.equals(ok)) {
                locked = false;
                if (behind != null) {
                    if (behind.getGlassPane() instanceof Cover) {
                        behind.setGlassPane(null);
                    }
                    behind.showBack();
                } else {
                    Nav.home();
                }
                shown();
            }
        }).except(failed -> {
            // Stays on the lock screen; the button asks again.
        });
    }

    /// What the app switcher sees: the brand colour and nothing else.
    private static final class Cover implements Painter {
        @Override
        public void paint(Graphics g, Rectangle rect) {
            g.setColor(UIManager.getInstance().getComponentStyle("PlLock").getBgColor());
            g.setAlpha(255);
            g.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
        }
    }
}
