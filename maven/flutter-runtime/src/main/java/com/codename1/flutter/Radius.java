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
 * A radius for the corner of a rounded rectangle, with independent x and y
 * components — Flutter's dart:ui {@code Radius}.
 */
public final class Radius {

    public static final Radius zero = new Radius(0, 0);

    private final double x;
    private final double y;

    private Radius(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public static Radius circular(double radius) {
        return new Radius(radius, radius);
    }

    public static Radius elliptical(double x, double y) {
        return new Radius(x, y);
    }

    /** Dart's {@code Radius.lerp(a, b, t)}: per-component linear interpolation. */
    public static Radius lerp(Radius a, Radius b, double t) {
        if (a == null && b == null) return null;
        if (a == null) return new Radius(b.x * t, b.y * t);
        if (b == null) return new Radius(a.x * (1.0 - t), a.y * (1.0 - t));
        return new Radius(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Radius)) {
            return false;
        }
        Radius r = (Radius) o;
        return r.x == x && r.y == y;
    }

    @Override
    public int hashCode() {
        long bits = ValueHash.bits(x) * 31 + ValueHash.bits(y);
        return (int) (bits ^ (bits >>> 32));
    }

    @Override
    public String toString() {
        return "Radius.elliptical(" + x + ", " + y + ")";
    }
}
