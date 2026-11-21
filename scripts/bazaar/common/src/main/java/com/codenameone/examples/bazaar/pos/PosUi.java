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
package com.codenameone.examples.bazaar.pos;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.Ui;

/// Pieces every register screen shares: the header with the sync pill, big
/// touch keys and a number pad.
final class PosUi {
    private PosUi() {
    }

    /// Whether the register is laid out side by side (tablet, desktop).
    static boolean wide() {
        return Layouts.wide();
    }

    static Label label(String text, String uiid) {
        Label l = new Label(text, uiid);
        return l;
    }

    /// A label that shows data, never translated.
    static Label data(String text, String uiid) {
        Label l = new Label(text == null ? "" : text, uiid);
        l.setShouldLocalize(false);
        return l;
    }

    static Button key(String text, String name, String uiid, ActionListener<ActionEvent> click) {
        Button b = new Button(text, uiid);
        b.setName(name);
        if (click != null) {
            b.addActionListener(click);
        }
        return b;
    }

    static Button icon(char icon, String text, String name, String uiid,
            ActionListener<ActionEvent> click) {
        Button b = key(text, name, uiid, click);
        FontImage.setMaterialIcon(b, icon, 3.6f);
        return b;
    }

    /// The sync status: Online, Offline with what is waiting, or Syncing.
    /// Tapping it opens the outbox.
    static Button pill() {
        final Button pill = new Button("", "PosPill");
        pill.setName("pos-sync-pill");
        pill.setShouldLocalize(false);
        final Runnable update = () -> {
            int pending = PosState.hasRegister() ? PosState.outbox().pending() : 0;
            int errors = PosState.hasRegister() ? PosState.outbox().errors() : 0;
            String status = PosSync.status();
            String text;
            String uiid;
            char icon;
            if (PosSync.SYNCING.equals(status)) {
                text = Lang.tr("Syncing");
                uiid = "PosPillSyncing";
                icon = FontImage.MATERIAL_SYNC;
            } else if (PosSync.OFFLINE.equals(status)) {
                text = pending > 0 ? Lang.tr("Offline, {0} pending", pending) : Lang.tr("Offline");
                uiid = "PosPillOffline";
                icon = FontImage.MATERIAL_CLOUD_OFF;
            } else if (pending > 0) {
                text = Lang.tr("{0} pending", pending);
                uiid = "PosPillSyncing";
                icon = FontImage.MATERIAL_CLOUD_UPLOAD;
            } else {
                text = Lang.tr("Online");
                uiid = "PosPill";
                icon = FontImage.MATERIAL_CLOUD_DONE;
            }
            int approvals = PosState.hasRegister() ? PosState.outbox().awaitingApproval() : 0;
            if (approvals > 0) {
                text = text + Ui.DOT + Lang.tr("{0} need approval", approvals);
                uiid = "PosPillOffline";
            } else if (errors > 0) {
                text = text + "  !" + errors;
                uiid = "PosPillOffline";
            }
            pill.setText(text);
            pill.setUIID(uiid);
            FontImage.setMaterialIcon(pill, icon, 3f);
            Container parent = pill.getParent();
            if (parent != null) {
                parent.revalidateLater();
            }
        };
        update.run();
        Runnable listening = live(pill, update, false);
        pill.addActionListener(e -> PosSections.show(PosSections.OUTBOX));
        // Listeners die with the pill: a screen is rebuilt often.
        pill.putClientProperty("posUpdate", listening);
        return pill;
    }

    /// Wraps `r` so it stops listening by itself once `owner` has been on
    /// screen and is no longer: screens are dropped without being told.
    static Runnable live(final Component owner, final Runnable r, final boolean cart) {
        final boolean[] seen = {false};
        final Runnable[] self = new Runnable[1];
        self[0] = () -> {
            if (owner.getComponentForm() != null) {
                seen[0] = true;
            } else if (seen[0]) {
                if (cart) {
                    PosState.offCart(self[0]);
                } else {
                    PosState.offStatus(self[0]);
                }
                return;
            }
            r.run();
        };
        if (cart) {
            PosState.onCart(self[0]);
        } else {
            PosState.onStatus(self[0]);
        }
        return self[0];
    }

    /// Stops `c`'s pills listening; call when a screen is replaced.
    static void detach(Component c) {
        if (c == null) {
            return;
        }
        Object update = c.getClientProperty("posUpdate");
        if (update instanceof Runnable) {
            PosState.offStatus((Runnable) update);
        }
        if (c instanceof Container) {
            Container k = (Container) c;
            for (int iter = 0; iter < k.getComponentCount(); iter++) {
                detach(k.getComponentAt(iter));
            }
        }
    }

    /// A header: a title on the left, the pill (and `extra`) on the right.
    static Container header(String title, Component extra) {
        Container bar = new Container(new BorderLayout());
        bar.setUIID("PosHeader");
        Label t = label(title, "PosTitle");
        bar.add(BorderLayout.CENTER, t);
        Container right = new Container(BoxLayout.x());
        if (extra != null) {
            right.add(extra);
        }
        right.add(pill());
        bar.add(BorderLayout.EAST, right);
        return bar;
    }

    /// Takes digits; what [#pad] sends.
    interface Keys {
        void key(char c);
    }

    /// A 3 by 4 pad: 1-9, then `left` (sent as a newline), 0 and backspace.
    static Container pad(String name, String left, final Keys keys) {
        Container pad = new Container(new GridLayout(4, 3));
        pad.setUIID("PosPad");
        String[] labels = {"1", "2", "3", "4", "5", "6", "7", "8", "9", left, "0", ""};
        for (int iter = 0; iter < labels.length; iter++) {
            final String l = labels[iter];
            final boolean back = iter == 11;
            final boolean left9 = iter == 9;
            Button b = new Button(l, "PosKey");
            b.setShouldLocalize(false);
            b.setName(name + "-" + (back ? "back" : l.length() == 0 ? "blank" : l));
            if (back) {
                FontImage.setMaterialIcon(b, FontImage.MATERIAL_BACKSPACE, 4f);
            }
            if (l.length() == 0 && !back) {
                b.setEnabled(false);
            }
            b.addActionListener(e -> keys.key(back ? '\b' : left9 ? '\n' : l.charAt(0)));
            pad.add(b);
        }
        return pad;
    }

    /// Cents typed digit by digit, the way a till takes them: 1, 2, 5 is
    /// 1.25. `\b` takes the last digit off.
    static long typed(long cents, char c) {
        if (c == '\b') {
            return cents / 10;
        }
        if (c == '.' || c < '0' || c > '9') {
            return cents;
        }
        long next = cents * 10 + (c - '0');
        return next > 99999999L ? cents : next;
    }

    /// Whether a keyboard-wedge field should hold the focus: only where
    /// there is no on-screen keyboard to keep popping up.
    static boolean holdFocus() {
        return CN.isDesktop();
    }
}
