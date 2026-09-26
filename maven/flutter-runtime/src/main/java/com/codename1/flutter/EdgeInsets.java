/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

/**
 * Immutable offsets for each of the four box edges, in logical pixels.
 */
public class EdgeInsets extends EdgeInsetsGeometry {

    public static final EdgeInsets zero = new EdgeInsets(0, 0, 0, 0);

    private final double left;
    private final double top;
    private final double right;
    private final double bottom;

    protected EdgeInsets(double left, double top, double right, double bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public static EdgeInsets all(double value) {
        return new EdgeInsets(value, value, value, value);
    }

    public static EdgeInsets only(double left, double top, double right, double bottom) {
        return new EdgeInsets(left, top, right, bottom);
    }

    public static EdgeInsets symmetric(double horizontal, double vertical) {
        return new EdgeInsets(horizontal, vertical, horizontal, vertical);
    }

    public static EdgeInsets fromLTRB(double left, double top, double right, double bottom) {
        return new EdgeInsets(left, top, right, bottom);
    }

    public double left() {
        return left;
    }

    public double top() {
        return top;
    }

    public double right() {
        return right;
    }

    public double bottom() {
        return bottom;
    }

    /**
     * {@code EdgeInsetsGeometry.add}: the edge-wise sum of this and {@code other}.
     * When {@code other} is a direction-relative inset it cannot be resolved
     * without a text direction, so only the absolute component contributes.
     */
    /**
     * Interpolates between two insets -- Flutter's {@code EdgeInsets.lerp}.
     *
     * <p>Two directional insets interpolate to a directional one, so an animation
     * between them does not silently become left-to-right at the first frame. Anything
     * else interpolates as LTRB, which is what a mixed pair resolves to here anyway --
     * see {@link EdgeInsetsDirectional}, which stores its LTR mapping.</p>
     */
    public static EdgeInsets lerp(EdgeInsets a, EdgeInsets b, double t) {
        if (a == null && b == null) {
            return null;
        }
        if (a == null) {
            a = zero;
        }
        if (b == null) {
            b = zero;
        }
        if (a instanceof EdgeInsetsDirectional && b instanceof EdgeInsetsDirectional) {
            EdgeInsetsDirectional da = (EdgeInsetsDirectional) a;
            EdgeInsetsDirectional db = (EdgeInsetsDirectional) b;
            return EdgeInsetsDirectional.fromSTEB(
                    da.start() + (db.start() - da.start()) * t,
                    da.top() + (db.top() - da.top()) * t,
                    da.end() + (db.end() - da.end()) * t,
                    da.bottom() + (db.bottom() - da.bottom()) * t);
        }
        return fromLTRB(a.left() + (b.left() - a.left()) * t,
                a.top() + (b.top() - a.top()) * t,
                a.right() + (b.right() - a.right()) * t,
                a.bottom() + (b.bottom() - a.bottom()) * t);
    }

    public EdgeInsets add(EdgeInsetsGeometry other) {
        if (other instanceof EdgeInsets) {
            EdgeInsets o = (EdgeInsets) other;
            return new EdgeInsets(left + o.left, top + o.top, right + o.right, bottom + o.bottom);
        }
        return this;
    }

    public double horizontal() {
        return left + right;
    }

    public double vertical() {
        return top + bottom;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EdgeInsets)) {
            return false;
        }
        EdgeInsets e = (EdgeInsets) o;
        return e.left == left && e.top == top && e.right == right && e.bottom == bottom;
    }

    @Override
    public int hashCode() {
        long bits = ValueHash.bits(left);
        bits = bits * 31 + ValueHash.bits(top);
        bits = bits * 31 + ValueHash.bits(right);
        bits = bits * 31 + ValueHash.bits(bottom);
        return (int) (bits ^ (bits >>> 32));
    }

    @Override
    public String toString() {
        return "EdgeInsets(" + left + ", " + top + ", " + right + ", " + bottom + ")";
    }
}
