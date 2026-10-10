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
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

/// A countdown drawn as a ring that empties, with the seconds left inside it.
///
/// The colours are two styles' text colours: `WlRing` for the part of the
/// ring still left and `WlRingTrack` for the part used up. Neither has a
/// background, so whatever the ring sits on shows through its middle.
public final class Ring extends Component {
    private long endsAt;
    private int total = 1;
    private int shown = -1;

    public Ring() {
        setUIID("WlRing");
    }

    /// Starts counting down from `seconds`, of a count that began at `of`.
    public void start(int seconds, int of) {
        total = Math.max(1, Math.max(seconds, of));
        endsAt = System.currentTimeMillis() + seconds * 1000L;
        repaint();
    }

    /// The whole seconds left, never less than none.
    public int secondsLeft() {
        long left = endsAt - System.currentTimeMillis();
        return left <= 0 ? 0 : (int) ((left + 999) / 1000);
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

    /// Repaints while the count is running, which is what makes the ring
    /// shrink smoothly and not a second at a time.
    @Override
    public boolean animate() {
        boolean running = endsAt > System.currentTimeMillis() || shown != 0;
        shown = secondsLeft();
        return running;
    }

    @Override
    protected Dimension calcPreferredSize() {
        Style style = getStyle();
        int side = CN.convertToPixels(10.5f);
        return new Dimension(side + style.getHorizontalPadding(),
                side + style.getVerticalPadding());
    }

    @Override
    public void paint(Graphics g) {
        Style style = getStyle();
        int track = UIManager.getInstance().getComponentStyle("WlRingTrack").getFgColor();
        int side = Math.min(getWidth() - style.getHorizontalPadding(),
                getHeight() - style.getVerticalPadding());
        if (side <= 0) {
            return;
        }
        int centerX = getX() + getWidth() / 2;
        int centerY = getY() + getHeight() / 2;
        int stroke = Math.max(2, CN.convertToPixels(0.9f));
        int radius = (side - stroke) / 2;
        long left = Math.max(0L, endsAt - System.currentTimeMillis());
        double part = Math.min(1d, left / (total * 1000d));

        // The ring as dots around the circle, the first at the top and going
        // clockwise: enough of them that they join up.
        g.setAntiAliased(true);
        int dots = Math.max(60, (int) (2 * Math.PI * radius / Math.max(1, stroke / 2)));
        int lit = (int) Math.round(dots * part);
        for (int dot = 0; dot < dots; dot++) {
            double angle = 2 * Math.PI * dot / dots;
            int x = centerX + (int) Math.round(Math.sin(angle) * radius);
            int y = centerY - (int) Math.round(Math.cos(angle) * radius);
            g.setColor(dot < lit ? style.getFgColor() : track);
            g.fillArc(x - stroke / 2, y - stroke / 2, stroke, stroke, 0, 360);
        }

        String text = String.valueOf(secondsLeft());
        Font font = style.getFont();
        g.setFont(font);
        g.setColor(style.getFgColor());
        g.drawString(text, centerX - font.stringWidth(text) / 2, centerY - font.getHeight() / 2);
    }
}
