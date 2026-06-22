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

/// A networked MVT (Mapbox Vector Tile) source. Point it at any
/// `{z}/{x}/{y}.pbf`/`.mvt` endpoint (MapLibre/OpenMapTiles, Protomaps,
/// MapTiler, ...). Most hosted vector basemaps require an API key, supplied
/// through [HttpTileSource#setApiKey] and referenced as `{key}` in the URL.
///
/// For a keyless, self-hostable basemap, Protomaps `.pmtiles` served behind a
/// `z/x/y` proxy works well; for fully offline maps use [BundledTileSource].
public final class MvtTileSource extends HttpTileSource {

    /// Creates a vector source from a `{z}/{x}/{y}` URL template.
    public MvtTileSource(String urlTemplate, int minZoom, int maxZoom) {
        super(urlTemplate, true, minZoom, maxZoom);
    }

    /// The free, keyless [OpenFreeMap](https://openfreemap.org) vector basemap,
    /// built from OpenStreetMap data. No API key or sign-up is required. Its
    /// tile URLs are versioned, so the source is given OpenFreeMap's TileJSON
    /// URL (no `{z}` token) and resolves the current tile template from it on
    /// first use. Works out of the box with [MapStyle#light()] / [MapStyle#dark()].
    public static MvtTileSource openFreeMap() {
        MvtTileSource s = new MvtTileSource("https://tiles.openfreemap.org/planet", 0, 14);
        s.setAttribution("(c) OpenStreetMap contributors, (c) OpenFreeMap");
        return s;
    }
}

