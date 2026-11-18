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
package com.codenameone.examples.brickyard.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;

/// A rounded progress bar: XP to the next level, a quest, a season tier.
public class Meter extends Component {
    private double value;
    private final int color;

    public Meter(double value, int color) {
        this.value = value;
        this.color = color;
        setUIID("Container");
        getAllStyles().setBgTransparency(0);
        getAllStyles().setMarginUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_DIPS);
        getAllStyles().setMargin(0.8f, 0.8f, 0, 0);
    }

    public void setValue(double v) {
        value = v;
        repaint();
    }

    /// The fraction drawn, clamped to 0..1.
    static double clamp(double v) {
        return v < 0 || v != v ? 0 : Math.min(1, v);
    }

    @Override
    public void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        int x = getX();
        int y = getY();
        g.setAntiAliased(true);
        g.setColor(0x0e0f11);
        g.fillRoundRect(x, y, w, h, h, h);
        int fill = (int) (w * clamp(value));
        if (fill > 0) {
            g.setColor(color);
            g.fillRoundRect(x, y, Math.max(h, fill), h, h, h);
            // a lighter top half reads as a glossy bar
            g.setColor(0xffffff);
            g.setAlpha(50);
            g.fillRoundRect(x + h / 4, y + 1, Math.max(h / 2, fill - h / 2), Math.max(1, h / 3), h / 3, h / 3);
            g.setAlpha(255);
        }
    }

    @Override
    protected Dimension calcPreferredSize() {
        return new Dimension(CN.convertToPixels(20), CN.convertToPixels(1.6f));
    }
}
