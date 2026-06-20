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

import com.codename1.maps.LatLng;
import com.codename1.maps.MapBounds;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Point;
import com.codename1.util.MathUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The pure-Codename One map renderer behind [com.codename1.maps.MapView] (and
/// the [com.codename1.maps.NativeMap] fallback).
///
/// It maintains the camera (center + fractional zoom), pulls tiles from a
/// [TileSource], rasterizes vector tiles once into 256px buffers (or decodes
/// raster tiles), caches them in an LRU [TileCache], and on each paint blits
/// the visible buffers scaled to the fractional zoom and places labels with
/// the [LabelEngine]. All drawing uses the framework [Graphics] API -- there
/// is no native peer.
public final class VectorMapEngine {

    private static final int tileSize = WebMercator.TILE_SIZE;
    private TileSource source;
    private MapStyle style;

    private double centerLat;
    private double centerLon;
    private double zoom = 2;

    private int viewWidth;
    private int viewHeight;

    private final TileCache rendered;
    private final Map labels = new HashMap();
    private final Map pending = new HashMap();
    private final Map failed = new HashMap();
    private final LabelEngine labelEngine = new LabelEngine();

    private Runnable repaintCallback;

    /// Creates an engine over `source`, styled by `style`.
    public VectorMapEngine(TileSource source, MapStyle style) {
        this.source = source;
        this.style = style == null ? MapStyle.light() : style;
        this.rendered = new TileCache(256);
    }

    /// Sets the callback invoked (on the EDT) whenever a tile finishes loading
    /// and the map needs to repaint.
    public void setRepaintCallback(Runnable r) {
        this.repaintCallback = r;
    }

    /// Replaces the tile source, clearing cached tiles.
    public void setSource(TileSource source) {
        this.source = source;
        rendered.clear();
        labels.clear();
        pending.clear();
        failed.clear();
    }

    /// The active tile source.
    public TileSource getSource() {
        return source;
    }

    /// Replaces the style, clearing rendered tiles so they redraw.
    public void setStyle(MapStyle style) {
        this.style = style;
        rendered.clear();
        labels.clear();
    }

    /// The active style.
    public MapStyle getStyle() {
        return style;
    }

    // ---- Camera -----------------------------------------------------------

    /// Recenters the camera at `center`, keeping the current zoom.
    public void setCenter(LatLng center) {
        this.centerLat = center.getLatitude();
        this.centerLon = center.getLongitude();
    }

    /// The geographic coordinate at the center of the viewport.
    public LatLng getCenter() {
        return new LatLng(centerLat, centerLon);
    }

    /// Sets the zoom level, clamped to the source's min/max.
    public void setZoom(double zoom) {
        this.zoom = clampZoom(zoom);
    }

    /// The current fractional zoom level.
    public double getZoom() {
        return zoom;
    }

    /// The smallest zoom level the tile source serves.
    public double getMinZoom() {
        return source.getMinZoom();
    }

    /// The largest zoom level the tile source serves.
    public double getMaxZoom() {
        return source.getMaxZoom();
    }

    /// Sets the pixel size of the viewport (called by the host component on
    /// layout before painting and coordinate conversion).
    public void setViewport(int width, int height) {
        this.viewWidth = width;
        this.viewHeight = height;
    }

    private double clampZoom(double z) {
        double min = source.getMinZoom();
        double max = source.getMaxZoom();
        if (z < min) {
            return min;
        }
        if (z > max) {
            return max;
        }
        return z;
    }

    // ---- Coordinate conversion -------------------------------------------

    /// Geographic to component-relative pixel.
    public Point latLngToScreen(LatLng coord) {
        double cwx = WebMercator.lonToWorldX(centerLon, zoom);
        double cwy = WebMercator.latToWorldY(centerLat, zoom);
        double wx = WebMercator.lonToWorldX(coord.getLongitude(), zoom);
        double wy = WebMercator.latToWorldY(coord.getLatitude(), zoom);
        int sx = (int) Math.floor(wx - cwx + viewWidth / 2.0 + 0.5);
        int sy = (int) Math.floor(wy - cwy + viewHeight / 2.0 + 0.5);
        return new Point(sx, sy);
    }

    /// Pans the camera by a pixel delta (used for drag gestures). A positive
    /// `dx` moves the map content to the right.
    public void panPixels(double dx, double dy) {
        double cwx = WebMercator.lonToWorldX(centerLon, zoom) - dx;
        double cwy = WebMercator.latToWorldY(centerLat, zoom) - dy;
        centerLon = WebMercator.worldXToLon(cwx, zoom);
        double lat = WebMercator.worldYToLat(cwy, zoom);
        if (lat > 85.05112878) {
            lat = 85.05112878;
        } else if (lat < -85.05112878) {
            lat = -85.05112878;
        }
        centerLat = lat;
    }

    /// Zooms to `newZoom` while keeping the geographic point currently under
    /// the component pixel `sx,sy` fixed (used for pinch and double-tap).
    public void zoomAround(double newZoom, int sx, int sy) {
        LatLng anchor = screenToLatLng(sx, sy);
        setZoom(newZoom);
        Point p = latLngToScreen(anchor);
        panPixels(sx - p.getX(), sy - p.getY());
    }

    /// Component-relative pixel to geographic.
    public LatLng screenToLatLng(int x, int y) {
        double cwx = WebMercator.lonToWorldX(centerLon, zoom);
        double cwy = WebMercator.latToWorldY(centerLat, zoom);
        double wx = x - viewWidth / 2.0 + cwx;
        double wy = y - viewHeight / 2.0 + cwy;
        return new LatLng(WebMercator.worldYToLat(wy, zoom), WebMercator.worldXToLon(wx, zoom));
    }

    /// The geographic bounds currently visible, or null before layout.
    public MapBounds getVisibleBounds() {
        if (viewWidth <= 0 || viewHeight <= 0) {
            return null;
        }
        LatLng nw = screenToLatLng(0, 0);
        LatLng se = screenToLatLng(viewWidth, viewHeight);
        return new MapBounds(new LatLng(se.getLatitude(), nw.getLongitude()),
                new LatLng(nw.getLatitude(), se.getLongitude()));
    }

    /// Moves the camera so `bounds` fits the viewport inset by `padding`.
    public void fitBounds(MapBounds bounds, int padding) {
        if (bounds == null || viewWidth <= 0 || viewHeight <= 0) {
            return;
        }
        double usableW = Math.max(1, viewWidth - 2 * padding);
        double usableH = Math.max(1, viewHeight - 2 * padding);
        double worldW = worldSpanX(bounds);
        double worldH = worldSpanY(bounds);
        double zx = worldW <= 0 ? getMaxZoom() : log2(usableW / worldW);
        double zy = worldH <= 0 ? getMaxZoom() : log2(usableH / worldH);
        setZoom(Math.min(zx, zy));
        setCenter(bounds.getCenter());
    }

    private double worldSpanX(MapBounds b) {
        double x0 = WebMercator.lonToWorldX(b.getSouthWest().getLongitude(), 0);
        double x1 = WebMercator.lonToWorldX(b.getNorthEast().getLongitude(), 0);
        return Math.abs(x1 - x0);
    }

    private double worldSpanY(MapBounds b) {
        double y0 = WebMercator.latToWorldY(b.getSouthWest().getLatitude(), 0);
        double y1 = WebMercator.latToWorldY(b.getNorthEast().getLatitude(), 0);
        return Math.abs(y1 - y0);
    }

    private static double log2(double v) {
        return MathUtil.log(v) / MathUtil.log(2);
    }

    // ---- Painting ---------------------------------------------------------

    /// Paints the basemap and labels into `g`, offset by `originX,originY`.
    /// The caller is responsible for clipping to the component bounds and for
    /// drawing overlays (markers/shapes) afterwards.
    public void paint(Graphics g, int originX, int originY, int width, int height) {
        viewWidth = width;
        viewHeight = height;

        int bg = style.getBackgroundColor();
        g.setAlpha(255);
        g.setColor(bg & 0xffffff);
        g.fillRect(originX, originY, width, height);

        int z = integerZoom();
        double s = MathUtil.pow(2, zoom - z);
        double cwx = WebMercator.lonToWorldX(centerLon, zoom);
        double cwy = WebMercator.latToWorldY(centerLat, zoom);

        int tiles = 1 << z;
        double wzLeft = (cwx - width / 2.0) / s;
        double wzRight = (cwx + width / 2.0) / s;
        double wzTop = (cwy - height / 2.0) / s;
        double wzBottom = (cwy + height / 2.0) / s;

        int txMin = floorDiv((int) Math.floor(wzLeft), tileSize);
        int txMax = floorDiv((int) Math.floor(wzRight), tileSize);
        int tyMin = floorDiv((int) Math.floor(wzTop), tileSize);
        int tyMax = floorDiv((int) Math.floor(wzBottom), tileSize);

        List visibleLabels = new ArrayList();

        for (int tx = txMin; tx <= txMax; tx++) {
            for (int ty = tyMin; ty <= tyMax; ty++) {
                if (ty < 0 || ty >= tiles) {
                    continue;
                }
                int wrappedTx = ((tx % tiles) + tiles) % tiles;
                String key = TileUtil.key(z, wrappedTx, ty);
                Image img = (Image) rendered.get(key);
                if (img == null) {
                    requestTile(z, wrappedTx, ty);
                } else {
                    int left = screenX(tx * (double) tileSize, s, cwx, originX, width);
                    int top = screenY(ty * (double) tileSize, s, cwy, originY, height);
                    int right = screenX((tx + 1) * (double) tileSize, s, cwx, originX, width);
                    int bottom = screenY((ty + 1) * (double) tileSize, s, cwy, originY, height);
                    g.drawImage(img, left, top, right - left, bottom - top);
                }
                List tileLabels = (List) labels.get(key);
                if (tileLabels != null) {
                    visibleLabels.addAll(tileLabels);
                }
            }
        }

        drawLabels(g, visibleLabels, s, cwx, cwy, originX, originY, width, height, z);
    }

    private void drawLabels(Graphics g, List candidates, double s, double cwx, double cwy,
                            int originX, int originY, int width, int height, int z) {
        labelEngine.reset();
        for (Object cand : candidates) {
            LabelCandidate c = (LabelCandidate) cand;
            // Candidate world coords are at its own tile zoom; rescale to this z.
            double factor = MathUtil.pow(2, z - c.tileZoom);
            double wzx = c.worldX * factor;
            double wzy = c.worldY * factor;
            int sx = (int) Math.floor(originX + wzx * s - cwx + width / 2.0 + 0.5);
            int sy = (int) Math.floor(originY + wzy * s - cwy + height / 2.0 + 0.5);
            if (sx < originX - 64 || sx > originX + width + 64
                    || sy < originY - 32 || sy > originY + height + 32) {
                continue;
            }
            labelEngine.place(g, c.text, c.sizePx, c.textColor, c.haloColor, sx, sy);
        }
    }

    private int screenX(double worldZ, double s, double cwx, int originX, int width) {
        return (int) Math.floor(originX + worldZ * s - cwx + width / 2.0 + 0.5);
    }

    private int screenY(double worldZ, double s, double cwy, int originY, int height) {
        return (int) Math.floor(originY + worldZ * s - cwy + height / 2.0 + 0.5);
    }

    private int integerZoom() {
        int z = (int) Math.floor(zoom + 0.5);
        if (z < source.getMinZoom()) {
            z = source.getMinZoom();
        }
        if (z > source.getMaxZoom()) {
            z = source.getMaxZoom();
        }
        if (z < 0) {
            z = 0;
        }
        return z;
    }

    private static int floorDiv(int a, int b) {
        int q = a / b;
        if ((a % b != 0) && ((a < 0) != (b < 0))) {
            q--;
        }
        return q;
    }

    // ---- Tile loading -----------------------------------------------------

    private void requestTile(final int z, final int x, final int y) {
        final String key = TileUtil.key(z, x, y);
        if (pending.containsKey(key) || failed.containsKey(key)) {
            return;
        }
        pending.put(key, Boolean.TRUE);
        source.fetchTile(z, x, y, new TileCallback() {
            @Override
            public void tileLoaded(int tz, int tx, int ty, byte[] data) {
                pending.remove(key);
                try {
                    if (source.isVector()) {
                        VectorTile tile = MvtDecoder.decode(data);
                        rendered.put(key, rasterize(tile, tz));
                        labels.put(key, TileRenderer.extractLabels(tile, style, tz, tx, ty, tileSize));
                    } else {
                        rendered.put(key, Image.createImage(data, 0, data.length));
                    }
                    repaint();
                } catch (Throwable t) {
                    failed.put(key, Boolean.TRUE);
                }
            }

            @Override
            public void tileFailed(int tz, int tx, int ty) {
                pending.remove(key);
                failed.put(key, Boolean.TRUE);
            }
        });
    }

    private Image rasterize(VectorTile tile, int z) {
        Image buffer = Image.createImage(tileSize, tileSize, 0);
        Graphics g = buffer.getGraphics();
        TileRenderer.renderTile(g, tile, style, z, tileSize);
        return buffer;
    }

    private void repaint() {
        if (repaintCallback != null) {
            repaintCallback.run();
        }
    }

    /// Drops all cached tiles (e.g. on low memory).
    public void clearCache() {
        rendered.clear();
        labels.clear();
        failed.clear();
    }
}
