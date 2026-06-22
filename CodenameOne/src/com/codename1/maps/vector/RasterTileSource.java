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

/// A networked raster (image) XYZ tile source. Use for keyless, zero-config
/// basemaps such as OpenStreetMap; the engine simply decodes and blits the
/// returned image rather than styling vector geometry.
public final class RasterTileSource extends HttpTileSource {

    /// Creates a raster source from a `{z}/{x}/{y}` image URL template.
    public RasterTileSource(String urlTemplate, int minZoom, int maxZoom) {
        super(urlTemplate, false, minZoom, maxZoom);
    }

    /// The standard OpenStreetMap raster basemap (HTTPS, keyless). Subject to
    /// the OSM tile usage policy; supply a real tileset for production traffic.
    public static RasterTileSource openStreetMap() {
        RasterTileSource s = new RasterTileSource(
                "https://tile.openstreetmap.org/{z}/{x}/{y}.png", 0, 19);
        s.setAttribution("(c) OpenStreetMap contributors");
        return s;
    }
}
