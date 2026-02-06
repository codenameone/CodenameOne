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

import java.util.ArrayList;

/// Represents a Rectangle position (x, y) and `Dimension` (width, height),
/// this is useful for measuring coordinates within the application.
///
/// @author Chen Fishbein
public class Rectangle implements Shape {

    private static final int MAX_POOL_SIZE = 20;
    private static ArrayList<Rectangle> pool;
    private final Dimension size;
    private int x;
    private int y;
    private GeneralPath path;

    /// Creates a new instance of Rectangle
    public Rectangle() {
        size = new Dimension();
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
    public Rectangle(int x, int y, Dimension size) {
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
    public Rectangle(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.size = new Dimension(w, h);
    }

    /// A copy Constructor
    ///
    /// #### Parameters
    ///
    /// - `rect`: the Rectangle to copy
    public Rectangle(Rectangle rect) {
        this(rect.getX(), rect.getY(),
                rect.getSize().getWidth(), rect.getSize().getHeight());
    }

    /// Creates a rectangle from a Rectangle object pool.  This is handy if you
    /// need to create a temporary Rectangle that you wish to recycle later.
    ///
    /// When you are done with this object you should return it to the pool using
    /// `#recycle(com.codename1.ui.geom.Rectangle)`.
    ///
    /// #### Parameters
    ///
    /// - `x`: The x coordinate of the rect.
    ///
    /// - `y`: The y coordinate of the rect.
    ///
    /// - `w`: The width of the rect.
    ///
    /// - `h`: The height of the rect.
    ///
    /// #### Returns
    ///
    /// A rectangle with the given dimensions.
    ///
    /// #### See also
    ///
    /// - #recycle(com.codename1.ui.geom.Rectangle)
    public static synchronized Rectangle createFromPool(int x, int y, int w, int h) {
        if (pool == null) {
            pool = new ArrayList<Rectangle>();
        }
        if (pool.isEmpty()) {
            return new Rectangle(x, y, w, h);
        } else {
            Rectangle r = pool.remove(pool.size() - 1);
            r.setBounds(x, y, w, h);
            return r;
        }
    }

    /// Returns the given rectangle to the object pool.
    ///
    /// #### Parameters
    ///
    /// - `r`: The rectangle to recycle.
    ///
    /// #### See also
    ///
    /// - #createFromPool(int, int, int, int)
    public static synchronized void recycle(Rectangle r) {
        if (pool.size() >= MAX_POOL_SIZE || r == null) {
            return;
        }
        pool.add(r);
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
    public static boolean contains(int x1, int y1, int w1, int h1,
                                   int x2, int y2, int w2, int h2) {
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
    public static void intersection(int rrX, int rrY, int rrW, int rrH, int rtx1, int rty1, int rtw2, int rth2, Rectangle dest) {
        int tx1 = rtx1;
        int ty1 = rty1;
        int rx1 = rrX;
        int ry1 = rrY;
        int tx2 = tx1;
        tx2 += rtw2;
        int ty2 = ty1;
        ty2 += rth2;
        int rx2 = rx1;
        rx2 += rrW;
        int ry2 = ry1;
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
    public static boolean intersects(int tx, int ty, int tw, int th, int x, int y, int width, int height) {
        int rw = width;
        int rh = height;
        if (rw <= 0 || rh <= 0 || tw <= 0 || th <= 0) {
            return false;
        }
        int rx = x;
        int ry = y;
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
    public void setBounds(int x, int y, int w, int h) {
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
    public int getWidth() {
        return size.getWidth();

    }

    /// Convenience method for getSize().setWidth();
    ///
    /// #### Parameters
    ///
    /// - `w`: the width
    public void setWidth(int w) {
        size.setWidth(w);
        path = null;
    }

    /// Convenience method for getSize().getHeight();
    ///
    /// #### Returns
    ///
    /// height
    public int getHeight() {
        return size.getHeight();
    }

    /// Convenience method for getSize().setHeight();
    ///
    /// #### Parameters
    ///
    /// - `h`: the height
    public void setHeight(int h) {
        size.setHeight(h);
        path = null;
    }

    /// Return the dimension of the rectangle
    ///
    /// #### Returns
    ///
    /// the size of the rectangle
    public Dimension getSize() {
        return size;
    }

    /// Return the x coordinate of the rectangle
    ///
    /// #### Returns
    ///
    /// the x coordinate of the rectangle
    public int getX() {
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

    /// Return the y coordinate of the rectangle
    ///
    /// #### Returns
    ///
    /// the y coordinate of the rectangle
    public int getY() {
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
    public boolean contains(Rectangle rect) {
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
    public boolean contains(int rX, int rY, int rWidth, int rHeight) {
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
    @Override
    public boolean contains(int rX, int rY) {
        return x <= rX && y <= rY && x + size.getWidth() >= rX &&
                y + size.getHeight() >= rY;
    }

    /// Returns a rectangle that intersects the given rectangle with this rectangle.  If they
    /// don't intersect, the resulting rectangle will have a negative width or height.
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
    public Rectangle intersection(int rX, int rY, int rW, int rH) {

        int tx1 = this.x;
        int ty1 = this.y;
        int rx1 = rX;
        int ry1 = rY;
        int tx2 = tx1;
        tx2 += this.size.getWidth();
        int ty2 = ty1;
        ty2 += this.size.getHeight();
        int rx2 = rx1;
        rx2 += rW;
        int ry2 = ry1;
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
        return new Rectangle(tx1, ty1, tx2, ty2);
    }

    public void intersection(Rectangle input, Rectangle output) {
        int tx1 = this.x;
        int ty1 = this.y;
        int rx1 = input.getX();
        int ry1 = input.getY();
        int tx2 = tx1;
        tx2 += this.size.getWidth();
        int ty2 = ty1;
        ty2 += this.size.getHeight();
        int rx2 = rx1;
        rx2 += input.getWidth();
        int ry2 = ry1;
        ry2 += input.getHeight();
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
        tx2 = Math.max(0, tx2);
        ty2 = Math.max(0, ty2);
        output.setBounds(tx1, ty1, tx2, ty2);
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
    @Override
    public Rectangle intersection(Rectangle r) {
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
    public boolean intersects(int x, int y, int width, int height) {
        int tw = size.getWidth();
        int th = size.getHeight();
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
    public boolean intersects(Rectangle rect) {
        return intersects(rect.getX(), rect.getY(),
                rect.getSize().getWidth(), rect.getSize().getHeight());
    }

    /// {{@inheritDoc}}
    @Override
    public PathIterator getPathIterator(Transform m) {
        if (path == null) {
            path = new GeneralPath();
            int w = size.getWidth();
            int h = size.getHeight();
            path.moveTo(x, y);
            path.lineTo(x + w, y);
            path.lineTo(x + w, y + h);
            path.lineTo(x, y + h);
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
        return this;
    }

    /// Sets the bounds of this rectangle.
    ///
    /// #### Parameters
    ///
    /// - `bounds`: A rectangle whose bounds will be copied.
    ///
    /// #### Since
    ///
    /// 7.0
    public void setBounds(Rectangle bounds) {
        setBounds(bounds.x, bounds.y, bounds.size.getWidth(), bounds.size.getHeight());
    }

    /// {{@inheritDoc}}
    @Override
    public float[] getBounds2D() {
        return new float[]{getX(), getY(), getWidth(), getHeight()};
    }

    /// {{@inheritDoc}}
    @Override
    public boolean isRectangle() {
        return true;
    }

    /// {{@inheritDoc}}
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Rectangle) {
            Rectangle r = (Rectangle) obj;
            return r.x == x && r.y == y && r.size.equals(size);
        }
        return false;
    }

    /// {{@inheritDoc}}
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + this.x;
        hash = 83 * hash + this.y;
        hash = 83 * hash + this.size.getWidth();
        hash = 83 * hash + this.size.getHeight();
        return hash;
    }


}
