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
package android.graphics;

/// An integer rectangle, left/top inclusive and right/bottom exclusive.
public final class Rect {
    public int left;
    public int top;
    public int right;
    public int bottom;

    public Rect() {
    }

    public Rect(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public Rect(Rect r) {
        if (r != null) {
            set(r);
        }
    }

    public boolean isEmpty() {
        return left >= right || top >= bottom;
    }

    public int width() {
        return right - left;
    }

    public int height() {
        return bottom - top;
    }

    public int centerX() {
        return (left + right) >> 1;
    }

    public int centerY() {
        return (top + bottom) >> 1;
    }

    public float exactCenterX() {
        return (left + right) * 0.5f;
    }

    public float exactCenterY() {
        return (top + bottom) * 0.5f;
    }

    public void setEmpty() {
        left = right = top = bottom = 0;
    }

    public void set(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public void set(Rect src) {
        left = src.left;
        top = src.top;
        right = src.right;
        bottom = src.bottom;
    }

    public void offset(int dx, int dy) {
        left += dx;
        top += dy;
        right += dx;
        bottom += dy;
    }

    public void offsetTo(int newLeft, int newTop) {
        right += newLeft - left;
        bottom += newTop - top;
        left = newLeft;
        top = newTop;
    }

    public void inset(int dx, int dy) {
        left += dx;
        top += dy;
        right -= dx;
        bottom -= dy;
    }

    public void inset(int l, int t, int r, int b) {
        left += l;
        top += t;
        right -= r;
        bottom -= b;
    }

    public boolean contains(int x, int y) {
        return left < right && top < bottom && x >= left && x < right && y >= top && y < bottom;
    }

    public boolean contains(int l, int t, int r, int b) {
        return left < right && top < bottom && left <= l && top <= t && right >= r && bottom >= b;
    }

    public boolean contains(Rect r) {
        return contains(r.left, r.top, r.right, r.bottom);
    }

    public boolean intersect(int l, int t, int r, int b) {
        if (left < r && l < right && top < b && t < bottom) {
            if (left < l) {
                left = l;
            }
            if (top < t) {
                top = t;
            }
            if (right > r) {
                right = r;
            }
            if (bottom > b) {
                bottom = b;
            }
            return true;
        }
        return false;
    }

    public boolean intersect(Rect r) {
        return intersect(r.left, r.top, r.right, r.bottom);
    }

    public boolean intersects(int l, int t, int r, int b) {
        return left < r && l < right && top < b && t < bottom;
    }

    public static boolean intersects(Rect a, Rect b) {
        return a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom;
    }

    public void union(int l, int t, int r, int b) {
        if (l < r && t < b) {
            if (left < right && top < bottom) {
                if (left > l) {
                    left = l;
                }
                if (top > t) {
                    top = t;
                }
                if (right < r) {
                    right = r;
                }
                if (bottom < b) {
                    bottom = b;
                }
            } else {
                set(l, t, r, b);
            }
        }
    }

    public void union(Rect r) {
        union(r.left, r.top, r.right, r.bottom);
    }

    /// Extends the edges to the coordinate itself, not one past it, so a point
    /// on the right or bottom side stays outside the exclusive edge. This is
    /// exactly what Android does and is kept for identical layout results.
    public void union(int x, int y) {
        if (x < left) {
            left = x;
        } else if (x > right) {
            right = x;
        }
        if (y < top) {
            top = y;
        } else if (y > bottom) {
            bottom = y;
        }
    }

    public void sort() {
        if (left > right) {
            int t = left;
            left = right;
            right = t;
        }
        if (top > bottom) {
            int t = top;
            top = bottom;
            bottom = t;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Rect)) {
            return false;
        }
        Rect r = (Rect) o;
        return left == r.left && top == r.top && right == r.right && bottom == r.bottom;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * (31 * left + top) + right) + bottom;
    }

    @Override
    public String toString() {
        return "Rect(" + left + ", " + top + " - " + right + ", " + bottom + ")";
    }

    public String toShortString() {
        return "[" + left + "," + top + "][" + right + "," + bottom + "]";
    }
}
