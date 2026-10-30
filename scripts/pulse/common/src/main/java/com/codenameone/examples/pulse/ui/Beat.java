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
package com.codenameone.examples.pulse.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.plaf.Style;

/// A heartbeat trace drawn across the component, the app's signature: flat,
/// a spike, flat again. It is drawn, not an image, so it is sharp at any
/// density and takes its colour from the style's foreground. When `running`
/// the spike travels along the line, the way a monitor's trace moves.
public final class Beat extends Component {
    private final boolean running;
    private long started;

    /// @param uiid the style whose foreground colour the trace takes
    public Beat(String uiid, boolean running) {
        setUIID(uiid);
        this.running = running;
        setFocusable(false);
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        started = System.currentTimeMillis();
        if (running && getComponentForm() != null) {
            getComponentForm().registerAnimated(this);
        }
    }

    @Override
    protected void deinitialize() {
        if (getComponentForm() != null) {
            getComponentForm().deregisterAnimated(this);
        }
        super.deinitialize();
    }

    @Override
    public boolean animate() {
        return running;
    }

    @Override
    protected Dimension calcPreferredSize() {
        return new Dimension(CN.convertToPixels(40), CN.convertToPixels(9));
    }

    @Override
    public void paint(Graphics g) {
        Style style = getStyle();
        int left = getX() + style.getPaddingLeftNoRTL();
        int width = getWidth() - style.getHorizontalPadding();
        int top = getY() + style.getPaddingTop();
        int height = getHeight() - style.getVerticalPadding();
        if (width <= 0 || height <= 0) {
            return;
        }
        int mid = top + height / 2;
        // Where the beat is, as a share of the width: one sweep every three
        // seconds when running, a third of the way along when still.
        double at = running ? ((System.currentTimeMillis() - started) % 3000L) / 3000d : 0.32d;
        int x0 = left + (int) (width * at);
        int unit = Math.max(2, height / 6);
        int[] xs = {left, x0, x0 + unit, x0 + 2 * unit, x0 + 3 * unit, x0 + 4 * unit,
            x0 + 5 * unit, x0 + 6 * unit, left + width};
        int[] ys = {mid, mid, mid - height / 6, mid + height / 5, mid - height / 2 + 1,
            mid + height / 2 - 1, mid - height / 8, mid, mid};
        g.setAntiAliased(true);
        g.setColor(style.getFgColor());
        g.setAlpha(255);
        float thickness = Math.max(2f, CN.convertToPixels(0.6f));
        if (g.isShapeSupported()) {
            GeneralPath path = new GeneralPath();
            path.moveTo(xs[0], ys[0]);
            for (int iter = 1; iter < xs.length; iter++) {
                path.lineTo(Math.min(xs[iter], left + width), ys[iter]);
            }
            g.drawShape(path, new Stroke(thickness, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 1f));
        } else {
            for (int iter = 1; iter < xs.length; iter++) {
                g.drawLine(xs[iter - 1], ys[iter - 1], Math.min(xs[iter], left + width),
                        ys[iter]);
            }
        }
    }
}
