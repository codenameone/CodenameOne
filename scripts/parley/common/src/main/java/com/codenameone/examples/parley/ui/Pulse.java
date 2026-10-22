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

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;

/// "Still looking": a dot that sends out rings, the way a search does on a
/// map. It runs for as long as it is on screen.
///
/// The colour is the `PlPulse` style's text colour.
public final class Pulse extends Component {
    /// How long one ring takes from the dot to the edge.
    private static final int PERIOD_MILLIS = 1800;
    private static final int RINGS = 3;

    public Pulse() {
        setUIID("PlPulse");
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        Form form = getComponentForm();
        if (form != null) {
            form.registerAnimated(this);
        }
    }

    @Override
    protected void deinitialize() {
        Form form = getComponentForm();
        if (form != null) {
            form.deregisterAnimated(this);
        }
        super.deinitialize();
    }

    @Override
    public boolean animate() {
        return true;
    }

    @Override
    protected Dimension calcPreferredSize() {
        Style style = getStyle();
        int side = CN.convertToPixels(13f);
        return new Dimension(side + style.getHorizontalPadding(),
                side + style.getVerticalPadding());
    }

    @Override
    public void paint(Graphics g) {
        Style style = getStyle();
        int side = Math.min(getWidth() - style.getHorizontalPadding(),
                getHeight() - style.getVerticalPadding());
        if (side <= 0) {
            return;
        }
        int centerX = getX() + getWidth() / 2;
        int centerY = getY() + getHeight() / 2;
        int dot = Math.max(4, CN.convertToPixels(2f));
        int alpha = g.getAlpha();
        g.setAntiAliased(true);
        g.setColor(style.getFgColor());
        long now = System.currentTimeMillis() % PERIOD_MILLIS;
        for (int ring = 0; ring < RINGS; ring++) {
            // Each ring is a third of the way behind the one before it, and
            // fades as it grows.
            double part = ((now + (long) ring * PERIOD_MILLIS / RINGS) % PERIOD_MILLIS)
                    / (double) PERIOD_MILLIS;
            int size = dot + (int) ((side - dot) * part);
            g.setAlpha((int) (90 * (1d - part)));
            g.fillArc(centerX - size / 2, centerY - size / 2, size, size, 0, 360);
        }
        g.setAlpha(alpha);
        g.fillArc(centerX - dot / 2, centerY - dot / 2, dot, dot, 0, 360);
    }
}
