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
package dart.typed_data;

/// Dart's {@code Float64x2}: two double-precision lanes, immutable.
public final class Float64x2 {

    final double x;
    final double y;

    public Float64x2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public static Float64x2 splat(double v) {
        return new Float64x2(v, v);
    }

    public static Float64x2 zero() {
        return new Float64x2(0, 0);
    }

    /// The x and y lanes of {@code v}, widened.
    public static Float64x2 fromFloat32x4(Float32x4 v) {
        return new Float64x2(v.x, v.y);
    }

    public double x() { return x; }
    public double y() { return y; }

    public long signMask() {
        return (Double.doubleToRawLongBits(x) >>> 63) | ((Double.doubleToRawLongBits(y) >>> 63) << 1);
    }

    public Float64x2 $plus(Float64x2 o) { return new Float64x2(x + o.x, y + o.y); }
    public Float64x2 $minus(Float64x2 o) { return new Float64x2(x - o.x, y - o.y); }
    public Float64x2 $minus() { return new Float64x2(-x, -y); }
    public Float64x2 $times(Float64x2 o) { return new Float64x2(x * o.x, y * o.y); }
    public Float64x2 $div(Float64x2 o) { return new Float64x2(x / o.x, y / o.y); }
    public Float64x2 scale(double s) { return new Float64x2(x * s, y * s); }
    public Float64x2 abs() { return new Float64x2(Math.abs(x), Math.abs(y)); }

    public Float64x2 clamp(Float64x2 lo, Float64x2 hi) {
        return new Float64x2(x < lo.x ? lo.x : x > hi.x ? hi.x : x, y < lo.y ? lo.y : y > hi.y ? hi.y : y);
    }

    public Float64x2 withX(double v) { return new Float64x2(v, y); }
    public Float64x2 withY(double v) { return new Float64x2(x, v); }
    public Float64x2 min(Float64x2 o) { return new Float64x2(Math.min(x, o.x), Math.min(y, o.y)); }
    public Float64x2 max(Float64x2 o) { return new Float64x2(Math.max(x, o.x), Math.max(y, o.y)); }
    public Float64x2 sqrt() { return new Float64x2(Math.sqrt(x), Math.sqrt(y)); }

    @Override
    public String toString() {
        return "[" + Float32x4.lane6(x) + ", " + Float32x4.lane6(y) + "]";
    }
}
