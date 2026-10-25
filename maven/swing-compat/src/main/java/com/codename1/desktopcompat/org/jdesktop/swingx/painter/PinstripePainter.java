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
package com.codename1.desktopcompat.org.jdesktop.swingx.painter;

import com.codename1.desktopcompat.java.awt.BasicStroke;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.Paint;
import com.codename1.desktopcompat.java.awt.geom.Line2D;

/// Covers the area with parallel stripes at an angle.
///
/// The angle is in degrees and turns clockwise from vertical stripes: 0
/// draws vertical lines, 90 horizontal ones and 45 lines that rise to the
/// right.
public class PinstripePainter extends AbstractPainter<Object> {

    private double angle = 45;
    private double spacing = 8;
    private double stripeWidth = 1;
    private Paint paint;

    public PinstripePainter() {
    }

    public PinstripePainter(Paint paint) {
        this(paint, 45);
    }

    public PinstripePainter(double angle) {
        this.angle = angle;
    }

    public PinstripePainter(Paint paint, double angle) {
        this.paint = paint;
        this.angle = angle;
    }

    public PinstripePainter(Paint paint, double angle, double stripeWidth, double spacing) {
        this.paint = paint;
        this.angle = angle;
        this.stripeWidth = stripeWidth;
        this.spacing = spacing;
    }

    public Paint getPaint() {
        return paint;
    }

    /// Sets the paint of the stripes; with `null` they are drawn in the
    /// color the graphics has, or the foreground of the component painted
    /// for.
    public void setPaint(Paint p) {
        Paint old = paint;
        paint = p;
        setDirty(true);
        firePropertyChange("paint", old, p);
    }

    public double getAngle() {
        return angle;
    }

    /// Sets the angle in degrees; it is brought into 0 to 360.
    public void setAngle(double angle) {
        double a = angle;
        if (a > 360) {
            a = a % 360;
        }
        if (a < 0) {
            a = 360 - ((a * -1) % 360);
        }
        double old = this.angle;
        this.angle = a;
        setDirty(true);
        firePropertyChange("angle", Double.valueOf(old), Double.valueOf(a));
    }

    public double getStripeWidth() {
        return stripeWidth;
    }

    public void setStripeWidth(double stripeWidth) {
        double old = this.stripeWidth;
        this.stripeWidth = stripeWidth;
        setDirty(true);
        firePropertyChange("stripeWidth", Double.valueOf(old), Double.valueOf(stripeWidth));
    }

    public double getSpacing() {
        return spacing;
    }

    /// The distance between two stripes.
    public void setSpacing(double spacing) {
        double old = this.spacing;
        this.spacing = spacing;
        setDirty(true);
        firePropertyChange("spacing", Double.valueOf(old), Double.valueOf(spacing));
    }

    @Override
    protected void doPaint(Graphics2D g, Object component, int width, int height) {
        double step = stripeWidth + spacing;
        if (stripeWidth <= 0 || step < 0.5) {
            return;
        }
        Paint p = paint;
        if (p == null && component instanceof com.codename1.desktopcompat.java.awt.Component) {
            Color fg = ((com.codename1.desktopcompat.java.awt.Component) component).getForeground();
            p = fg;
        }
        if (p != null) {
            g.setPaint(p);
        }
        g.clipRect(0, 0, width, height);
        g.setStroke(new BasicStroke((float) stripeWidth));
        // Vertical lines over a square that covers the area at any angle,
        // turned about the center of the area.
        double reach = Math.sqrt((double) width * width + (double) height * height) / 2 + step;
        double cx = width / 2.0;
        double cy = height / 2.0;
        g.rotate(Math.toRadians(angle), cx, cy);
        int count = (int) Math.ceil(reach / step);
        for (int i = -count; i <= count; i++) {
            double x = cx + i * step;
            g.draw(new Line2D.Double(x, cy - reach, x, cy + reach));
        }
    }
}
