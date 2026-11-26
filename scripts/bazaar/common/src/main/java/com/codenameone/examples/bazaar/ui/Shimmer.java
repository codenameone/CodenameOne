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
package com.codenameone.examples.bazaar.ui;

import com.codename1.appkit.Skeleton;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.layouts.BorderLayout;

/// A loading [Skeleton] with a soft band of light sweeping across it, once
/// every 1.2 seconds, so a wait reads as work under way rather than a frozen
/// screen. The band is the surface colour at low alpha; it stops when the
/// placeholder leaves the screen.
public final class Shimmer extends Container {
    private static final long PERIOD = 1200;
    private long start;

    /// `rows` placeholder rows in style `uiid`, shimmering.
    public Shimmer(int rows, String uiid) {
        super(new BorderLayout());
        add(BorderLayout.CENTER, new Skeleton(rows, uiid));
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        start = System.currentTimeMillis();
        Form f = getComponentForm();
        if (f != null) {
            f.registerAnimated(this);
        }
    }

    @Override
    protected void deinitialize() {
        Form f = getComponentForm();
        if (f != null) {
            f.deregisterAnimated(this);
        }
        super.deinitialize();
    }

    @Override
    public boolean animate() {
        return true;
    }

    @Override
    protected void paintGlass(Graphics g) {
        int w = getWidth();
        if (w <= 0) {
            return;
        }
        long t = (System.currentTimeMillis() - start) % PERIOD;
        int band = Math.max(w / 4, 1);
        int x = getX() - band + (int) ((w + band * 2) * t / PERIOD);
        int old = g.getAlpha();
        int color = g.getColor();
        g.setColor(getUnselectedStyle().getBgColor() == 0 ? 0xffffff
                : getUnselectedStyle().getBgColor());
        // Three steps of alpha make the band soft at its edges.
        int step = band / 3;
        g.setAlpha(40);
        g.fillRect(x, getY(), band, getHeight());
        g.setAlpha(50);
        g.fillRect(x + step, getY(), band - step * 2, getHeight());
        g.setColor(color);
        g.setAlpha(old);
    }
}
