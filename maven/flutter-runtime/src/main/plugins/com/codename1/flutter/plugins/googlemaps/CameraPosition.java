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

/** google_maps_flutter's CameraPosition. */
public class CameraPosition {
    private LatLng target;
    private double zoom;
    private double bearing;
    private double tilt;

    public CameraPosition() {
    }

    public void target(LatLng v) {
        target = v;
    }

    public void zoom(double v) {
        zoom = v;
    }

    public void bearing(double v) {
        bearing = v;
    }

    public void tilt(double v) {
        tilt = v;
    }

    public LatLng target() {
        return target;
    }

    public double zoom() {
        return zoom;
    }

    public double bearing() {
        return bearing;
    }

    public double tilt() {
        return tilt;
    }

    static CameraPosition of(com.codename1.maps.CameraPosition p) {
        CameraPosition c = new CameraPosition();
        c.target = LatLng.of(p.getTarget());
        c.zoom = p.getZoom();
        c.bearing = p.getBearing();
        c.tilt = p.getTilt();
        return c;
    }

    com.codename1.maps.CameraPosition toCn1() {
        return new com.codename1.maps.CameraPosition(target == null ? new com.codename1.maps.LatLng(0, 0)
                : target.toCn1(), zoom, bearing, tilt);
    }
}
