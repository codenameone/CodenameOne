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

import com.codename1.io.Util;
import com.codename1.ui.CN;
import com.codename1.ui.Display;

import java.io.InputStream;

/// A [TileSource] that loads tiles from application resources bundled into the
/// app (the classpath), with no network access. It powers offline maps and,
/// crucially, the deterministic map screenshot tests: a small fixture tileset
/// is shipped as a resource and rendered identically on every run.
///
/// The resource path is a template containing the literal tokens `{z}`, `{x}`
/// and `{y}` (for example `/maptiles/{z}/{x}/{y}.mvt`).
public final class BundledTileSource implements TileSource {

    private final String pathTemplate;
    private final boolean vector;
    private final int minZoom;
    private final int maxZoom;
    private String attribution = "";

    /// Creates a bundled source.
    ///
    /// #### Parameters
    ///
    /// - `pathTemplate`: a resource path containing `{z}`/`{x}`/`{y}` tokens
    ///
    /// - `vector`: true for MVT tiles, false for raster image tiles
    ///
    /// - `minZoom`: the smallest available zoom
    ///
    /// - `maxZoom`: the largest available zoom
    public BundledTileSource(String pathTemplate, boolean vector, int minZoom, int maxZoom) {
        this.pathTemplate = pathTemplate;
        this.vector = vector;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
    }

    /// Sets the attribution string shown over the map.
    public BundledTileSource setAttribution(String attribution) {
        this.attribution = attribution;
        return this;
    }

    /// {@inheritDoc}
    public boolean isVector() {
        return vector;
    }

    /// {@inheritDoc}
    public int getTileSize() {
        return WebMercator.TILE_SIZE;
    }

    /// {@inheritDoc}
    public int getMinZoom() {
        return minZoom;
    }

    /// {@inheritDoc}
    public int getMaxZoom() {
        return maxZoom;
    }

    /// {@inheritDoc}
    public String getAttribution() {
        return attribution;
    }

    /// {@inheritDoc}
    public void fetchTile(final int z, final int x, final int y, final TileCallback callback) {
        final String path = resolve(z, x, y);
        CN.callSerially(new Runnable() {
            public void run() {
                byte[] data = null;
                try {
                    InputStream is = Display.getInstance().getResourceAsStream(
                            BundledTileSource.this.getClass(), path);
                    if (is != null) {
                        try {
                            data = TileUtil.maybeGunzip(Util.readInputStream(is));
                        } finally {
                            is.close();
                        }
                    }
                } catch (Throwable t) {
                    data = null;
                }
                if (data != null) {
                    callback.tileLoaded(z, x, y, data);
                } else {
                    callback.tileFailed(z, x, y);
                }
            }
        });
    }

    private String resolve(int z, int x, int y) {
        String s = pathTemplate;
        s = replace(s, "{z}", Integer.toString(z));
        s = replace(s, "{x}", Integer.toString(x));
        s = replace(s, "{y}", Integer.toString(y));
        return s;
    }

    private static String replace(String src, String token, String value) {
        int idx = src.indexOf(token);
        if (idx < 0) {
            return src;
        }
        return src.substring(0, idx) + value + src.substring(idx + token.length());
    }
}
