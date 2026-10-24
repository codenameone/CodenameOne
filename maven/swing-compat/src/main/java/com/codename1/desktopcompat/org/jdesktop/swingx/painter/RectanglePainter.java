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

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Paint;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.Shape;
import com.codename1.desktopcompat.java.awt.geom.Rectangle2D;
import com.codename1.desktopcompat.java.awt.geom.RectangularShape;
import com.codename1.desktopcompat.java.awt.geom.RoundRectangle2D;

/// Paints a rectangle, with square or rounded corners, filled, outlined
/// or both.
///
/// A painter made without a size fills the painted area inside its
/// insets. One made with a width and height keeps that size and is placed
/// by its alignments. The outline is drawn inside the rectangle's bounds:
/// the shape is drawn half a border width in from them.
public class RectanglePainter extends AbstractAreaPainter<Object> {

    private boolean rounded;
    private int roundWidth = 20;
    private int roundHeight = 20;
    private int width = -1;
    private int height = -1;

    public RectanglePainter() {
        this(0, 0, 0, 0);
    }

    public RectanglePainter(int top, int left, int bottom, int right) {
        this(top, left, bottom, right, 0, 0);
    }

    public RectanglePainter(int top, int left, int bottom, int right, int roundWidth, int roundHeight) {
        this(top, left, bottom, right, roundWidth, roundHeight, roundWidth != 0 || roundHeight != 0, Color.RED, 1f,
                Color.BLACK);
    }

    public RectanglePainter(int top, int left, int bottom, int right, int roundWidth, int roundHeight,
            boolean rounded, Paint fillPaint, float strokeWidth, Paint borderPaint) {
        this(new Insets(top, left, bottom, right), -1, -1, roundWidth, roundHeight, rounded, fillPaint, strokeWidth,
                borderPaint);
    }

    public RectanglePainter(Color fillPaint, Color borderPaint) {
        this(fillPaint, borderPaint, 1f, null);
    }

    public RectanglePainter(Paint fillPaint, Paint borderPaint, float borderWidth, Style style) {
        this(new Insets(0, 0, 0, 0), -1, -1, 0, 0, false, fillPaint, borderWidth, borderPaint);
        setStyle(style);
        setDirty(false);
    }

    public RectanglePainter(int width, int height, int cornerRadius, Paint fillPaint) {
        this(new Insets(0, 0, 0, 0), width, height, cornerRadius, cornerRadius, true, fillPaint, 1f, Color.BLACK);
    }

    public RectanglePainter(Insets insets, int width, int height, int roundWidth, int roundHeight, boolean rounded,
            Paint fillPaint, float strokeWidth, Paint borderPaint) {
        this.width = width;
        this.height = height;
        setFillHorizontal(width < 0);
        setFillVertical(height < 0);
        setInsets(insets);
        this.roundWidth = roundWidth;
        this.roundHeight = roundHeight;
        this.rounded = rounded;
        setFillPaint(fillPaint);
        setBorderWidth(strokeWidth);
        setBorderPaint(borderPaint);
        setDirty(false);
    }

    public boolean isRounded() {
        return rounded;
    }

    public void setRounded(boolean rounded) {
        boolean old = this.rounded;
        this.rounded = rounded;
        setDirty(true);
        firePropertyChange("rounded", Boolean.valueOf(old), Boolean.valueOf(rounded));
    }

    public int getRoundWidth() {
        return roundWidth;
    }

    public void setRoundWidth(int roundWidth) {
        int old = this.roundWidth;
        this.roundWidth = roundWidth;
        setDirty(true);
        firePropertyChange("roundWidth", Integer.valueOf(old), Integer.valueOf(roundWidth));
    }

    public int getRoundHeight() {
        return roundHeight;
    }

    public void setRoundHeight(int roundHeight) {
        int old = this.roundHeight;
        this.roundHeight = roundHeight;
        setDirty(true);
        firePropertyChange("roundHeight", Integer.valueOf(old), Integer.valueOf(roundHeight));
    }

    /// The rectangle painted in an area of the given size.
    protected RectangularShape calculateShape(int width, int height) {
        Rectangle bounds = calculateLayout(Math.max(0, this.width), Math.max(0, this.height), width, height);
        double inset = cn1Outlines() ? getBorderWidth() / 2.0 : 0;
        double x = bounds.x + inset;
        double y = bounds.y + inset;
        double w = Math.max(0, bounds.width - 2 * inset);
        double h = Math.max(0, bounds.height - 2 * inset);
        if (rounded) {
            return new RoundRectangle2D.Double(x, y, w, h, roundWidth, roundHeight);
        }
        return new Rectangle2D.Double(x, y, w, h);
    }

    @Override
    protected void doPaint(Graphics2D g, Object component, int width, int height) {
        Shape shape = provideShape(g, component, width, height);
        if (getStyle() == Style.NONE) {
            return;
        }
        cn1PaintShape(g, shape, width, height);
    }

    @Override
    protected Shape provideShape(Graphics2D g, Object comp, int width, int height) {
        return calculateShape(width, height);
    }
}
