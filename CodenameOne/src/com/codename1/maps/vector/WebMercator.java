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
package com.codename1.maps.vector;

import com.codename1.util.MathUtil;

/// Spherical Web Mercator (EPSG:3857) helpers expressed in "world pixels" --
/// the slippy-map coordinate space where the whole world spans
/// `tileSize * 2^zoom` pixels. All of the vector engine's panning and tile
/// math is done in this space.
///
/// These are the standard OSM tiling formulas; kept here (rather than reusing
/// the projected [com.codename1.maps.Mercator]) so the renderer can work in
/// fractional pixels at fractional zoom without round-tripping through the
/// legacy `Coord` projection flag.
public final class WebMercator {

    /// The canonical tile edge length in pixels.
    public static final int TILE_SIZE = 256;

    private static final double PI = Math.PI;

    private WebMercator() {
    }

    /// The world width/height in pixels at `zoom` (may be fractional).
    public static double worldSize(double zoom) {
        return TILE_SIZE * MathUtil.pow(2, zoom);
    }

    /// Longitude in degrees to an absolute world-pixel x at `zoom`.
    public static double lonToWorldX(double lon, double zoom) {
        return (lon + 180.0) / 360.0 * worldSize(zoom);
    }

    /// Latitude in degrees to an absolute world-pixel y at `zoom`.
    public static double latToWorldY(double lat, double zoom) {
        double latRad = lat * PI / 180.0;
        double y = MathUtil.log(Math.tan(latRad) + 1.0 / Math.cos(latRad));
        return (1.0 - y / PI) / 2.0 * worldSize(zoom);
    }

    /// World-pixel x at `zoom` back to longitude in degrees.
    public static double worldXToLon(double worldX, double zoom) {
        return worldX / worldSize(zoom) * 360.0 - 180.0;
    }

    /// World-pixel y at `zoom` back to latitude in degrees.
    public static double worldYToLat(double worldY, double zoom) {
        double n = PI * (1.0 - 2.0 * worldY / worldSize(zoom));
        double latRad = MathUtil.atan(sinh(n));
        return latRad * 180.0 / PI;
    }

    /// Hyperbolic sine, absent from the minimal device `Math`.
    public static double sinh(double x) {
        return (MathUtil.exp(x) - MathUtil.exp(-x)) / 2.0;
    }
}
