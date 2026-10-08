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

/// A float rectangle.
public class RectF {
    public float left;
    public float top;
    public float right;
    public float bottom;

    public RectF() {
    }

    public RectF(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public RectF(RectF r) {
        if (r != null) {
            set(r);
        }
    }

    public RectF(Rect r) {
        if (r != null) {
            set(r);
        }
    }

    public final boolean isEmpty() {
        return left >= right || top >= bottom;
    }

    public final float width() {
        return right - left;
    }

    public final float height() {
        return bottom - top;
    }

    public final float centerX() {
        return (left + right) * 0.5f;
    }

    public final float centerY() {
        return (top + bottom) * 0.5f;
    }

    public void setEmpty() {
        left = right = top = bottom = 0;
    }

    public void set(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public void set(RectF src) {
        set(src.left, src.top, src.right, src.bottom);
    }

    public void set(Rect src) {
        set(src.left, src.top, src.right, src.bottom);
    }

    public void offset(float dx, float dy) {
        left += dx;
        top += dy;
        right += dx;
        bottom += dy;
    }

    public void offsetTo(float newLeft, float newTop) {
        right += newLeft - left;
        bottom += newTop - top;
        left = newLeft;
        top = newTop;
    }

    public void inset(float dx, float dy) {
        left += dx;
        top += dy;
        right -= dx;
        bottom -= dy;
    }

    public boolean contains(float x, float y) {
        return left < right && top < bottom && x >= left && x < right && y >= top && y < bottom;
    }

    public boolean contains(RectF r) {
        return left < right && top < bottom && left <= r.left && top <= r.top && right >= r.right
                && bottom >= r.bottom;
    }

    public boolean intersect(float l, float t, float r, float b) {
        if (left < r && l < right && top < b && t < bottom) {
            left = Math.max(left, l);
            top = Math.max(top, t);
            right = Math.min(right, r);
            bottom = Math.min(bottom, b);
            return true;
        }
        return false;
    }

    public boolean intersect(RectF r) {
        return intersect(r.left, r.top, r.right, r.bottom);
    }

    public boolean intersects(float l, float t, float r, float b) {
        return left < r && l < right && top < b && t < bottom;
    }

    public void union(float l, float t, float r, float b) {
        if (l < r && t < b) {
            if (left < right && top < bottom) {
                left = Math.min(left, l);
                top = Math.min(top, t);
                right = Math.max(right, r);
                bottom = Math.max(bottom, b);
            } else {
                set(l, t, r, b);
            }
        }
    }

    public void union(RectF r) {
        union(r.left, r.top, r.right, r.bottom);
    }

    public void round(Rect dst) {
        dst.set(Math.round(left), Math.round(top), Math.round(right), Math.round(bottom));
    }

    public void roundOut(Rect dst) {
        dst.set((int) Math.floor(left), (int) Math.floor(top), (int) Math.ceil(right), (int) Math.ceil(bottom));
    }

    public void sort() {
        if (left > right) {
            float t = left;
            left = right;
            right = t;
        }
        if (top > bottom) {
            float t = top;
            top = bottom;
            bottom = t;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RectF)) {
            return false;
        }
        RectF r = (RectF) o;
        return left == r.left && top == r.top && right == r.right && bottom == r.bottom;
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits(left) * 31 + Float.floatToIntBits(top) * 17 + Float.floatToIntBits(right) * 7
                + Float.floatToIntBits(bottom);
    }

    @Override
    public String toString() {
        return "RectF(" + left + ", " + top + ", " + right + ", " + bottom + ")";
    }
}
