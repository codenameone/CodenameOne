/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.plugins.googlemaps;

import com.codename1.flutter.Color;

import dart.runtime.Funcs;

/**
 * google_maps_flutter's {@code Circle}, drawn as a Codename One
 * {@code com.codename1.maps.Circle}: center, radius, fill and stroke colors, stroke width
 * and visibility are applied. zIndex is accepted and not applied. The core map reports
 * a tap as a position only, so the map element tests it against the radius itself. As
 * in the plugin, {@code onTap} is called only for a circle with {@code consumeTapEvents}
 * set, and such a tap does not reach the map's own onTap.
 */
public class Circle {
    CircleId circleId;
    LatLng center;
    double radius;
    Color fillColor;
    Color strokeColor;
    long strokeWidth = 10;
    boolean visible = true;
    Funcs.VoidFunc0 onTap;
    boolean consumeTapEvents;

    public Circle() {
    }

    public void circleId(CircleId v) {
        circleId = v;
    }

    public void center(LatLng v) {
        center = v;
    }

    public void radius(double v) {
        radius = v;
    }

    public void fillColor(Color v) {
        fillColor = v;
    }

    public void strokeColor(Color v) {
        strokeColor = v;
    }

    public void strokeWidth(long v) {
        strokeWidth = v;
    }

    public void visible(boolean v) {
        visible = v;
    }

    public void zIndex(long v) {
    }

    public void consumeTapEvents(boolean v) {
        consumeTapEvents = v;
    }

    public void onTap(Funcs.VoidFunc0 v) {
        onTap = v;
    }

    public CircleId circleId() {
        return circleId;
    }

    public LatLng center() {
        return center;
    }

    public double radius() {
        return radius;
    }

    public boolean visible() {
        return visible;
    }

    /** The core circle this one draws as; null without a center. */
    com.codename1.maps.Circle toCn1() {
        if (center == null) {
            return null;
        }
        com.codename1.maps.Circle c = new com.codename1.maps.Circle(center.toCn1(), radius);
        if (fillColor != null) {
            c.setFillColor(fillColor.argb());
        }
        if (strokeColor != null) {
            c.setStrokeColor(strokeColor.argb() & 0xffffff);
        }
        c.setStrokeWidth((int) Math.max(0, strokeWidth));
        return c;
    }

    /// Whether {@code at} lies within the circle.
    public boolean hitTest(LatLng at) {
        return at != null && center != null && MapHit.distance(center, at) <= radius;
    }

    /// Delivers a tap at {@code at}: answers whether this circle consumed it, calling
    /// onTap when it did.
    public boolean tapAt(LatLng at) {
        if (!consumeTapEvents || !visible || !hitTest(at)) {
            return false;
        }
        if (onTap != null) {
            onTap.call();
        }
        return true;
    }
}
