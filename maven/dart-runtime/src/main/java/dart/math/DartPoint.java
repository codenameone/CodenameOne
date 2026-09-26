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
package dart.math;

/**
 * Dart's {@code dart:math} {@code Point<T extends num>}. The gallery only uses
 * {@code Point<double>}, so coordinates are stored as {@code double}; the type
 * parameter {@code T} exists so the transpiler's {@code Point<double>} type
 * argument resolves.
 *
 * @param <T> the (numeric) coordinate type; phantom in this runtime
 */
public final class DartPoint<T> {

    private final double x;
    private final double y;

    public DartPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    /**
     * Value equality, as Dart's Point has: Point(1, 2) == Point(1, 2). Inherited identity
     * made every two points unequal, so equal points could not find each other's entries.
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof DartPoint && ((DartPoint<?>) o).x == x && ((DartPoint<?>) o).y == y;
    }

    /** Consistent with equals, whose == makes 0.0 and -0.0 equal: hashed alike. */
    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(x == 0.0 ? 0.0 : x) * 31
                + Double.doubleToLongBits(y == 0.0 ? 0.0 : y);
        return (int) (bits ^ (bits >>> 32));
    }

    public double distanceTo(DartPoint<T> other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
