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

/// Supplies tile payloads to the [VectorMapEngine] given a slippy-map
/// `z/x/y` address. Two flavors exist: vector sources ([#isVector] true)
/// returning MVT protobuf bytes that the engine decodes and styles, and
/// raster sources returning encoded-image bytes that the engine simply
/// blits. The bundled [BundledTileSource], the networked [MvtTileSource] and
/// the keyless [RasterTileSource] cover the universal, vector and zero-config
/// cases respectively.
public interface TileSource {

    /// True for MVT vector tiles, false for raster image tiles.
    boolean isVector();

    /// The tile edge in pixels (almost always 256).
    int getTileSize();

    /// The smallest zoom level this source serves.
    int getMinZoom();

    /// The largest zoom level this source serves.
    int getMaxZoom();

    /// Attribution text that must be displayed over the map.
    String getAttribution();

    /// Requests the tile at `z/x/y`, delivering the result to `callback` on
    /// the event dispatch thread.
    void fetchTile(int z, int x, int y, TileCallback callback);
}
