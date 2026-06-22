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

import com.codename1.io.CharArrayReader;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// A [TileSource] that fetches tiles over HTTPS from a slippy-map URL
/// template. The template contains `{z}`/`{x}`/`{y}` tokens and, optionally,
/// a `{key}` token substituted with the configured API key. Downloads run on
/// the Codename One network thread and deliver results on the EDT, with
/// transparent gunzip for vector payloads.
///
/// When the URL has no `{z}` token it is treated as a *TileJSON* endpoint: on
/// first use the source fetches that document, reads its `tiles` template and
/// then serves tiles from it. This is how the keyless OpenFreeMap basemap
/// (whose tile URLs are versioned) is supported -- see
/// [MvtTileSource#openFreeMap()].
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

    // TileJSON resolution: when urlTemplate carries no {z} token it is a
    // TileJSON document URL whose `tiles` template we resolve once, queueing
    // any tile requests that arrive while resolution is in flight.
    private String resolvedTemplate;
    private boolean resolving;
    private final List pendingRequests = new ArrayList();

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
    @Override
    public boolean isVector() {
        return vector;
    }

    /// {@inheritDoc}
    @Override
    public int getTileSize() {
        return WebMercator.TILE_SIZE;
    }

    /// {@inheritDoc}
    @Override
    public int getMinZoom() {
        return minZoom;
    }

    /// {@inheritDoc}
    @Override
    public int getMaxZoom() {
        return maxZoom;
    }

    /// {@inheritDoc}
    @Override
    public String getAttribution() {
        return attribution;
    }

    /// {@inheritDoc}
    @Override
    public void fetchTile(int z, int x, int y, TileCallback callback) {
        if (needsTileJson()) {
            synchronized (this) {
                if (resolvedTemplate == null) {
                    pendingRequests.add(new Object[]{Integer.valueOf(z), Integer.valueOf(x),
                            Integer.valueOf(y), callback});
                    if (!resolving) {
                        resolving = true;
                        resolveTileJson();
                    }
                    return;
                }
            }
        }
        doFetch(z, x, y, callback);
    }

    private boolean needsTileJson() {
        return urlTemplate.indexOf("{z}") < 0;
    }

    private void doFetch(int z, int x, int y, TileCallback callback) {
        TileRequest req = new TileRequest(z, x, y, callback);
        req.setUrl(resolve(z, x, y));
        req.setPost(false);
        req.setFailSilently(true);
        NetworkManager.getInstance().addToQueue(req);
    }

    private String resolve(int z, int x, int y) {
        String resolved;
        synchronized (this) {
            resolved = resolvedTemplate;
        }
        String s = resolved != null ? resolved : urlTemplate;
        s = replace(s, "{z}", Integer.toString(z));
        s = replace(s, "{x}", Integer.toString(x));
        s = replace(s, "{y}", Integer.toString(y));
        s = replace(s, "{key}", apiKey);
        return s;
    }

    private void resolveTileJson() {
        ConnectionRequest req = new ConnectionRequest() {
            private byte[] body;

            @Override
            protected void readResponse(InputStream input) throws IOException {
                body = Util.readInputStream(input);
            }

            @Override
            protected void postResponse() {
                String tiles = body == null ? null : parseTileJsonTemplate(body);
                List drain;
                synchronized (HttpTileSource.this) {
                    resolvedTemplate = tiles;
                    resolving = false;
                    drain = new ArrayList(pendingRequests);
                    pendingRequests.clear();
                }
                for (Object drainItem : drain) {
                    Object[] r = (Object[]) drainItem;
                    TileCallback cb = (TileCallback) r[3];
                    if (tiles == null) {
                        cb.tileFailed(((Integer) r[0]).intValue(),
                                ((Integer) r[1]).intValue(), ((Integer) r[2]).intValue());
                    } else {
                        doFetch(((Integer) r[0]).intValue(), ((Integer) r[1]).intValue(),
                                ((Integer) r[2]).intValue(), cb);
                    }
                }
            }

            @Override
            protected void handleException(Exception err) {
                failAllPending();
            }

            @Override
            protected void handleErrorResponseCode(int code, String message) {
                failAllPending();
            }
        };
        req.setUrl(replace(urlTemplate, "{key}", apiKey));
        req.setPost(false);
        req.setFailSilently(true);
        NetworkManager.getInstance().addToQueue(req);
    }

    private void failAllPending() {
        List drain;
        synchronized (this) {
            resolving = false;
            drain = new ArrayList(pendingRequests);
            pendingRequests.clear();
        }
        for (Object drainItem : drain) {
            Object[] r = (Object[]) drainItem;
            ((TileCallback) r[3]).tileFailed(((Integer) r[0]).intValue(),
                    ((Integer) r[1]).intValue(), ((Integer) r[2]).intValue());
        }
    }

    private static String parseTileJsonTemplate(byte[] json) {
        try {
            Map root = new JSONParser().parseJSON(new CharArrayReader(new String(json, "UTF-8").toCharArray()));
            Object tiles = root.get("tiles");
            if (tiles instanceof List && !((List) tiles).isEmpty()) {
                return String.valueOf(((List) tiles).get(0));
            }
        } catch (Throwable t) {
            // Malformed TileJSON -> treat as unresolved.
            return null;
        }
        return null;
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

        @Override
        protected void readResponse(InputStream input) throws IOException {
            result = Util.readInputStream(input);
        }

        @Override
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

        @Override
        protected void handleException(Exception err) {
            callback.tileFailed(z, x, y);
        }

        @Override
        protected void handleErrorResponseCode(int code, String message) {
            callback.tileFailed(z, x, y);
        }
    }
}
