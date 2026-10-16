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
 * A point within a rectangle expressed in Flutter's -1..1 coordinate system:
 * (-1,-1) is the top left, (0,0) the center, (1,1) the bottom right.
 */
public class Alignment {

    public static final Alignment topLeft = new Alignment(-1, -1);
    public static final Alignment topCenter = new Alignment(0, -1);
    public static final Alignment topRight = new Alignment(1, -1);
    public static final Alignment centerLeft = new Alignment(-1, 0);
    public static final Alignment center = new Alignment(0, 0);
    public static final Alignment centerRight = new Alignment(1, 0);
    public static final Alignment bottomLeft = new Alignment(-1, 1);
    public static final Alignment bottomCenter = new Alignment(0, 1);
    public static final Alignment bottomRight = new Alignment(1, 1);

    private final double x;
    private final double y;

    public Alignment(double x, double y) {
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
     * The offset of a child of the given extent within a parent of the given
     * extent, along one axis.
     */
    public static double along(double alignment, double parentExtent, double childExtent) {
        return (parentExtent - childExtent) * (alignment + 1) / 2;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Alignment)) {
            return false;
        }
        Alignment a = (Alignment) o;
        return a.x == x && a.y == y;
    }

    @Override
    public int hashCode() {
        long bits = ValueHash.bits(x) * 31 + ValueHash.bits(y);
        return (int) (bits ^ (bits >>> 32));
    }
}
