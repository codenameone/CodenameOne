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
package com.codename1.desktopcompat.java.awt;

import com.codename1.desktopcompat.java.awt.geom.Rectangle2D;

/// A rectangle in integer coordinates.
///
/// A rectangle with a zero width or height is empty and contains nothing; one
/// with a negative width or height does not exist at all, and is ignored by
/// `union` and `add`. Sums that would not fit an `int` are worked out in
/// `long` and clamped.
public class Rectangle extends Rectangle2D implements Shape {

    public int x;

    public int y;

    public int width;

    public int height;

    public Rectangle() {
    }

    public Rectangle(Rectangle r) {
        this.x = r.x;
        this.y = r.y;
        this.width = r.width;
        this.height = r.height;
    }

    public Rectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rectangle(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Rectangle(Point p, Dimension d) {
        this.x = p.x;
        this.y = p.y;
        this.width = d.width;
        this.height = d.height;
    }

    public Rectangle(Point p) {
        this.x = p.x;
        this.y = p.y;
    }

    public Rectangle(Dimension d) {
        this.width = d.width;
        this.height = d.height;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getWidth() {
        return width;
    }

    @Override
    public double getHeight() {
        return height;
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    @Override
    public Rectangle2D getBounds2D() {
        return new Rectangle(x, y, width, height);
    }

    public void setBounds(Rectangle r) {
        setBounds(r.x, r.y, r.width, r.height);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /// Sets the smallest integer rectangle that encloses the given one.
    @Override
    public void setRect(double x, double y, double width, double height) {
        int newX = (int) Math.floor(x);
        int newY = (int) Math.floor(y);
        // a cast clamps to the int range, so far-off values stay far off
        this.width = width >= 0.0 ? (int) Math.ceil(width + (x - newX)) : (int) Math.floor(width);
        this.height = height >= 0.0 ? (int) Math.ceil(height + (y - newY)) : (int) Math.floor(height);
        this.x = newX;
        this.y = newY;
    }

    public Point getLocation() {
        return new Point(x, y);
    }

    public void setLocation(Point p) {
        setLocation(p.x, p.y);
    }

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void translate(int dx, int dy) {
        this.x = clamp((long) x + dx);
        this.y = clamp((long) y + dy);
    }

    public Dimension getSize() {
        return new Dimension(width, height);
    }

    public void setSize(Dimension d) {
        setSize(d.width, d.height);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public boolean contains(Point p) {
        return contains(p.x, p.y);
    }

    public boolean contains(int px, int py) {
        if (width < 0 || height < 0 || px < x || py < y) {
            return false;
        }
        return px < (long) x + width && py < (long) y + height;
    }

    public boolean contains(Rectangle r) {
        return contains(r.x, r.y, r.width, r.height);
    }

    /// Whether the given rectangle lies wholly inside this one. An empty
    /// rectangle on either side gives false.
    public boolean contains(int rx, int ry, int rw, int rh) {
        if (width <= 0 || height <= 0 || rw <= 0 || rh <= 0 || rx < x || ry < y) {
            return false;
        }
        return (long) rx + rw <= (long) x + width && (long) ry + rh <= (long) y + height;
    }

    public boolean intersects(Rectangle r) {
        if (width <= 0 || height <= 0 || r.width <= 0 || r.height <= 0) {
            return false;
        }
        return (long) r.x + r.width > x && (long) r.y + r.height > y
                && (long) x + width > r.x && (long) y + height > r.y;
    }

    /// The overlap of the two rectangles. When they do not overlap the
    /// result has a negative or zero width or height.
    public Rectangle intersection(Rectangle r) {
        int x1 = Math.max(x, r.x);
        int y1 = Math.max(y, r.y);
        long x2 = Math.min((long) x + width, (long) r.x + r.width);
        long y2 = Math.min((long) y + height, (long) r.y + r.height);
        return new Rectangle(x1, y1, clamp(x2 - x1), clamp(y2 - y1));
    }

    /// The smallest rectangle enclosing both. When this rectangle has a
    /// negative size the other is returned, and the other way round.
    public Rectangle union(Rectangle r) {
        if (width < 0 || height < 0) {
            return new Rectangle(r);
        }
        Rectangle result = new Rectangle(this);
        result.add(r);
        return result;
    }

    /// Grows the rectangle to reach a point. A rectangle with a negative
    /// size becomes the empty rectangle at that point.
    public void add(int newx, int newy) {
        if (width < 0 || height < 0) {
            this.x = newx;
            this.y = newy;
            this.width = 0;
            this.height = 0;
            return;
        }
        long x2 = Math.max((long) x + width, (long) newx);
        long y2 = Math.max((long) y + height, (long) newy);
        this.x = Math.min(x, newx);
        this.y = Math.min(y, newy);
        this.width = clamp(x2 - this.x);
        this.height = clamp(y2 - this.y);
    }

    public void add(Point pt) {
        add(pt.x, pt.y);
    }

    /// Grows the rectangle to enclose another, ignoring one that has a
    /// negative size. A rectangle that itself has a negative size first
    /// moves to the other one; as in the JDK its own width and height still
    /// take part from there, so a positive one of the two can survive.
    public void add(Rectangle r) {
        long w = width;
        long h = height;
        if (width < 0 || height < 0) {
            setBounds(r.x, r.y, r.width, r.height);
        }
        if (r.width < 0 || r.height < 0) {
            return;
        }
        long x2 = Math.max(x + w, (long) r.x + r.width);
        long y2 = Math.max(y + h, (long) r.y + r.height);
        this.x = Math.min(x, r.x);
        this.y = Math.min(y, r.y);
        this.width = clamp(x2 - this.x);
        this.height = clamp(y2 - this.y);
    }

    /// Moves each side outwards by the given amounts; negative amounts
    /// shrink the rectangle and can leave it with a negative size.
    public void grow(int h, int v) {
        long x1 = (long) x - h;
        long y1 = (long) y - v;
        long x2 = (long) x + width + h;
        long y2 = (long) y + height + v;
        this.x = clamp(x1);
        this.y = clamp(y1);
        this.width = clamp(x2 - this.x);
        this.height = clamp(y2 - this.y);
    }

    private static int clamp(long v) {
        if (v > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (v < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) v;
    }

    @Override
    public boolean isEmpty() {
        return width <= 0 || height <= 0;
    }

    @Override
    public int outcode(double x, double y) {
        int out = 0;
        if (width <= 0) {
            out |= OUT_LEFT | OUT_RIGHT;
        } else if (x < this.x) {
            out |= OUT_LEFT;
        } else if (x > this.x + (double) width) {
            out |= OUT_RIGHT;
        }
        if (height <= 0) {
            out |= OUT_TOP | OUT_BOTTOM;
        } else if (y < this.y) {
            out |= OUT_TOP;
        } else if (y > this.y + (double) height) {
            out |= OUT_BOTTOM;
        }
        return out;
    }

    @Override
    public Rectangle2D createIntersection(Rectangle2D r) {
        if (r instanceof Rectangle) {
            return intersection((Rectangle) r);
        }
        Rectangle2D dest = new Rectangle2D.Double();
        Rectangle2D.intersect(this, r, dest);
        return dest;
    }

    @Override
    public Rectangle2D createUnion(Rectangle2D r) {
        if (r instanceof Rectangle) {
            return union((Rectangle) r);
        }
        Rectangle2D dest = new Rectangle2D.Double();
        Rectangle2D.union(this, r, dest);
        return dest;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Rectangle) {
            Rectangle r = (Rectangle) obj;
            return x == r.x && y == r.y && width == r.width && height == r.height;
        }
        return super.equals(obj);
    }

    @Override
    public Object clone() {
        return new Rectangle(x, y, width, height);
    }

    @Override
    public String toString() {
        return "java.awt.Rectangle[x=" + x + ",y=" + y + ",width=" + width + ",height=" + height + "]";
    }
}
