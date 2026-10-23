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
package com.codename1.impl.javase.record;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

/// Draws where the screen is being touched, the way a demo video needs to show it.
///
/// Touches arrive in screen coordinates from the port's pointer dispatch (mouse and
/// MCP alike), so whatever drives the simulator is visible. Each touch is kept for
/// a short fade after release. All access is synchronized: touches are added on
/// the AWT thread and painted from the recorder's capture thread as well.
public final class TouchOverlay {

    /// How a touch is drawn.
    public enum Style {
        /// Nothing is drawn.
        NONE,
        /// A filled translucent dot under the finger, like Android's "Show taps".
        DOT,
        /// A dot plus an expanding ring on press and release, like iOS screen
        /// recordings with touch indicators on.
        RIPPLE
    }

    private static final long FADE_MS = 350;
    private static final long RIPPLE_MS = 400;
    private static final int MAX_TRAIL = 48;

    private static final class Touch {
        final long down;
        long up = -1;
        final List<int[]> trail = new ArrayList<int[]>();
        int x;
        int y;

        Touch(int x, int y, long down) {
            this.x = x;
            this.y = y;
            this.down = down;
            trail.add(new int[]{x, y});
        }
    }

    private final List<Touch> touches = new ArrayList<Touch>();
    private Touch active;
    private Style style = Style.RIPPLE;
    private boolean showTrail = true;
    private Color color = new Color(128, 128, 128, 128);
    private Color outline = new Color(255, 255, 255, 200);
    private int radius = 22;
    private boolean liveVisible;

    public synchronized Style getStyle() {
        return style;
    }

    public synchronized void setStyle(Style style) {
        this.style = style == null ? Style.NONE : style;
    }

    public synchronized void setShowTrail(boolean showTrail) {
        this.showTrail = showTrail;
    }

    public synchronized boolean isShowTrail() {
        return showTrail;
    }

    /// Fill colour as 0xAARRGGBB.
    public synchronized void setColor(int argb) {
        this.color = new Color(argb, true);
    }

    public synchronized int getColor() {
        return color.getRGB();
    }

    /// Radius of the touch dot in screen pixels.
    public synchronized void setRadius(int radius) {
        this.radius = Math.max(2, radius);
    }

    public synchronized int getRadius() {
        return radius;
    }

    /// Whether the indicators are also drawn in the simulator window, not only into
    /// recordings. Useful when screen recording the window with another tool.
    public synchronized void setLiveVisible(boolean liveVisible) {
        this.liveVisible = liveVisible;
    }

    public synchronized boolean isLiveVisible() {
        return liveVisible;
    }

    public synchronized void pressed(int x, int y) {
        long now = System.currentTimeMillis();
        if (active != null && active.up < 0) {
            // The release went elsewhere (a popup menu took it); without this
            // the touch would stay down, and visible, forever.
            active.up = now;
        }
        active = new Touch(x, y, now);
        touches.add(active);
        prune(active.down);
    }

    public synchronized void dragged(int x, int y) {
        if (active == null) {
            return;
        }
        active.x = x;
        active.y = y;
        active.trail.add(new int[]{x, y});
        if (active.trail.size() > MAX_TRAIL) {
            active.trail.remove(0);
        }
    }

    public synchronized void released(int x, int y) {
        if (active == null) {
            return;
        }
        active.x = x;
        active.y = y;
        active.up = System.currentTimeMillis();
        active = null;
    }

    /// True while something is still drawn, so the caller keeps repainting.
    public synchronized boolean hasVisibleTouches() {
        if (style == Style.NONE) {
            return false;
        }
        long now = System.currentTimeMillis();
        for (Touch t : touches) {
            if (t.up < 0 || now - t.up < Math.max(FADE_MS, RIPPLE_MS)) {
                return true;
            }
        }
        return false;
    }

    private void prune(long now) {
        for (int i = touches.size() - 1; i >= 0; i--) {
            Touch t = touches.get(i);
            if (t.up >= 0 && now - t.up > Math.max(FADE_MS, RIPPLE_MS)) {
                touches.remove(i);
            }
        }
    }

    /// Paints the indicators. The graphics are in buffer pixels; `scale` converts a
    /// screen coordinate to one, and `offsetY` is added after scaling (the pan
    /// offset, in buffer pixels, when the screen is shifted up for the keyboard).
    public synchronized void paint(Graphics2D g, double scale, int offsetY) {
        if (style == Style.NONE || touches.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        prune(now);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float r = (float) (radius * scale);
        for (Touch t : touches) {
            float alpha = 1f;
            if (t.up >= 0) {
                alpha = Math.max(0f, 1f - (now - t.up) / (float) FADE_MS);
            }
            float cx = (float) (t.x * scale);
            float cy = (float) (t.y * scale) + offsetY;
            if (showTrail && t.trail.size() > 1 && alpha > 0) {
                g.setStroke(new BasicStroke(r * 0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(withAlpha(color, alpha * 0.45f));
                int[] prev = t.trail.get(0);
                for (int i = 1; i < t.trail.size(); i++) {
                    int[] p = t.trail.get(i);
                    g.drawLine((int) (prev[0] * scale), (int) (prev[1] * scale) + offsetY,
                            (int) (p[0] * scale), (int) (p[1] * scale) + offsetY);
                    prev = p;
                }
            }
            if (alpha > 0) {
                g.setColor(withAlpha(color, alpha));
                g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
                g.setStroke(new BasicStroke(Math.max(1f, r / 10f)));
                g.setColor(withAlpha(outline, alpha));
                g.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
            }
            if (style == Style.RIPPLE) {
                paintRipple(g, cx, cy, r, now - t.down);
                if (t.up >= 0) {
                    paintRipple(g, cx, cy, r, now - t.up);
                }
            }
        }
    }

    private void paintRipple(Graphics2D g, float cx, float cy, float r, long age) {
        if (age < 0 || age > RIPPLE_MS) {
            return;
        }
        float f = age / (float) RIPPLE_MS;
        float rr = r * (1f + f * 1.2f);
        g.setStroke(new BasicStroke(Math.max(1f, r / 6f)));
        g.setColor(withAlpha(color, (1f - f) * 0.8f));
        g.draw(new Ellipse2D.Float(cx - rr, cy - rr, rr * 2, rr * 2));
    }

    private static Color withAlpha(Color c, float factor) {
        int a = Math.max(0, Math.min(255, Math.round(c.getAlpha() * factor)));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }
}
