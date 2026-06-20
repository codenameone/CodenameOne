/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps;

/// An immutable description of the map camera: where it looks ([#getTarget]),
/// how far it is zoomed in ([#getZoom]), the compass bearing in degrees
/// ([#getBearing]) and the tilt away from nadir ([#getTilt]).
///
/// Zoom uses the standard slippy-map scale where each whole increment
/// doubles the scale (zoom 0 shows the whole world in a single 256px tile).
/// Fractional zoom is supported by the vector engine; native providers may
/// round it. Bearing and tilt are honored by native providers that support
/// them and ignored by the pure-vector [MapView].
public final class CameraPosition {

    private final LatLng target;
    private final double zoom;
    private final double bearing;
    private final double tilt;

    /// Creates a camera position that looks straight down (no bearing/tilt).
    public CameraPosition(LatLng target, double zoom) {
        this(target, zoom, 0, 0);
    }

    /// Creates a fully specified camera position.
    ///
    /// #### Parameters
    ///
    /// - `target`: the geographic point at the center of the viewport
    ///
    /// - `zoom`: the slippy-map zoom level
    ///
    /// - `bearing`: the compass bearing in degrees (0 = north up)
    ///
    /// - `tilt`: the viewing angle away from straight-down, in degrees
    public CameraPosition(LatLng target, double zoom, double bearing, double tilt) {
        this.target = target;
        this.zoom = zoom;
        this.bearing = bearing;
        this.tilt = tilt;
    }

    /// The geographic point at the center of the viewport.
    public LatLng getTarget() {
        return target;
    }

    /// The slippy-map zoom level.
    public double getZoom() {
        return zoom;
    }

    /// The compass bearing in degrees (0 = north up).
    public double getBearing() {
        return bearing;
    }

    /// The viewing tilt in degrees (0 = straight down).
    public double getTilt() {
        return tilt;
    }

    /// Returns a copy of this position with a different target.
    public CameraPosition withTarget(LatLng newTarget) {
        return new CameraPosition(newTarget, zoom, bearing, tilt);
    }

    /// Returns a copy of this position with a different zoom level.
    public CameraPosition withZoom(double newZoom) {
        return new CameraPosition(target, newZoom, bearing, tilt);
    }

    /// {@inheritDoc}
    @Override
    public String toString() {
        return "CameraPosition{target=" + target + ", zoom=" + zoom
                + ", bearing=" + bearing + ", tilt=" + tilt + "}";
    }
}
