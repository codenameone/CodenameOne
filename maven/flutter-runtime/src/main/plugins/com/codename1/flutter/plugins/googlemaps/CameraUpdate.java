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
package com.codename1.flutter.plugins.googlemaps;

/** google_maps_flutter's CameraUpdate factories. */
public final class CameraUpdate {
    final LatLng target;
    final Double zoom;
    final double zoomBy;
    final CameraPosition position;

    private CameraUpdate(LatLng target, Double zoom, double zoomBy, CameraPosition position) {
        this.target = target;
        this.zoom = zoom;
        this.zoomBy = zoomBy;
        this.position = position;
    }

    public static CameraUpdate newLatLng(LatLng target) {
        return new CameraUpdate(target, null, 0, null);
    }

    public static CameraUpdate newLatLngZoom(LatLng target, double zoom) {
        return new CameraUpdate(target, Double.valueOf(zoom), 0, null);
    }

    public static CameraUpdate newCameraPosition(CameraPosition p) {
        return new CameraUpdate(null, null, 0, p);
    }

    public static CameraUpdate zoomIn() {
        return new CameraUpdate(null, null, 1, null);
    }

    public static CameraUpdate zoomOut() {
        return new CameraUpdate(null, null, -1, null);
    }

    public static CameraUpdate zoomTo(double zoom) {
        return new CameraUpdate(null, Double.valueOf(zoom), 0, null);
    }

    public static CameraUpdate zoomBy(double amount, Object focus) {
        return new CameraUpdate(null, null, amount, null);
    }

    /** Applies this update to {@code p}, returning the camera it moves to. */
    CameraPosition applyTo(CameraPosition p) {
        if (position != null) {
            return position;
        }
        CameraPosition out = new CameraPosition();
        out.target(target != null ? target : p.target());
        out.zoom((zoom != null ? zoom.doubleValue() : p.zoom()) + zoomBy);
        out.bearing(p.bearing());
        out.tilt(p.tilt());
        return out;
    }
}
