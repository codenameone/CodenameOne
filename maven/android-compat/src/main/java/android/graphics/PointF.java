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

/// A float point.
public class PointF {
    public float x;
    public float y;

    public PointF() {
    }

    public PointF(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public PointF(Point p) {
        this.x = p.x;
        this.y = p.y;
    }

    public final void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public final void set(PointF p) {
        this.x = p.x;
        this.y = p.y;
    }

    public final void offset(float dx, float dy) {
        x += dx;
        y += dy;
    }

    public final float length() {
        return (float) Math.sqrt(x * x + y * y);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PointF && Float.compare(((PointF) o).x, x) == 0 && Float.compare(((PointF) o).y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits(x) * 31 + Float.floatToIntBits(y);
    }

    @Override
    public String toString() {
        return "PointF(" + x + ", " + y + ")";
    }
}
