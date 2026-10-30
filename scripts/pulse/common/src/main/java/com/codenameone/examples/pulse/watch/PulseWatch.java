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
import com.codename1.ui.Form;
import com.codename1.ui.Label;
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
    private final Label title = new Label("", "SvWatchTitle");
    private final Label when = new Label("", "SvWatchText");
    private final Label where = new Label("", "SvWatchText");
    private final Label code = new Label("", "SvWatchCode");
    private byte[] secret;
    private int period = 30;
    private int digits = 8;

    @Override
    public void runApp() {
        form = new Form("Pulse", BoxLayout.y());
        form.setName("Watch");
        form.setScrollableY(true);
        title.setName("watchClass");
        code.setName("watchCode");
        form.add(title).add(when).add(where).add(new Label("Door code", "SvWatchText")).add(code);
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
                where.setText(data.getString("room", "") + " - " + data.getString("trainer", ""));
            }
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
        if (secret == null || secret.length == 0) {
            code.setText("Open Pulse on your phone");
            return;
        }
        long counter = System.currentTimeMillis() / 1000L / period;
        String now = Otp.hotp(secret, counter, digits <= 0 ? 8 : digits, Hash.SHA256);
        if (!now.equals(code.getText())) {
            code.setText(now);
            code.repaint();
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
