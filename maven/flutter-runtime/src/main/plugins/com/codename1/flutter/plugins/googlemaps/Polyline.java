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
 * google_maps_flutter's {@code Polyline}, drawn as a Codename One
 * {@code com.codename1.maps.Polyline}. Points, color, width and visibility are applied;
 * geodesic, patterns, caps, joints and zIndex are accepted and not applied. The core
 * map reports a tap as a position only, so the map element hit-tests it against the
 * segments itself (within a few millimeters, or half the stroke width if wider). As in
 * the plugin, {@code onTap} is called only for a polyline with
 * {@code consumeTapEvents} set, and such a tap does not reach the map's own onTap.
 */
public class Polyline {
    PolylineId polylineId;
    Iterable<?> points;
    Color color;
    long width = 10;
    boolean visible = true;
    Funcs.VoidFunc0 onTap;
    boolean consumeTapEvents;

    public Polyline() {
    }

    public void polylineId(PolylineId v) {
        polylineId = v;
    }

    public void points(Iterable<?> v) {
        points = v;
    }

    public void color(Color v) {
        color = v;
    }

    public void width(long v) {
        width = v;
    }

    public void visible(boolean v) {
        visible = v;
    }

    public void geodesic(boolean v) {
    }

    public void zIndex(long v) {
    }

    public void consumeTapEvents(boolean v) {
        consumeTapEvents = v;
    }

    public void patterns(Object v) {
    }

    public void startCap(Object v) {
    }

    public void endCap(Object v) {
    }

    public void jointType(Object v) {
    }

    public void onTap(Funcs.VoidFunc0 v) {
        onTap = v;
    }

    public PolylineId polylineId() {
        return polylineId;
    }

    public Iterable<?> points() {
        return points;
    }

    public long width() {
        return width;
    }

    public boolean visible() {
        return visible;
    }

    /** The core polyline this one draws as. */
    com.codename1.maps.Polyline toCn1() {
        com.codename1.maps.Polyline p = new com.codename1.maps.Polyline();
        if (points != null) {
            for (Object o : points) {
                if (o instanceof LatLng) {
                    p.addPoint(((LatLng) o).toCn1());
                }
            }
        }
        int argb = color == null ? 0xff000000 : color.argb();
        p.setStrokeColor(argb & 0xffffff);
        p.setStrokeAlpha((argb >>> 24) & 0xff);
        p.setStrokeWidth((int) Math.max(1, width));
        return p;
    }

    /// Whether {@code at} lies within {@code toleranceMeters} of the line.
    public boolean hitTest(LatLng at, double toleranceMeters) {
        if (at == null || points == null) {
            return false;
        }
        LatLng prev = null;
        for (Object o : points) {
            if (!(o instanceof LatLng)) {
                continue;
            }
            LatLng cur = (LatLng) o;
            double d = prev == null ? MapHit.distance(at, cur) : MapHit.distanceToSegment(at, prev, cur);
            if (d <= toleranceMeters) {
                return true;
            }
            prev = cur;
        }
        return false;
    }

    /// Delivers a tap at {@code at}: answers whether this polyline consumed it, calling
    /// onTap when it did.
    public boolean tapAt(LatLng at, double toleranceMeters) {
        if (!consumeTapEvents || !visible || !hitTest(at, toleranceMeters)) {
            return false;
        }
        if (onTap != null) {
            onTap.call();
        }
        return true;
    }
}
