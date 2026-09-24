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
 * An immutable set of corner radii for a box — Flutter's {@code BorderRadius}.
 */
public final class BorderRadius extends BorderRadiusGeometry {

    public static final BorderRadius zero =
            new BorderRadius(Radius.zero, Radius.zero, Radius.zero, Radius.zero);

    private final Radius topLeft;
    private final Radius topRight;
    private final Radius bottomLeft;
    private final Radius bottomRight;

    private BorderRadius(Radius topLeft, Radius topRight, Radius bottomLeft, Radius bottomRight) {
        this.topLeft = topLeft == null ? Radius.zero : topLeft;
        this.topRight = topRight == null ? Radius.zero : topRight;
        this.bottomLeft = bottomLeft == null ? Radius.zero : bottomLeft;
        this.bottomRight = bottomRight == null ? Radius.zero : bottomRight;
    }

    public static BorderRadius all(Radius radius) {
        return new BorderRadius(radius, radius, radius, radius);
    }

    public static BorderRadius circular(double radius) {
        return all(Radius.circular(radius));
    }

    public static BorderRadius only(Radius topLeft, Radius topRight,
                                    Radius bottomLeft, Radius bottomRight) {
        return new BorderRadius(topLeft, topRight, bottomLeft, bottomRight);
    }

    public static BorderRadius vertical(Radius top, Radius bottom) {
        return new BorderRadius(top, top, bottom, bottom);
    }

    public static BorderRadius horizontal(Radius left, Radius right) {
        return new BorderRadius(left, right, left, right);
    }

    public Radius topLeft() {
        return topLeft;
    }

    public Radius topRight() {
        return topRight;
    }

    public Radius bottomLeft() {
        return bottomLeft;
    }

    public Radius bottomRight() {
        return bottomRight;
    }

    public RRect toRRect(Rect rect) {
        return RRect.fromRectAndCorners(rect, topLeft, topRight, bottomLeft, bottomRight);
    }

    /**
     * Dart's {@code BorderRadius.lerp(a, b, t)}: per-corner linear interpolation.
     * Returns null only when both inputs are null (mirroring Flutter).
     */
    public static BorderRadius lerp(BorderRadius a, BorderRadius b, double t) {
        if (a == null && b == null) return null;
        // A null end is zero, as in Flutter: the other end's corners scale by t (or
        // 1 - t). Returning it unchanged made the tween jump to, or from, fully rounded.
        if (a == null) return lerp(zero, b, t);
        if (b == null) return lerp(a, zero, t);
        return new BorderRadius(
                Radius.lerp(a.topLeft, b.topLeft, t),
                Radius.lerp(a.topRight, b.topRight, t),
                Radius.lerp(a.bottomLeft, b.bottomLeft, t),
                Radius.lerp(a.bottomRight, b.bottomRight, t));
    }
}
