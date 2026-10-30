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
package com.codenameone.examples.pulse.watch;

import com.codename1.l10n.SimpleDateFormat;
import com.codename1.security.Hash;
import com.codename1.security.Otp;
import com.codename1.system.Lifecycle;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;
import com.codename1.wearable.WearableConnection;
import com.codename1.wearable.WearableDataListener;
import com.codename1.wearable.WearableMessage;
import java.util.Date;

/// The watch app: the next class and the door code, on the wrist.
///
/// This is the class `codename1.watchMain` names, so the watch build starts
/// here and carries only what it reaches -- not the phone's screens or its
/// REST clients. It talks to no server: everything it shows is what
/// [WatchBridge] replicated from the phone, read when the app starts and
/// again whenever the phone changes it. The code is worked out on the watch
/// every period, the same way the phone's pass does it.
public class PulseWatch extends Lifecycle {
    private Form form;
    // Made in runApp, not here: the lifecycle is constructed before the
    // display exists, and a component needs its theme.
    private Label title;
    private Label when;
    private Label where;
    private Label code;
    private Label stats;
    private Label streakLine;
    private Countdown left;
    private byte[] secret;
    private int period = 30;
    private int digits = 8;

    @Override
    public void runApp() {
        title = new Label("", "SvWatchTitle");
        when = new Label("", "SvWatchText");
        where = new Label("", "SvWatchText");
        code = new Label("", "SvWatchCode");
        stats = new Label("", "SvWatchStat");
        streakLine = new Label("", "SvWatchStreak");
        left = new Countdown();
        form = new Form("Pulse", BoxLayout.y());
        form.setUIID("SvWatchForm");
        form.getContentPane().setUIID("SvWatchForm");
        // A watch face has no room for a title bar; the eyebrows say it all.
        form.getToolbar().setHidden(true);
        form.setName("Watch");
        form.setScrollableY(true);
        form.getContentPane().setScrollVisible(false);
        title.setName("watchClass");
        code.setName("watchCode");
        stats.setName("watchStats");
        form.add(new Label("NEXT CLASS", "SvWatchEyebrow")).add(title).add(when).add(where);
        Container pass = new Container(BoxLayout.y());
        pass.setUIID("SvWatchCard");
        pass.add(new Label("DOOR CODE", "SvWatchEyebrow")).add(code).add(left);
        form.add(pass).add(stats).add(streakLine);
        WearableConnection.addDataListener(new WearableDataListener() {
            @Override
            public void dataChanged(WearableMessage data) {
                show(data);
            }

            @Override
            public void dataRemoved(String path) {
                show(new WearableMessage(path));
            }
        });
        show(stored(WatchBridge.NEXT));
        show(stored(WatchBridge.PASS));
        show(stored(WatchBridge.STATS));
        UITimer.timer(1000, true, form, this::tick);
        form.show();
    }

    private static WearableMessage stored(String path) {
        WearableMessage found = WearableConnection.getData(path);
        return found == null ? new WearableMessage(path) : found;
    }

    void show(WearableMessage data) {
        if (WatchBridge.NEXT.equals(data.getPath())) {
            long startsAt = data.getLong("startsAt", 0L);
            if (startsAt == 0L) {
                title.setText("No class booked");
                when.setText("");
                where.setText("");
            } else {
                title.setText(data.getString("name", ""));
                when.setText(new SimpleDateFormat("EEE HH:mm").format(new Date(startsAt)));
                // The phone sends "location, room"; the room is what a
                // member walks to, and the wrist has no room for both.
                String room = data.getString("room", "");
                int comma = room.lastIndexOf(", ");
                where.setText((comma < 0 ? room : room.substring(comma + 2)) + " \u00b7 "
                        + data.getString("trainer", ""));
            }
        } else if (WatchBridge.STATS.equals(data.getPath())) {
            int points = data.getInt("points", 0);
            int streak = data.getInt("streak", 0);
            stats.setText(points == 0 ? ""
                    : points + " pts \u00b7 Level " + data.getInt("level", 1));
            streakLine.setText(streak > 0 ? streak + " week streak" : "");
        } else if (WatchBridge.PASS.equals(data.getPath())) {
            secret = hex(data.getString("secret", ""));
            period = Math.max(1, data.getInt("period", 30));
            digits = data.getInt("digits", 8);
            tick();
        }
        if (form != null) {
            form.revalidate();
        }
    }

    private void tick() {
        if (code == null) {
            return;
        }
        if (secret == null || secret.length == 0) {
            code.setText("Open Pulse on your phone");
            return;
        }
        long seconds = System.currentTimeMillis() / 1000L;
        long counter = seconds / period;
        left.set(1f - (float) (seconds % period) / period);
        String now = Otp.hotp(secret, counter, digits <= 0 ? 8 : digits, Hash.SHA256);
        if (now.length() > 5) {
            // Grouped in two, as a door keypad code is read out.
            now = now.substring(0, now.length() / 2) + " " + now.substring(now.length() / 2);
        }
        if (!now.equals(code.getText())) {
            code.setText(now);
            code.repaint();
        }
    }

    /// A thin bar that empties as the code's period runs out.
    static final class Countdown extends Component {
        private float fraction = 1f;

        Countdown() {
            setUIID("SvWatchBar");
        }

        void set(float fraction) {
            if (fraction != this.fraction) {
                this.fraction = fraction;
                repaint();
            }
        }

        @Override
        public void paint(Graphics g) {
            int h = getHeight() - getStyle().getVerticalPadding();
            int y = getY() + getStyle().getPaddingTop();
            int x = getX() + getStyle().getPaddingLeftNoRTL();
            int w = getWidth() - getStyle().getHorizontalPadding();
            g.setAntiAliased(true);
            g.setColor(0x2a2a30);
            g.fillRoundRect(x, y, w, h, h, h);
            g.setColor(fraction < 0.2f ? 0xff6b4a : 0xc6f432);
            g.fillRoundRect(x, y, Math.max(h, (int) (w * fraction)), h, h, h);
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(CN.convertToPixels(20),
                    CN.convertToPixels(1.2f) + getStyle().getVerticalPadding());
        }
    }

    static byte[] hex(String text) {
        if (text == null) {
            return new byte[0];
        }
        byte[] out = new byte[text.length() / 2];
        for (int iter = 0; iter < out.length; iter++) {
            out[iter] = (byte) ((nibble(text.charAt(iter * 2)) << 4)
                    | nibble(text.charAt(iter * 2 + 1)));
        }
        return out;
    }

    private static int nibble(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return 0;
    }
}
