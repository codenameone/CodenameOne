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
package com.codename1.desktopcompat.org.jdesktop.swingx;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.javax.swing.Icon;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.Timer;
import com.codename1.desktopcompat.org.jdesktop.swingx.painter.BusyPainter;
import com.codename1.desktopcompat.rt.Fonts;
import com.codename1.desktopcompat.rt.MiniHtml;
import com.codename1.desktopcompat.rt.Units;

/// A label with a "busy" wheel where its icon would be, turning while the
/// label is busy.
///
/// The wheel is drawn by a [BusyPainter] over the place of an empty icon
/// of the wheel's size, so the label's text sits beside it as beside any
/// icon. While the label is busy and shown, a Swing timer advances the
/// painter one point per tick and repaints; an idle label shows the wheel
/// at rest, every point in the base color.
///
/// ## What differs from SwingX
///
///  - Calling `setIcon` with an icon of the application's replaces the
///    wheel for good.
///  - The wheel is placed for an icon beside the text; with the text set
///    to be above or below the icon it is drawn where a leading icon
///    would be.
public class JXBusyLabel extends JLabel {

    private BusyPainter busyPainter;
    private Timer busy;
    private int delay = 100;
    private boolean busyState;
    private final Dimension size;
    private Icon space;
    private int frame = -1;
    private BusyPainter.Direction direction;

    /// A wheel of 26 by 26 pixels.
    public JXBusyLabel() {
        this(null);
    }

    /// A wheel of the given size; `null` is 26 by 26.
    public JXBusyLabel(Dimension dim) {
        super();
        size = dim == null ? new Dimension(26, 26) : new Dimension(dim.width, dim.height);
        space = new Space(size.width, size.height);
        setIcon(space);
    }

    /// An icon that reserves the wheel's place and draws nothing.
    private static final class Space implements Icon {
        private final int width;
        private final int height;

        Space(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }
    }

    /// Sets the way the wheel turns.
    public void setDirection(BusyPainter.Direction dir) {
        direction = dir;
        getBusyPainter().setDirection(dir);
        repaint();
    }

    /// Makes the painter for a wheel of the given size, unless there is
    /// one already.
    protected void initPainter(Dimension dim) {
        if (busyPainter == null) {
            busyPainter = createBusyPainter(dim);
            adopt(busyPainter, direction);
        }
    }

    /// Brings a painter to the frame the label is at.
    private void adopt(BusyPainter p, BusyPainter.Direction dir) {
        if (p != null) {
            p.setFrame(frame);
            if (dir != null) {
                p.setDirection(dir);
            }
        }
    }

    /// The painter a new label draws its wheel with: the default wheel,
    /// scaled to the height of `dim`.
    protected BusyPainter createBusyPainter(Dimension dim) {
        BusyPainter p = dim == null ? new BusyPainter() : new BusyPainter(Math.max(1, dim.height));
        p.setPaintCentered(true);
        return p;
    }

    public boolean isBusy() {
        return busyState;
    }

    /// Starts or stops the wheel. A busy label that is not in a shown
    /// window starts turning when it gets into one.
    public void setBusy(boolean busy) {
        boolean old = busyState;
        busyState = busy;
        if (old != busy) {
            if (busy) {
                startAnimation();
            } else {
                stopAnimation();
            }
            firePropertyChange("busy", old, busy);
            repaint();
        }
    }

    private void startAnimation() {
        if (busy != null || !isDisplayable()) {
            return;
        }
        busy = new Timer(delay, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cn1Tick();
            }
        });
        busy.start();
    }

    private void stopAnimation() {
        if (busy != null) {
            busy.stop();
            busy = null;
        }
        frame = -1;
        if (busyPainter != null) {
            busyPainter.setFrame(-1);
        }
    }

    /// One step of the animation, as the timer takes it: the highlight
    /// moves on by one point. Does nothing while the label is not busy.
    public void cn1Tick() {
        if (!busyState) {
            return;
        }
        BusyPainter p = getBusyPainter();
        if (p == null) {
            return;
        }
        int points = p.getPoints();
        frame = points > 0 ? (frame + 1) % points : 0;
        p.setFrame(frame);
        frameChanged();
    }

    /// Whether the timer that turns the wheel is running.
    public boolean cn1Animating() {
        return busy != null && busy.isRunning();
    }

    @Override
    public void removeNotify() {
        if (busy != null) {
            busy.stop();
            busy = null;
        }
        super.removeNotify();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (busyState) {
            startAnimation();
        }
    }

    /// Called after every step of the animation; repaints the label.
    protected void frameChanged() {
        repaint();
    }

    /// The painter of the wheel.
    public final BusyPainter getBusyPainter() {
        if (busyPainter == null) {
            initPainter(size);
        }
        return busyPainter;
    }

    public final void setBusyPainter(BusyPainter busyPainter) {
        BusyPainter old = this.busyPainter;
        this.busyPainter = busyPainter;
        adopt(busyPainter, null);
        firePropertyChange("busyPainter", old, busyPainter);
        repaint();
    }

    public int getDelay() {
        return delay;
    }

    /// Sets the time between two steps, in milliseconds.
    public void setDelay(int delay) {
        int old = this.delay;
        this.delay = delay;
        if (old != delay) {
            if (busy != null) {
                busy.setDelay(delay);
                busy.setInitialDelay(delay);
            }
            firePropertyChange("delay", old, delay);
        }
    }

    /// Where the wheel is drawn: the place of the empty icon.
    Rectangle cn1WheelBounds() {
        Insets b = getInsets();
        int left = b.left;
        int right = b.right;
        int top = b.top;
        int bottom = b.bottom;
        if (com.codename1.ui.Display.isInitialized()) {
            com.codename1.ui.plaf.Style st = cn1Peer().getStyle();
            top += Units.toLogicalCeil(st.getPaddingTop());
            bottom += Units.toLogicalCeil(st.getPaddingBottom());
            left += Units.toLogicalCeil(st.getPaddingLeftNoRTL());
            right += Units.toLogicalCeil(st.getPaddingRightNoRTL());
        }
        String text = getText();
        Font f = getFont() != null ? getFont() : Fonts.defaultFont();
        FontMetrics fm = Fonts.metrics(f);
        boolean hasText = text != null && text.length() > 0 && !MiniHtml.isHtml(text);
        int tw = hasText ? fm.stringWidth(text) : 0;
        int gap = hasText ? getIconTextGap() : 0;
        int cw = size.width + gap + tw;
        int ch = Math.max(size.height, hasText ? fm.getHeight() : 0);
        int availW = getWidth() - left - right;
        int availH = getHeight() - top - bottom;
        int x = left;
        int ha = getHorizontalAlignment();
        if (ha == CENTER) {
            x += (availW - cw) / 2;
        } else if (ha == RIGHT || ha == TRAILING) {
            x += availW - cw;
        }
        int y = top;
        int va = getVerticalAlignment();
        if (va == CENTER) {
            y += (availH - ch) / 2;
        } else if (va == BOTTOM) {
            y += availH - ch;
        }
        int ht = getHorizontalTextPosition();
        if (ht == LEFT || ht == LEADING) {
            x += tw + gap;
        }
        return new Rectangle(x, y + (ch - size.height) / 2, size.width, size.height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        BusyPainter p = getBusyPainter();
        if (p == null || getIcon() != space) {
            return;
        }
        Rectangle r = cn1WheelBounds();
        Graphics copy = g.create();
        try {
            if (copy instanceof Graphics2D) {
                Graphics2D g2 = (Graphics2D) copy;
                g2.translate(r.x, r.y);
                p.paint(g2, this, r.width, r.height);
            }
        } finally {
            copy.dispose();
        }
    }
}
