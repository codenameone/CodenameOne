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

import com.codename1.io.grpc.ProtoWriter;
import com.codename1.ui.CN;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// A self-contained [TileSource] that synthesizes a deterministic Mapbox
/// Vector Tile in memory for every address, with no network or bundled
/// assets. Every tile carries the same recognizable content -- a water body,
/// a landuse area, two roads, a building and a labeled place -- which makes it
/// ideal for offline demos and for reproducible map screenshot tests.
public final class DemoTileSource implements TileSource {

    private static final int EXTENT = 4096;
    private byte[] cachedTile;

    /// {@inheritDoc}
    public boolean isVector() {
        return true;
    }

    /// {@inheritDoc}
    public int getTileSize() {
        return WebMercator.TILE_SIZE;
    }

    /// {@inheritDoc}
    public int getMinZoom() {
        return 0;
    }

    /// {@inheritDoc}
    public int getMaxZoom() {
        return 18;
    }

    /// {@inheritDoc}
    public String getAttribution() {
        return "Codename One demo tiles";
    }

    /// {@inheritDoc}
    public void fetchTile(final int z, final int x, final int y, final TileCallback callback) {
        CN.callSerially(new Runnable() {
            public void run() {
                try {
                    callback.tileLoaded(z, x, y, tileBytes());
                } catch (Throwable t) {
                    callback.tileFailed(z, x, y);
                }
            }
        });
    }

    private synchronized byte[] tileBytes() throws IOException {
        if (cachedTile == null) {
            cachedTile = buildTile();
        }
        return cachedTile;
    }

    /// Builds the synthetic MVT payload. Public and static so unit tests and
    /// the demo can reuse exactly the same bytes.
    public static byte[] buildTile() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ProtoWriter tile = new ProtoWriter(out);

        // water: left third of the tile.
        tile.writeBytes(3, polygonLayer("water",
                new int[]{0, 1500, 1500, 0}, new int[]{0, 0, EXTENT, EXTENT}));
        // landuse: top-right region.
        tile.writeBytes(3, polygonLayer("landuse",
                new int[]{1500, EXTENT, EXTENT, 1500}, new int[]{0, 0, 2200, 2200}));
        // road: two crossing lines.
        ByteArrayOutputStream roads = new ByteArrayOutputStream();
        ProtoWriter rl = new ProtoWriter(roads);
        rl.writeString(1, "road");
        rl.writeBytes(2, lineFeature(new int[]{0, EXTENT}, new int[]{EXTENT, 0}));
        rl.writeBytes(2, lineFeature(new int[]{0, EXTENT}, new int[]{2048, 2048}));
        rl.writeInt32(5, EXTENT);
        tile.writeBytes(3, roads.toByteArray());
        // building: small square.
        tile.writeBytes(3, polygonLayer("building",
                new int[]{2750, 3200, 3200, 2750}, new int[]{2650, 2650, 3050, 3050}));
        // place: a labeled point in the center.
        tile.writeBytes(3, placeLayer("place", "CN1 City", 2048, 2048));

        return out.toByteArray();
    }

    private static byte[] polygonLayer(String name, int[] xs, int[] ys) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ProtoWriter layer = new ProtoWriter(buf);
        layer.writeString(1, name);
        layer.writeBytes(2, polygonFeature(xs, ys));
        layer.writeInt32(5, EXTENT);
        return buf.toByteArray();
    }

    private static byte[] placeLayer(String name, String label, int x, int y) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ProtoWriter layer = new ProtoWriter(buf);
        layer.writeString(1, name);
        // feature with one tag: name -> label.
        ByteArrayOutputStream fb = new ByteArrayOutputStream();
        ProtoWriter f = new ProtoWriter(fb);
        List tags = new ArrayList();
        tags.add(new Integer(0));
        tags.add(new Integer(0));
        f.writePackedInt32(2, tags);
        f.writeInt32(3, VectorFeature.GEOM_POINT);
        f.writePackedInt32(4, pointGeometry(x, y));
        layer.writeBytes(2, fb.toByteArray());
        // keys[0] = "name"
        layer.writeString(3, "name");
        // values[0] = string label
        ByteArrayOutputStream vb = new ByteArrayOutputStream();
        ProtoWriter v = new ProtoWriter(vb);
        v.writeString(1, label);
        layer.writeBytes(4, vb.toByteArray());
        layer.writeInt32(5, EXTENT);
        return buf.toByteArray();
    }

    private static byte[] polygonFeature(int[] xs, int[] ys) throws IOException {
        ByteArrayOutputStream fb = new ByteArrayOutputStream();
        ProtoWriter f = new ProtoWriter(fb);
        f.writeInt32(3, VectorFeature.GEOM_POLYGON);
        f.writePackedInt32(4, ringGeometry(xs, ys, true));
        return fb.toByteArray();
    }

    private static byte[] lineFeature(int[] xs, int[] ys) throws IOException {
        ByteArrayOutputStream fb = new ByteArrayOutputStream();
        ProtoWriter f = new ProtoWriter(fb);
        f.writeInt32(3, VectorFeature.GEOM_LINESTRING);
        f.writePackedInt32(4, ringGeometry(xs, ys, false));
        return fb.toByteArray();
    }

    private static List ringGeometry(int[] xs, int[] ys, boolean close) {
        List g = new ArrayList();
        int cx = 0;
        int cy = 0;
        // MoveTo first point.
        g.add(new Integer(command(1, 1)));
        g.add(new Integer(ProtoWriter.zigZag32(xs[0] - cx)));
        g.add(new Integer(ProtoWriter.zigZag32(ys[0] - cy)));
        cx = xs[0];
        cy = ys[0];
        // LineTo remaining points.
        int n = xs.length - 1;
        if (n > 0) {
            g.add(new Integer(command(2, n)));
            for (int i = 1; i < xs.length; i++) {
                g.add(new Integer(ProtoWriter.zigZag32(xs[i] - cx)));
                g.add(new Integer(ProtoWriter.zigZag32(ys[i] - cy)));
                cx = xs[i];
                cy = ys[i];
            }
        }
        if (close) {
            g.add(new Integer(command(7, 1)));
        }
        return g;
    }

    private static List pointGeometry(int x, int y) {
        List g = new ArrayList();
        g.add(new Integer(command(1, 1)));
        g.add(new Integer(ProtoWriter.zigZag32(x)));
        g.add(new Integer(ProtoWriter.zigZag32(y)));
        return g;
    }

    private static int command(int id, int count) {
        return (id & 0x7) | (count << 3);
    }
}
