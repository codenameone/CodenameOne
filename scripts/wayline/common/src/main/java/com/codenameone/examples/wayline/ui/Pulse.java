/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;

/// "Still looking": a dot that sends out rings, the way a search does on a
/// map. It runs for as long as it is on screen.
///
/// The colour is the `WlPulse` style's text colour.
public final class Pulse extends Component {
    /// How long one ring takes from the dot to the edge.
    private static final int PERIOD_MILLIS = 1800;
    private static final int RINGS = 3;

    public Pulse() {
        setUIID("WlPulse");
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
