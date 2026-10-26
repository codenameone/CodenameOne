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
package com.codenameone.examples.bastion.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;

/// Draws a [QrCode]: black modules on white with the four-module quiet zone
/// scanners need, whatever the theme -- a QR code in dark-mode colours is
/// one many cameras cannot read.
public final class QrView extends Component {
    private static final int QUIET = 4;
    private final QrCode code;

    public QrView(QrCode code) {
        this.code = code;
        setUIID("BnQr");
    }

    @Override
    protected Dimension calcPreferredSize() {
        int side = Math.min(CN.convertToPixels(45), CN.getDisplayWidth() * 3 / 4);
        return new Dimension(side, side);
    }

    @Override
    public void paint(Graphics g) {
        int modules = code.size() + QUIET * 2;
        int side = Math.min(getWidth(), getHeight());
        int cell = Math.max(1, side / modules);
        int drawn = cell * modules;
        int left = getX() + (getWidth() - drawn) / 2;
        int top = getY() + (getHeight() - drawn) / 2;
        g.setColor(0xffffff);
        g.fillRect(left, top, drawn, drawn);
        g.setColor(0x000000);
        for (int y = 0; y < code.size(); y++) {
            for (int x = 0; x < code.size(); x++) {
                if (code.isDark(x, y)) {
                    g.fillRect(left + (x + QUIET) * cell, top + (y + QUIET) * cell, cell, cell);
                }
            }
        }
    }
}
