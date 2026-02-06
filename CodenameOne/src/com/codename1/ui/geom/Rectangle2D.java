/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.geom;

import com.codename1.ui.Transform;

/// Represents a Rectangle position (x, y) and `Dimension` (width, height),
/// this is useful for measuring coordinates within the application.
///
/// @author Chen Fishbein
public class Rectangle2D implements Shape {

    private final Dimension2D size;
    private double x;
    private double y;
    private GeneralPath path;

    /// Creates a new instance of Rectangle
    public Rectangle2D() {
        size = new Dimension2D();
    }

    /// Creates a new instance of Rectangle at position (x, y) and with
    /// predefine dimension
    ///
    /// #### Parameters
    ///
    /// - `x`: the x coordinate of the rectangle
    ///
    /// - `y`: the y coordinate of the rectangle
    ///
    /// - `size`: the `Dimension` of the rectangle
    public Rectangle2D(double x, double y, Dimension2D size) {
        this.x = x;
        this.y = y;
        this.size = size;
    }

    /// Creates a new instance of Rectangle at position (x, y) and with
    /// predefine width and height
    ///
    /// #### Parameters
    ///
    /// - `x`: the x coordinate of the rectangle
    ///
    /// - `y`: the y coordinate of the rectangle
    ///
    /// - `w`: the width of the rectangle
    ///
    /// - `h`: the height of the rectangle
    public Rectangle2D(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.size = new Dimension2D(w, h);
    }

    /// A copy Constructor
    ///
    /// #### Parameters
    ///
    /// - `rect`: the Rectangle to copy
    public Rectangle2D(Rectangle2D rect) {
        this(rect.getX(), rect.getY(),
                rect.getSize().getWidth(), rect.getSize().getHeight());
    }

    /// Checks wheather the 2nd rectangle is contained in the first rectangle
    ///
    /// #### Parameters
    ///
    /// - `x1`: first rect x
    ///
    /// - `y1`: first rect y
    ///
    /// - `w1`: first rect w
    ///
    /// - `h1`: first rect h
    ///
    /// - `x2`: second rect x
    ///
    /// - `y2`: second rect y
    ///
    /// - `w2`: second rect w
    ///
    /// - `h2`: second rect h
    ///
    /// #### Returns
    ///
    /// true if x2, y2, w2, h2 is contained in x1, y1, w1, h1
    public static boolean contains(double x1, double y1, double w1, double h1,
                                   double x2, double y2, double w2, double h2) {
        return x1 <= x2 && y1 <= y2 && x1 + w1 >= x2 + w2 &&
                y1 + h1 >= y2 + h2;
    }

    /// Returns a rectangle that intersects the given rectangle with this rectangle
    ///
    /// #### Parameters
    ///
    /// - `rrX`: rectangle to intersect with this rectangle
    ///
    /// - `rrY`: rectangle to intersect with this rectangle
    ///
    /// - `rrW`: rectangle to intersect with this rectangle
    ///
    /// - `rrH`: rectangle to intersect with this rectangle
    ///
    /// - `rtx1`: rectangle to intersect with this rectangle
    ///
    /// - `rty1`: rectangle to intersect with this rectangle
    ///
    /// - `rtw2`: rectangle to intersect with this rectangle
    ///
    /// - `rth2`: rectangle to intersect with this rectangle
    ///
    /// - `dest`: result of the intersection are stored here
    public static void intersection(double rrX, double rrY, double rrW, double rrH, double rtx1, double rty1, double rtw2, double rth2, Rectangle2D dest) {
        double tx1 = rtx1;
        double ty1 = rty1;
        double rx1 = rrX;
        double ry1 = rrY;
        double tx2 = tx1;
        tx2 += rtw2;
        double ty2 = ty1;
        ty2 += rth2;
        double rx2 = rx1;
        rx2 += rrW;
        double ry2 = ry1;
        ry2 += rrH;
        if (tx1 < rx1) {
            tx1 = rx1;
        }
        if (ty1 < ry1) {
            ty1 = ry1;
        }
        if (tx2 > rx2) {
            tx2 = rx2;
        }
        if (ty2 > ry2) {
            ty2 = ry2;
        }
        tx2 -= tx1;
        ty2 -= ty1;

        // tx2,ty2 will never overflow (they will never be
        // larger than the smallest of the two source w,h)
        // they might underflow, though...
        if (tx2 < Integer.MIN_VALUE) {
            tx2 = Integer.MIN_VALUE;
        }
        if (ty2 < Integer.MIN_VALUE) {
            ty2 = Integer.MIN_VALUE;
        }

        dest.x = tx1;
        dest.y = ty1;
        dest.size.setWidth(tx2);
        dest.size.setHeight(ty2);
    }

    /// Helper method allowing us to determine if two coordinate sets intersect. This saves
    /// us the need of creating a rectangle object for a quick calculation
    ///
    /// #### Parameters
    ///
    /// - `tx`: x of first rectangle
    ///
    /// - `ty`: y of first rectangle
    ///
    /// - `tw`: width of first rectangle
    ///
    /// - `th`: height of first rectangle
    ///
    /// - `x`: x of second rectangle
    ///
    /// - `y`: y of second rectangle
    ///
    /// - `width`: width of second rectangle
    ///
    /// - `height`: height of second rectangle
    ///
    /// #### Returns
    ///
    /// true if the rectangles intersect
    public static boolean intersects(double tx, double ty, double tw, double th, double x, double y, double width, double height) {
        double rw = width;
        double rh = height;
        if (rw <= 0 || rh <= 0 || tw <= 0 || th <= 0) {
            return false;
        }
        double rx = x;
        double ry = y;
        rw += rx;
        rh += ry;
        tw += tx;
        th += ty;
        return ((rw < rx || rw > tx) &&
                (rh < ry || rh > ty) &&
                (tw < tx || tw > rx) &&
                (th < ty || th > ry));

    }

    /// Helper method to set coordinates
    public void setBounds(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.size.setWidth(w);
        this.size.setHeight(h);
        path = null;
    }

    /// Convenience method for getSize().getWidth();
    ///
    /// #### Returns
    ///
    /// width
    public double getWidth() {
        return size.getWidth();

    }

    /// Convenience method for getSize().setWidth();
    ///
    /// #### Parameters
    ///
    /// - `w`: the width
    public void setWidth(double w) {
        size.setWidth(w);
        path = null;
    }

    /// Convenience method for getSize().getHeight();
    ///
    /// #### Returns
    ///
    /// height
    public double getHeight() {
        return size.getHeight();
    }

    /// Convenience method for getSize().setHeight();
    ///
    /// #### Parameters
    ///
    /// - `h`: the height
    public void setHeight(double h) {
        size.setHeight(h);
        path = null;
    }

    /// Return the dimension of the rectangle
    ///
    /// #### Returns
    ///
    /// the size of the rectangle
    public Dimension2D getSize() {
        return size;
    }

    /// Return the x coordinate of the rectangle
    ///
    /// #### Returns
    ///
    /// the x coordinate of the rectangle
    public double getX() {
        return x;
    }

    /// Sets the x position of the rectangle
    ///
    /// #### Parameters
    ///
    /// - `x`: the x coordinate of the rectangle
    public void setX(int x) {
        this.x = x;
        path = null;
    }

    /// Sets the x position of the rectangle as a double.
    ///
    /// #### Parameters
    ///
    /// - `x`
    public void setX(double x) {
        this.x = x;
        path = null;
    }

    /// Return the y coordinate of the rectangle
    ///
    /// #### Returns
    ///
    /// the y coordinate of the rectangle
    public double getY() {
        return y;
    }

    /// Sets the y position of the rectangle
    ///
    /// #### Parameters
    ///
    /// - `y`: the y coordinate of the rectangle
    public void setY(int y) {
        this.y = y;
        path = null;
    }

    /// Sets the y position of the rectangle as a double.
    ///
    /// #### Parameters
    ///
    /// - `y`: The y position
    public void setY(double y) {
        this.y = y;
        path = null;
    }

    /// {@inheritDoc}
    @Override
    public String toString() {
        return "x = " + x + " y = " + y + " size = " + size;
    }

    /// Checks whether or not this Rectangle entirely contains the specified
    /// Rectangle.
    ///
    /// #### Parameters
    ///
    /// - `rect`: the specified Rectangle
    ///
    /// #### Returns
    ///
    /// @return true if the Rectangle is contained entirely inside this
    /// Rectangle; false otherwise
    public boolean contains(Rectangle2D rect) {
        return contains(rect.x, rect.y, rect.size.getWidth(), rect.size.getHeight());
    }

    /// Checks whether this Rectangle entirely contains the Rectangle
    /// at the specified location (rX, rY) with the specified
    /// dimensions (rWidth, rHeight).
    ///
    /// #### Parameters
    ///
    /// - `rX`: the specified x coordinate
    ///
    /// - `rY`: the specified y coordinate
    ///
    /// - `rWidth`: the width of the Rectangle
    ///
    /// - `rHeight`: the height of the Rectangle
    ///
    /// #### Returns
    ///
    /// @return true if the Rectangle specified by (rX, rY, rWidth, rHeight)
    /// is entirely enclosed inside this Rectangle; false otherwise.
    public boolean contains(double rX, double rY, double rWidth, double rHeight) {
        return x <= rX && y <= rY && x + size.getWidth() >= rX + rWidth &&
                y + size.getHeight() >= rY + rHeight;
    }

    /// Checks whether or not this Rectangle contains the point at the specified
    /// location (rX, rY).
    ///
    /// #### Parameters
    ///
    /// - `rX`: the specified x coordinate
    ///
    /// - `rY`: the specified y coordinate
    ///
    /// #### Returns
    ///
    /// @return true if the point (rX, rY) is inside this Rectangle;
    /// false otherwise.
    public boolean contains(double rX, double rY) {
        return x <= rX && y <= rY && x + size.getWidth() >= rX &&
                y + size.getHeight() >= rY;
    }

    /// Returns a rectangle that intersects the given rectangle with this rectangle
    ///
    /// #### Parameters
    ///
    /// - `rX`: rectangle to intersect with this rectangle
    ///
    /// - `rY`: rectangle to intersect with this rectangle
    ///
    /// - `rW`: rectangle to intersect with this rectangle
    ///
    /// - `rH`: rectangle to intersect with this rectangle
    ///
    /// #### Returns
    ///
    /// the intersection
    public Rectangle2D intersection(double rX, double rY, double rW, double rH) {
        double tx1 = this.x;
        double ty1 = this.y;
        double rx1 = rX;
        double ry1 = rY;
        double tx2 = tx1;
        tx2 += this.size.getWidth();
        double ty2 = ty1;
        ty2 += this.size.getHeight();
        double rx2 = rx1;
        rx2 += rW;
        double ry2 = ry1;
        ry2 += rH;
        if (tx1 < rx1) {
            tx1 = rx1;
        }
        if (ty1 < ry1) {
            ty1 = ry1;
        }
        if (tx2 > rx2) {
            tx2 = rx2;
        }
        if (ty2 > ry2) {
            ty2 = ry2;
        }
        tx2 -= tx1;
        ty2 -= ty1;
        // tx2,ty2 will never overflow (they will never be
        // larger than the smallest of the two source w,h)
        // they might underflow, though...
        if (tx2 < Integer.MIN_VALUE) {
            tx2 = Integer.MIN_VALUE;
        }
        if (ty2 < Integer.MIN_VALUE) {
            ty2 = Integer.MIN_VALUE;
        }
        return new Rectangle2D(tx1, ty1, tx2, ty2);
    }

    /// Returns a rectangle that intersects the given rectangle with this rectangle
    ///
    /// #### Parameters
    ///
    /// - `r`: rectangle to intersect with this rectangle
    ///
    /// #### Returns
    ///
    /// the intersection
    public Rectangle2D intersection(Rectangle2D r) {
        return intersection(r.x, r.y, r.size.getWidth(), r.size.getHeight());
    }

    /// Determines whether or not this Rectangle and the specified Rectangle
    /// location (x, y) with the specified dimensions (width, height),
    /// intersect. Two rectangles intersect if their intersection is nonempty.
    ///
    /// #### Parameters
    ///
    /// - `x`: the specified x coordinate
    ///
    /// - `y`: the specified y coordinate
    ///
    /// - `width`: the width of the Rectangle
    ///
    /// - `height`: the height of the Rectangle
    ///
    /// #### Returns
    ///
    /// @return true if the specified Rectangle and this Rectangle intersect;
    /// false otherwise.
    public boolean intersects(double x, double y, double width, double height) {
        double tw = size.getWidth();
        double th = size.getHeight();
        return intersects(this.x, this.y, tw, th, x, y, width, height);
    }

    /// Determines whether or not this Rectangle and the specified Rectangle
    /// location (x, y) with the specified dimensions (width, height),
    /// intersect. Two rectangles intersect if their intersection is nonempty.
    ///
    /// #### Parameters
    ///
    /// - `rect`: the Rectangle to check intersection with
    ///
    /// #### Returns
    ///
    /// @return true if the specified Rectangle and this Rectangle intersect;
    /// false otherwise.
    public boolean intersects(Rectangle2D rect) {
        return intersects(rect.getX(), rect.getY(),
                rect.getSize().getWidth(), rect.getSize().getHeight());
    }

    /// {{@inheritDoc}}
    @Override
    public PathIterator getPathIterator(Transform m) {
        if (path == null) {
            path = new GeneralPath();
            path.moveTo(x, y);
            path.lineTo(x + size.getWidth(), y);
            path.lineTo(x + size.getWidth(), y + size.getHeight());
            path.lineTo(x, y + size.getHeight());
            path.closePath();

        }
        return path.getPathIterator(m);

    }

    /// {{@inheritDoc}}
    @Override
    public PathIterator getPathIterator() {
        return getPathIterator(null);
    }

    /// {{@inheritDoc}}
    @Override
    public Rectangle getBounds() {
        return new Rectangle(
                (int) Math.floor(getX()),
                (int) Math.floor(getY()),
                (int) Math.ceil(getWidth()),
                (int) Math.ceil(getHeight())
        );
    }

    /// {{@inheritDoc}}
    @Override
    public float[] getBounds2D() {
        return new float[]{(float) getX(), (float) getY(), (float) getWidth(), (float) getHeight()};
    }

    /// {{@inheritDoc}}
    @Override
    public boolean isRectangle() {
        return true;
    }

    @Override
    public boolean contains(int x, int y) {
        return contains(x, (double) y);
    }

    @Override
    public Shape intersection(Rectangle rect) {
        Rectangle2D r2 = new Rectangle2D(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
        return intersection(r2);
    }

    public void translate(double x, double y) {
        this.x += x;
        this.y += y;
    }
}
