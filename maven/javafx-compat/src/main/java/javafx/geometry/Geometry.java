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
package javafx.geometry;

/// Value comparison and hashing of coordinates, shared by the geometry
/// classes. Coordinates are equal when `==` says so, as in JavaFX: positive
/// and negative zero are the same coordinate and NaN equals nothing.
final class Geometry {

    private Geometry() {
    }

    static boolean same(double first, double second) {
        return first == second;
    }

    static int hash(int seed, double value) {
        // Negative zero equals zero, so both must hash alike.
        long bits = Double.doubleToLongBits(value == 0.0 ? 0.0 : value);
        return 37 * seed + (int) (bits ^ (bits >>> 32));
    }
}
