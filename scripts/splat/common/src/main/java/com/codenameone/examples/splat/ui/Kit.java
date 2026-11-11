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
package com.codenameone.examples.splat.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Label;

/// Small pieces of motion the menus share.
final class Kit {
    private Kit() {
    }

    /// Counts a label up from 0 to `to` over a second, with `prefix` before.
    public static Label countUp(final String prefix, final int to, String uiid) {
        final Label l = new Label(prefix + to, uiid);
        final long start = System.currentTimeMillis();
        // Sized for the final number, so counting never truncates or jumps.
        l.setPreferredSize(l.getPreferredSize());
        l.setText(prefix + "0");
        CN.callSerially(new Runnable() {
            public void run() {
                if (l.getComponentForm() == null) {
                    l.setText(prefix + to);
                    return;
                }
                float k = Math.min(1f, (System.currentTimeMillis() - start) / 1100f);
                k = 1f - (1f - k) * (1f - k);
                l.setText(prefix + (int) (to * k));
                l.repaint();
                if (k < 1f) {
                    com.codename1.ui.util.UITimer.timer(30, false, l.getComponentForm(), this);
                }
            }
        });
        return l;
    }
}
