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
import java.util.Collections;
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
                if (value == null || String.valueOf(value).trim().length() == 0) {
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

    // Put road names halfway along the longest line part. Averaging vertices
    // can put a label far from a curved road, and biases it toward dense bends.
    private static double[] lineAnchor(List parts) {
        int[] longest = null;
        double longestLength = 0;
        for (Object part : parts) {
            int[] line = (int[]) part;
            double length = 0;
            for (int i = 2; i + 1 < line.length; i += 2) {
                length += segmentLength(line, i);
            }
            if (length > longestLength) {
                longest = line;
                longestLength = length;
            }
        }
        if (longest == null) {
            return null;
        }
        double remaining = longestLength / 2;
        for (int i = 2; i + 1 < longest.length; i += 2) {
            double length = segmentLength(longest, i);
            if (length > 0 && remaining <= length) {
                double fraction = remaining / length;
                return new double[]{
                    longest[i - 2] + fraction * ((double) longest[i] - longest[i - 2]),
                    longest[i - 1] + fraction * ((double) longest[i + 1] - longest[i - 1])
                };
            }
            remaining -= length;
        }
        return null;
    }

    private static double segmentLength(int[] line, int end) {
        double dx = (double) line[end] - line[end - 2];
        double dy = (double) line[end + 1] - line[end - 1];
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static double[] anchorOf(VectorFeature f) {
        List parts = f.getParts();
        if (parts.isEmpty()) {
            return null;
        }
        if (f.getGeometryType() == VectorFeature.GEOM_LINESTRING) {
            return lineAnchor(parts);
        }
        int[] first = (int[]) parts.get(0);
        if (first.length < 2) {
            return null;
        }
        if (f.getGeometryType() == VectorFeature.GEOM_POINT) {
            return new double[]{first[0], first[1]};
        }
        return polygonAnchor(parts);
    }

    // Find the midpoint of the widest interior horizontal span. Unlike a
    // vertex average, this remains inside concave polygons and skips holes.
    private static double[] polygonAnchor(List parts) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (Object partObj : parts) {
            int[] ring = (int[]) partObj;
            for (int i = 0; i + 1 < ring.length; i += 2) {
                minX = Math.min(minX, ring[i]);
                maxX = Math.max(maxX, ring[i]);
                minY = Math.min(minY, ring[i + 1]);
                maxY = Math.max(maxY, ring[i + 1]);
            }
        }
        if (minX > maxX || minY > maxY) {
            return null;
        }
        double height = maxY - minY;
        int rows = height <= 0 ? 1 : 32;
        double[] best = null;
        double bestWidth = -1;
        for (int row = 0; row < rows; row++) {
            double y = height <= 0 ? minY : minY + height * (row + 0.5) / rows;
            List intersections = new ArrayList();
            for (Object partObj : parts) {
                int[] ring = (int[]) partObj;
                for (int i = 0; i + 1 < ring.length; i += 2) {
                    int next = (i + 2) % ring.length;
                    double y0 = ring[i + 1];
                    double y1 = ring[next + 1];
                    if ((y0 > y) != (y1 > y)) {
                        double x0 = ring[i];
                        double x1 = ring[next];
                        intersections.add(Double.valueOf(x0 + (y - y0) * (x1 - x0) / (y1 - y0)));
                    }
                }
            }
            Collections.sort(intersections);
            for (int i = 0; i + 1 < intersections.size(); i += 2) {
                double left = ((Double) intersections.get(i)).doubleValue();
                double right = ((Double) intersections.get(i + 1)).doubleValue();
                double width = right - left;
                double x = (left + right) / 2;
                if (width > bestWidth && pointInPolygon(x, y, parts)) {
                    bestWidth = width;
                    best = new double[]{x, y};
                }
            }
        }
        return best;
    }

    private static boolean pointInPolygon(double x, double y, List parts) {
        boolean inside = false;
        for (Object partObj : parts) {
            int[] ring = (int[]) partObj;
            for (int i = 0, j = ring.length - 2; i + 1 < ring.length; j = i, i += 2) {
                double xi = ring[i];
                double yi = ring[i + 1];
                double xj = ring[j];
                double yj = ring[j + 1];
                if ((yi > y) != (yj > y)
                        && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }
}
