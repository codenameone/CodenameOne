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
 * A rounded rectangle: an axis-aligned {@link Rect} with a {@link Radius} at
 * each corner — Flutter's dart:ui {@code RRect}.
 */
public final class RRect {

    private final Rect rect;
    private final Radius topLeft;
    private final Radius topRight;
    private final Radius bottomLeft;
    private final Radius bottomRight;

    private RRect(Rect rect, Radius topLeft, Radius topRight, Radius bottomLeft, Radius bottomRight) {
        this.rect = rect;
        this.topLeft = topLeft == null ? Radius.zero : topLeft;
        this.topRight = topRight == null ? Radius.zero : topRight;
        this.bottomLeft = bottomLeft == null ? Radius.zero : bottomLeft;
        this.bottomRight = bottomRight == null ? Radius.zero : bottomRight;
    }

    public static RRect fromRectAndRadius(Rect rect, Radius radius) {
        return new RRect(rect, radius, radius, radius, radius);
    }

    public static RRect fromLTRBR(double left, double top, double right, double bottom, Radius radius) {
        return new RRect(Rect.fromLTRB(left, top, right, bottom), radius, radius, radius, radius);
    }

    public static RRect fromRectAndCorners(Rect rect, Radius topLeft, Radius topRight,
                                           Radius bottomLeft, Radius bottomRight) {
        return new RRect(rect, topLeft, topRight, bottomLeft, bottomRight);
    }

    public Rect outerRect() {
        return rect;
    }

    public Radius tlRadius() {
        return topLeft;
    }

    public Radius trRadius() {
        return topRight;
    }

    public Radius blRadius() {
        return bottomLeft;
    }

    public Radius brRadius() {
        return bottomRight;
    }

    /**
     * {@code RRect.middleRect}: the rectangle inside the rounded corners, i.e.
     * the outer rect inset on each edge by the larger of the two corner radii
     * touching that edge.
     */
    public Rect middleRect() {
        return Rect.fromLTRB(
                rect.left() + Math.max(bottomLeft.x(), topLeft.x()),
                rect.top() + Math.max(topLeft.y(), topRight.y()),
                rect.right() - Math.max(topRight.x(), bottomRight.x()),
                rect.bottom() - Math.max(bottomRight.y(), bottomLeft.y()));
    }
}
