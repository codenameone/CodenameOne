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

/// One rule of a [MapStyle]: it selects features from a named vector-tile
/// source layer (optionally narrowed by a single attribute equality filter)
/// within a zoom range, and describes how to paint them.
///
/// Supports the four layer types that cover a usable basemap: a single
/// [#TYPE_BACKGROUND] fill, [#TYPE_FILL] polygons, [#TYPE_LINE] strokes and
/// [#TYPE_SYMBOL] text labels. This is intentionally a pragmatic subset of the
/// MapLibre GL style spec rather than a full implementation.
public final class StyleLayer {

    /// A full-viewport background fill.
    public static final int TYPE_BACKGROUND = 0;
    /// Filled (and optionally stroked) polygons.
    public static final int TYPE_FILL = 1;
    /// Stroked lines.
    public static final int TYPE_LINE = 2;
    /// Text labels placed at points, line midpoints or polygon centers.
    public static final int TYPE_SYMBOL = 3;

    private final int type;
    private String sourceLayer;
    private double minZoom = 0;
    private double maxZoom = 24;

    private int fillColor = 0xff000000;
    private int lineColor = 0xff000000;
    private ZoomValue lineWidth = ZoomValue.constant(1);
    private int textColor = 0xff000000;
    private int textHaloColor = 0x00000000;
    private ZoomValue textSize = ZoomValue.constant(12);
    private String textField;

    private String filterKey;
    private String[] filterValues;
    private String excludeKey;
    private String[] excludeValues;
    private boolean linesOnly;

    StyleLayer(int type) {
        this.type = type;
    }

    int getType() {
        return type;
    }

    String getSourceLayer() {
        return sourceLayer;
    }

    StyleLayer sourceLayer(String sourceLayer) {
        this.sourceLayer = sourceLayer;
        return this;
    }

    StyleLayer zoomRange(double minZoom, double maxZoom) {
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        return this;
    }

    boolean visibleAt(double zoom) {
        return zoom >= minZoom && zoom <= maxZoom;
    }

    StyleLayer fillColor(int argb) {
        this.fillColor = argb;
        return this;
    }

    int getFillColor() {
        return fillColor;
    }

    StyleLayer lineColor(int argb) {
        this.lineColor = argb;
        return this;
    }

    int getLineColor() {
        return lineColor;
    }

    StyleLayer lineWidth(ZoomValue width) {
        this.lineWidth = width;
        return this;
    }

    double lineWidthAt(double zoom) {
        return lineWidth.eval(zoom);
    }

    StyleLayer textColor(int argb) {
        this.textColor = argb;
        return this;
    }

    int getTextColor() {
        return textColor;
    }

    StyleLayer textHaloColor(int argb) {
        this.textHaloColor = argb;
        return this;
    }

    int getTextHaloColor() {
        return textHaloColor;
    }

    StyleLayer textSize(ZoomValue size) {
        this.textSize = size;
        return this;
    }

    double textSizeAt(double zoom) {
        return textSize.eval(zoom);
    }

    StyleLayer textField(String field) {
        this.textField = field;
        return this;
    }

    String getTextField() {
        return textField;
    }

    StyleLayer filter(String key, String value) {
        return filterIn(key, new String[]{value});
    }

    /// Keeps only the features whose `key` attribute is one of `values`: the
    /// classes of road that are drawn as main roads, say.
    StyleLayer filterIn(String key, String[] values) {
        this.filterKey = key;
        this.filterValues = values;
        return this;
    }

    /// Drops features whose `key` attribute is one of `values`.
    StyleLayer excludeIn(String key, String[] values) {
        this.excludeKey = key;
        this.excludeValues = values;
        return this;
    }

    /// Makes a line layer stroke lines alone. A layer of roads also carries
    /// areas -- piers, squares, the decks of bridges -- and stroking the edge
    /// of an area as though it were a road draws a road around every pier.
    StyleLayer linesOnly() {
        this.linesOnly = true;
        return this;
    }

    boolean isLinesOnly() {
        return linesOnly;
    }

    /// Drops features whose `key` attribute equals `value` (e.g. excluding
    /// `class=ferry` so ferry routes are not drawn as roads across the water).
    StyleLayer excludeFilter(String key, String value) {
        return excludeIn(key, new String[]{value});
    }

    /// Whether `feature` passes this layer's optional equality / exclusion filters.
    boolean accepts(VectorFeature feature) {
        if (excludeKey != null) {
            Object ev = feature.getAttribute(excludeKey);
            if (ev != null && among(excludeValues, String.valueOf(ev))) {
                return false;
            }
        }
        if (filterKey == null) {
            return true;
        }
        Object v = feature.getAttribute(filterKey);
        return v != null && among(filterValues, String.valueOf(v));
    }

    private static boolean among(String[] values, String value) {
        if (values != null) {
            for (String candidate : values) {
                if (candidate != null && candidate.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }
}
