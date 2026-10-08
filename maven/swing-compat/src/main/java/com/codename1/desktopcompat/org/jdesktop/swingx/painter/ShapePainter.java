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
import com.codename1.desktopcompat.java.awt.Paint;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.Shape;
import com.codename1.desktopcompat.java.awt.geom.AffineTransform;
import com.codename1.desktopcompat.java.awt.geom.Ellipse2D;
import com.codename1.desktopcompat.java.awt.geom.Path2D;

/// Paints any shape, filled, outlined or both, placed by the layout
/// properties: the shape's bounds are what is aligned.
///
/// The shape is not scaled when an axis is filled; filling only moves it
/// to the inset edge.
public class ShapePainter extends AbstractAreaPainter<Object> {

    private Shape shape;

    /// A red circle of 100 pixels with a black outline 3 wide.
    public ShapePainter() {
        this(new Ellipse2D.Double(0, 0, 100, 100), Color.RED);
    }

    public ShapePainter(Shape shape) {
        this(shape, Color.RED);
    }

    public ShapePainter(Shape shape, Paint paint) {
        this(shape, paint, Style.BOTH);
    }

    public ShapePainter(Shape shape, Paint paint, Style style) {
        super(paint);
        this.shape = shape;
        setBorderWidth(3);
        setBorderPaint(Color.BLACK);
        setStyle(style);
        setDirty(false);
    }

    public void setShape(Shape s) {
        Shape old = shape;
        shape = s;
        setDirty(true);
        firePropertyChange("shape", old, s);
    }

    public Shape getShape() {
        return shape;
    }

    @Override
    protected void doPaint(Graphics2D g, Object component, int w, int h) {
        if (getStyle() == Style.NONE) {
            return;
        }
        cn1PaintShape(g, provideShape(g, component, w, h), w, h);
    }

    /// The shape, moved to where the layout properties place it.
    @Override
    protected Shape provideShape(Graphics2D g, Object comp, int width, int height) {
        if (shape == null) {
            return null;
        }
        Rectangle bounds = shape.getBounds();
        Rectangle at = calculateLayout(bounds.width, bounds.height, width, height);
        return new Path2D.Double(shape, AffineTransform.getTranslateInstance(at.x - bounds.x, at.y - bounds.y));
    }
}
