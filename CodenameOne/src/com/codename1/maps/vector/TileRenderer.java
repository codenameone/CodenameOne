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

import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;

import java.util.ArrayList;
import java.util.List;

/// Rasterizes a decoded [VectorTile] into a tile-sized buffer according to a
/// [MapStyle], and extracts the text labels for the engine to place globally.
///
/// Fills and lines are drawn into the per-tile buffer with [GeneralPath] +
/// [Stroke]; symbol layers are not drawn here -- their labels are collected by
/// [#extractLabels] and placed across tile boundaries by the [LabelEngine] so
/// they neither clip nor duplicate at tile seams.
final class TileRenderer {

    private TileRenderer() {
    }

    /// Draws the fill and line layers of `tile` into `g` (a buffer of
    /// `tileSize` pixels), honoring the rules in `style` at integer `zoom`.
    static void renderTile(Graphics g, VectorTile tile, MapStyle style, int zoom, int tileSize) {
        g.setAntiAliased(true);
        List styleLayers = style.getLayers();
        for (Object slObj : styleLayers) {
            StyleLayer sl = (StyleLayer) slObj;
            if (sl.getType() == StyleLayer.TYPE_SYMBOL || sl.getType() == StyleLayer.TYPE_BACKGROUND) {
                continue;
            }
            if (!sl.visibleAt(zoom) || sl.getSourceLayer() == null) {
                continue;
            }
            VectorLayer vl = tile.getLayer(sl.getSourceLayer());
            if (vl == null) {
                continue;
            }
            double scale = (double) tileSize / vl.getExtent();
            List features = vl.getFeatures();
            if (sl.getType() == StyleLayer.TYPE_FILL) {
                renderFills(g, features, sl, scale);
            } else {
                renderLines(g, features, sl, scale, zoom);
            }
        }
    }

    private static void renderFills(Graphics g, List features, StyleLayer sl, double scale) {
        int argb = sl.getFillColor();
        applyColor(g, argb);
        for (Object featureObj : features) {
            VectorFeature f = (VectorFeature) featureObj;
            if (f.getGeometryType() != VectorFeature.GEOM_POLYGON || !sl.accepts(f)) {
                continue;
            }
            List parts = f.getParts();
            if (parts.isEmpty()) {
                continue;
            }
            GeneralPath path = new GeneralPath();
            for (Object partObj : parts) {
                int[] ring = (int[]) partObj;
                appendRing(path, ring, scale, true);
            }
            g.fillShape(path);
        }
    }

    private static void renderLines(Graphics g, List features, StyleLayer sl, double scale, int zoom) {
        applyColor(g, sl.getLineColor());
        float width = (float) sl.lineWidthAt(zoom);
        if (width < 0.5f) {
            width = 0.5f;
        }
        Stroke stroke = new Stroke(width, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 4f);
        for (Object featureObj : features) {
            VectorFeature f = (VectorFeature) featureObj;
            int gt = f.getGeometryType();
            if ((gt != VectorFeature.GEOM_LINESTRING && gt != VectorFeature.GEOM_POLYGON) || !sl.accepts(f)) {
                continue;
            }
            List parts = f.getParts();
            for (Object partObj : parts) {
                int[] line = (int[]) partObj;
                GeneralPath path = new GeneralPath();
                appendRing(path, line, scale, false);
                g.drawShape(path, stroke);
            }
        }
    }

    private static void appendRing(GeneralPath path, int[] coords, double scale, boolean close) {
        if (coords.length < 2) {
            return;
        }
        path.moveTo((float) (coords[0] * scale), (float) (coords[1] * scale));
        for (int i = 2; i + 1 < coords.length; i += 2) {
            path.lineTo((float) (coords[i] * scale), (float) (coords[i + 1] * scale));
        }
        if (close) {
            path.closePath();
        }
    }

    private static void applyColor(Graphics g, int argb) {
        int a = (argb >>> 24) & 0xff;
        if (a == 0) {
            a = 255;
        }
        g.setAlpha(a);
        g.setColor(argb & 0xffffff);
    }

    /// Collects the labels declared by the style's symbol layers for one tile.
    /// `tileX`/`tileY` are the tile's slippy coordinates at integer `zoom`.
    static List extractLabels(VectorTile tile, MapStyle style, int zoom,
                              int tileX, int tileY, int tileSize) {
        List out = new ArrayList();
        List styleLayers = style.getLayers();
        for (Object slObj : styleLayers) {
            StyleLayer sl = (StyleLayer) slObj;
            if (sl.getType() != StyleLayer.TYPE_SYMBOL || sl.getSourceLayer() == null) {
                continue;
            }
            if (!sl.visibleAt(zoom)) {
                continue;
            }
            VectorLayer vl = tile.getLayer(sl.getSourceLayer());
            if (vl == null) {
                continue;
            }
            int extent = vl.getExtent();
            double scale = (double) tileSize / extent;
            double originX = (double) tileX * tileSize;
            double originY = (double) tileY * tileSize;
            List features = vl.getFeatures();
            for (Object featureObj : features) {
                VectorFeature f = (VectorFeature) featureObj;
                if (!sl.accepts(f)) {
                    continue;
                }
                Object value = sl.getTextField() == null ? null : f.getAttribute(sl.getTextField());
                if (value == null) {
                    continue;
                }
                double[] anchor = anchorOf(f);
                if (anchor == null) {
                    continue;
                }
                // Drop labels whose anchor falls in the tile's buffer (outside
                // 0..extent): those belong to a neighbouring tile and would
                // otherwise float in empty space past the loaded coverage.
                if (anchor[0] < 0 || anchor[0] > extent || anchor[1] < 0 || anchor[1] > extent) {
                    continue;
                }
                double worldX = originX + anchor[0] * scale;
                double worldY = originY + anchor[1] * scale;
                out.add(new LabelCandidate(String.valueOf(value), worldX, worldY, zoom,
                        sl.getTextColor(), sl.getTextHaloColor(), sl.textSizeAt(zoom)));
            }
        }
        return out;
    }

    private static double[] anchorOf(VectorFeature f) {
        List parts = f.getParts();
        if (parts.isEmpty()) {
            return null;
        }
        int[] first = (int[]) parts.get(0);
        if (first.length < 2) {
            return null;
        }
        if (f.getGeometryType() == VectorFeature.GEOM_POINT) {
            return new double[]{first[0], first[1]};
        }
        // Centroid of the first ring/line as the label anchor.
        double sx = 0;
        double sy = 0;
        int n = 0;
        for (int i = 0; i + 1 < first.length; i += 2) {
            sx += first[i];
            sy += first[i + 1];
            n++;
        }
        if (n == 0) {
            return null;
        }
        return new double[]{sx / n, sy / n};
    }
}
