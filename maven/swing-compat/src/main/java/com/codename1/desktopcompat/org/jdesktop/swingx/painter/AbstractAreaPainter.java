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
import com.codename1.desktopcompat.java.awt.GradientPaint;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.Paint;
import com.codename1.desktopcompat.java.awt.Shape;
import com.codename1.desktopcompat.java.awt.geom.Point2D;

/// A painter of a shape: an inside that is filled and an outline that is
/// drawn, each with a paint of its own.
///
/// ## What differs from SwingX
///
///  - A paint is a color or a `GradientPaint`; the graphics of this layer
///    draws with nothing else.
///  - A stretched gradient is fitted to the painted area by its direction:
///    a horizontal gradient runs across the width, a vertical one down the
///    height and any other from corner to corner.
///  - There are no area effects (shadows, glows), so their accessors are
///    absent.
public abstract class AbstractAreaPainter<T> extends AbstractLayoutPainter<T> {

    /// Which parts of the shape are painted.
    public enum Style {
        BOTH,
        FILLED,
        OUTLINE,
        NONE
    }

    private Paint fillPaint;
    private Paint borderPaint;
    private boolean paintStretched;
    private Style style = Style.BOTH;
    private float borderWidth;

    /// A painter that fills with red, the SwingX default.
    public AbstractAreaPainter() {
        fillPaint = Color.RED;
    }

    public AbstractAreaPainter(Paint paint) {
        fillPaint = paint;
    }

    public Paint getFillPaint() {
        return fillPaint;
    }

    public void setFillPaint(Paint p) {
        Paint old = fillPaint;
        fillPaint = p;
        setDirty(true);
        firePropertyChange("fillPaint", old, p);
    }

    public boolean isPaintStretched() {
        return paintStretched;
    }

    /// Whether a gradient is fitted to the area that is painted instead of
    /// being used at the coordinates it was made with.
    public void setPaintStretched(boolean paintStretched) {
        boolean old = this.paintStretched;
        this.paintStretched = paintStretched;
        setDirty(true);
        firePropertyChange("paintStretched", Boolean.valueOf(old), Boolean.valueOf(paintStretched));
    }

    public void setBorderPaint(Paint p) {
        Paint old = borderPaint;
        borderPaint = p;
        setDirty(true);
        firePropertyChange("borderPaint", old, p);
    }

    public Paint getBorderPaint() {
        return borderPaint;
    }

    /// Sets what is painted; `null` is [Style#BOTH].
    public void setStyle(Style s) {
        Style old = style;
        style = s == null ? Style.BOTH : s;
        setDirty(true);
        firePropertyChange("style", old, style);
    }

    public Style getStyle() {
        return style;
    }

    public void setBorderWidth(float s) {
        float old = borderWidth;
        borderWidth = s;
        setDirty(true);
        firePropertyChange("borderWidth", Float.valueOf(old), Float.valueOf(s));
    }

    public float getBorderWidth() {
        return borderWidth;
    }

    /// The outline of what this painter draws in an area of the given
    /// size.
    protected abstract Shape provideShape(Graphics2D g, T comp, int width, int height);

    /// `p`, fitted to the area when it is a gradient and stretching is on.
    Paint cn1Fitted(Paint p, int width, int height) {
        if (!paintStretched || !(p instanceof GradientPaint)) {
            return p;
        }
        GradientPaint gp = (GradientPaint) p;
        Point2D a = gp.getPoint1();
        Point2D b = gp.getPoint2();
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        float x1 = 0;
        float y1 = 0;
        float x2 = 0;
        float y2 = 0;
        if (dx > 0) {
            x2 = width;
        } else if (dx < 0) {
            x1 = width;
        }
        if (dy > 0) {
            y2 = height;
        } else if (dy < 0) {
            y1 = height;
        }
        if (x1 == x2 && y1 == y2) {
            return gp.getColor1();
        }
        return new GradientPaint(x1, y1, gp.getColor1(), x2, y2, gp.getColor2(), gp.isCyclic());
    }

    boolean cn1Fills() {
        return (style == Style.BOTH || style == Style.FILLED) && fillPaint != null;
    }

    boolean cn1Outlines() {
        return (style == Style.BOTH || style == Style.OUTLINE) && borderPaint != null && borderWidth > 0;
    }

    /// Fills and outlines `shape` as the style and paints say.
    void cn1PaintShape(Graphics2D g, Shape shape, int width, int height) {
        if (shape == null) {
            return;
        }
        if (cn1Fills()) {
            g.setPaint(cn1Fitted(fillPaint, width, height));
            g.fill(shape);
        }
        if (cn1Outlines()) {
            g.setPaint(cn1Fitted(borderPaint, width, height));
            g.setStroke(new BasicStroke(borderWidth));
            g.draw(shape);
        }
    }
}
