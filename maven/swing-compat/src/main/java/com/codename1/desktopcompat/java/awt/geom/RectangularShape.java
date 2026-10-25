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
package com.codename1.desktopcompat.java.awt.geom;

import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.Shape;

/// The base of every shape defined by a rectangular frame: rectangles,
/// rounded rectangles, ellipses and arcs.
public abstract class RectangularShape implements Shape, Cloneable {

    protected RectangularShape() {
    }

    public abstract double getX();

    public abstract double getY();

    public abstract double getWidth();

    public abstract double getHeight();

    public double getMinX() {
        return getX();
    }

    public double getMinY() {
        return getY();
    }

    public double getMaxX() {
        return getX() + getWidth();
    }

    public double getMaxY() {
        return getY() + getHeight();
    }

    public double getCenterX() {
        return getX() + getWidth() / 2.0;
    }

    public double getCenterY() {
        return getY() + getHeight() / 2.0;
    }

    public Rectangle2D getFrame() {
        return new Rectangle2D.Double(getX(), getY(), getWidth(), getHeight());
    }

    public abstract boolean isEmpty();

    public abstract void setFrame(double x, double y, double w, double h);

    public void setFrame(Point2D loc, Dimension2D size) {
        setFrame(loc.getX(), loc.getY(), size.getWidth(), size.getHeight());
    }

    public void setFrame(Rectangle2D r) {
        setFrame(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public void setFrameFromDiagonal(double x1, double y1, double x2, double y2) {
        setFrame(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
    }

    public void setFrameFromDiagonal(Point2D p1, Point2D p2) {
        setFrameFromDiagonal(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    public void setFrameFromCenter(double centerX, double centerY, double cornerX, double cornerY) {
        double halfW = Math.abs(cornerX - centerX);
        double halfH = Math.abs(cornerY - centerY);
        setFrame(centerX - halfW, centerY - halfH, halfW * 2.0, halfH * 2.0);
    }

    public void setFrameFromCenter(Point2D center, Point2D corner) {
        setFrameFromCenter(center.getX(), center.getY(), corner.getX(), corner.getY());
    }

    @Override
    public boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    /// The smallest integer rectangle enclosing the frame, or an empty one at
    /// the origin when the frame has a negative size.
    @Override
    public Rectangle getBounds() {
        double w = getWidth();
        double h = getHeight();
        if (w < 0.0 || h < 0.0) {
            return new Rectangle();
        }
        double x1 = Math.floor(getX());
        double y1 = Math.floor(getY());
        double x2 = Math.ceil(getX() + w);
        double y2 = Math.ceil(getY() + h);
        return new Rectangle((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new FlatteningPathIterator(getPathIterator(at), flatness);
    }

    /// Returns a copy of this shape. Every shape of this package returns its
    /// own type; a subclass from elsewhere that does not override it gets a
    /// rectangle with the same frame.
    @Override
    public Object clone() {
        return new Rectangle2D.Double(getX(), getY(), getWidth(), getHeight());
    }
}
