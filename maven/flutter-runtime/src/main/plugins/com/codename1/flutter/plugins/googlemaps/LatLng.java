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

/** google_maps_flutter's LatLng. */
public class LatLng {
    private final double latitude;
    private final double longitude;

    public LatLng(double latitude, double longitude) {
        this.latitude = Math.max(-90, Math.min(90, latitude));
        double lng = longitude;
        while (lng > 180) {
            lng -= 360;
        }
        while (lng < -180) {
            lng += 360;
        }
        this.longitude = lng;
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    com.codename1.maps.LatLng toCn1() {
        return new com.codename1.maps.LatLng(latitude, longitude);
    }

    static LatLng of(com.codename1.maps.LatLng l) {
        return l == null ? null : new LatLng(l.getLatitude(), l.getLongitude());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LatLng)) {
            return false;
        }
        LatLng l = (LatLng) o;
        return l.latitude == latitude && l.longitude == longitude;
    }

    @Override
    public int hashCode() {
        long b = Double.doubleToLongBits(latitude) * 31 + Double.doubleToLongBits(longitude);
        return (int) (b ^ (b >>> 32));
    }

    @Override
    public String toString() {
        return "LatLng(" + latitude + ", " + longitude + ")";
    }
}
