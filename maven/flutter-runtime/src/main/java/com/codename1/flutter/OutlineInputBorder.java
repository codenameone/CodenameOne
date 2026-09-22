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

/** A rounded-rectangle outline drawn around a Material text field — Flutter's {@code OutlineInputBorder}. */
public class OutlineInputBorder extends InputBorder {

    private Object borderRadius;
    private double gapPadding = 4.0;

    public void borderRadius(Object v) {
        this.borderRadius = v;
    }

    public void gapPadding(double v) {
        this.gapPadding = v;
    }

    public Object getBorderRadius() {
        return borderRadius;
    }

    public double getGapPadding() {
        return gapPadding;
    }

    // Dart-getter-named accessors the shrine study's CutCornersBorder reads off
    // `this`/`super` (Dart `get borderSide` / `borderRadius` / `gapPadding`).

    public BorderSide borderSide() {
        return borderSide;
    }

    public BorderRadius borderRadius() {
        if (borderRadius instanceof BorderRadius) {
            return (BorderRadius) borderRadius;
        }
        // Material's default, rather than null.
        //
        // Flutter states this as a DEFAULT PARAMETER VALUE, and a subclass that
        // inherits it through `super.borderRadius = ...` does not necessarily
        // carry it across the transpile -- Shrine's CutCornersBorder is exactly
        // that, and its fields came out with square corners against the
        // reference's rounded ones. An outline input border is never square.
        return BorderRadius.circular(DEFAULT_RADIUS_LP);
    }

    /** Flutter's {@code OutlineInputBorder} default corner radius. */
    private static final double DEFAULT_RADIUS_LP = 4;

    public double gapPadding() {
        return gapPadding;
    }

    /**
     * Dart's {@code ShapeBorder.lerpFrom} / {@code lerpTo}: interpolate this
     * border to/from another. The base outline has no distinctive geometry to
     * blend here, so the fallback returns null (subclasses like CutCornersBorder
     * override with their own blend).
     */
    public ShapeBorder lerpFrom(ShapeBorder a, double t) {
        return null;
    }

    public ShapeBorder lerpTo(ShapeBorder b, double t) {
        return null;
    }
}
