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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;

import java.io.IOException;
import java.io.InputStream;

/// A [TileSource] that fetches tiles over HTTPS from a slippy-map URL
/// template. The template contains `{z}`/`{x}`/`{y}` tokens and, optionally,
/// a `{key}` token substituted with the configured API key. Downloads run on
/// the Codename One network thread and deliver results on the EDT, with
/// transparent gunzip for vector payloads.
///
/// This is the shared base for [MvtTileSource] (vector) and
/// [RasterTileSource] (raster).
public class HttpTileSource implements TileSource {

    private final String urlTemplate;
    private final boolean vector;
    private final int minZoom;
    private final int maxZoom;
    private String apiKey = "";
    private String attribution = "";

    /// Creates an HTTP tile source.
    ///
    /// #### Parameters
    ///
    /// - `urlTemplate`: a URL with `{z}`/`{x}`/`{y}` (and optional `{key}`) tokens
    ///
    /// - `vector`: true for MVT tiles, false for raster image tiles
    ///
    /// - `minZoom`: the smallest available zoom
    ///
    /// - `maxZoom`: the largest available zoom
    public HttpTileSource(String urlTemplate, boolean vector, int minZoom, int maxZoom) {
        this.urlTemplate = urlTemplate;
        this.vector = vector;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
    }

    /// Sets the API key substituted into the `{key}` token of the template.
    public HttpTileSource setApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey;
        return this;
    }

    /// Sets the attribution string shown over the map.
    public HttpTileSource setAttribution(String attribution) {
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
    public void fetchTile(int z, int x, int y, TileCallback callback) {
        TileRequest req = new TileRequest(z, x, y, callback);
        req.setUrl(resolve(z, x, y));
        req.setPost(false);
        req.setFailSilently(true);
        NetworkManager.getInstance().addToQueue(req);
    }

    private String resolve(int z, int x, int y) {
        String s = urlTemplate;
        s = replace(s, "{z}", Integer.toString(z));
        s = replace(s, "{x}", Integer.toString(x));
        s = replace(s, "{y}", Integer.toString(y));
        s = replace(s, "{key}", apiKey);
        return s;
    }

    private static String replace(String src, String token, String value) {
        int idx;
        while ((idx = src.indexOf(token)) >= 0) {
            src = src.substring(0, idx) + value + src.substring(idx + token.length());
        }
        return src;
    }

    private final class TileRequest extends ConnectionRequest {
        private final int z;
        private final int x;
        private final int y;
        private final TileCallback callback;
        private byte[] result;

        TileRequest(int z, int x, int y, TileCallback callback) {
            this.z = z;
            this.x = x;
            this.y = y;
            this.callback = callback;
        }

        protected void readResponse(InputStream input) throws IOException {
            result = Util.readInputStream(input);
        }

        protected void postResponse() {
            if (result == null || result.length == 0) {
                callback.tileFailed(z, x, y);
                return;
            }
            try {
                callback.tileLoaded(z, x, y, vector ? TileUtil.maybeGunzip(result) : result);
            } catch (Throwable t) {
                callback.tileFailed(z, x, y);
            }
        }

        protected void handleException(Exception err) {
            callback.tileFailed(z, x, y);
        }

        protected void handleErrorResponseCode(int code, String message) {
            callback.tileFailed(z, x, y);
        }
    }
}
