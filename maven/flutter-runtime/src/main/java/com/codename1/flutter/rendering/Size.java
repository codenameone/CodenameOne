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
package com.codename1.flutter.rendering;

import com.codename1.flutter.Offset;

/**
 * An immutable width/height pair, in the same unit as the constraints that
 * produced it (device pixels at runtime, raw logical values in unit tests).
 */
public final class Size {

    public static final Size ZERO = new Size(0, 0);

    private final double width;
    private final double height;

    public Size(double width, double height) {
        this.width = width;
        this.height = height;
    }

    /** {@code Size.fromRadius}: a square that bounds a circle of {@code radius}. */
    public static Size fromRadius(double radius) {
        return new Size(radius * 2, radius * 2);
    }

    /** {@code Size.fromHeight}: a fixed height, unbounded width. */
    public static Size fromHeight(double height) {
        return new Size(Double.POSITIVE_INFINITY, height);
    }

    /** {@code Size.fromWidth}: a fixed width, unbounded height. */
    public static Size fromWidth(double width) {
        return new Size(width, Double.POSITIVE_INFINITY);
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    /** {@code Size.shortestSide}: the lesser of {@link #width()} and {@link #height()}. */
    public double shortestSide() {
        return Math.min(width, height);
    }

    /** {@code Size.longestSide}: the greater of {@link #width()} and {@link #height()}. */
    public double longestSide() {
        return Math.max(width, height);
    }

    /**
     * {@code Size.center}: the offset to the center of this size, given a
     * top-left {@code origin}.
     */
    public Offset center(Offset origin) {
        return new Offset(origin.dx() + width / 2, origin.dy() + height / 2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Size)) {
            return false;
        }
        Size s = (Size) o;
        return s.width == width && s.height == height;
    }

    @Override
    public int hashCode() {
        long bits = com.codename1.flutter.ValueHash.bits(width) * 31 + com.codename1.flutter.ValueHash.bits(height);
        return (int) (bits ^ (bits >>> 32));
    }

    @Override
    public String toString() {
        return "Size(" + width + ", " + height + ")";
    }
}
