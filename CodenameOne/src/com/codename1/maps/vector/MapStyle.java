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
import com.codename1.io.JSONParser;
import com.codename1.ui.CSSColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// An ordered list of [StyleLayer] rules plus a background color, describing
/// how a vector tile is painted. The built-in [#light] and [#dark] styles
/// target the common OpenMapTiles/Protomaps source-layer names and also the
/// simplified names used by the bundled screenshot fixtures, so they render
/// real basemaps and the offline fixtures alike.
public final class MapStyle {

    private final String name;
    private int backgroundColor;
    private final List layers = new ArrayList();

    /// Creates an empty style with the given background color (0xAARRGGBB).
    public MapStyle(String name, int backgroundColor) {
        this.name = name;
        this.backgroundColor = backgroundColor;
    }

    /// The style name.
    public String getName() {
        return name;
    }

    /// The viewport background color as 0xAARRGGBB.
    public int getBackgroundColor() {
        return backgroundColor;
    }

    /// Appends a layer rule (rendered in insertion order, bottom to top).
    public MapStyle add(StyleLayer layer) {
        layers.add(layer);
        return this;
    }

    List getLayers() {
        return layers;
    }

    // ---- Built-in styles --------------------------------------------------

    /// A clean light basemap (sensible default for most apps).
    public static MapStyle light() {
        MapStyle s = new MapStyle("light", 0xfff2efe9);
        addPolygonRule(s, "water", 0xffa0c8f0);
        addPolygonRule(s, "ocean", 0xffa0c8f0);
        addPolygonRule(s, "landcover", 0xffd8e8c8);
        addPolygonRule(s, "landuse", 0xffe8f0d8);
        addPolygonRule(s, "park", 0xffc8e0b0);
        addLineRule(s, "waterway", 0xffa0c8f0, 6, 1.0, 16, 4.0);
        addLineRule(s, "road", 0xffffffff, 6, 1.0, 18, 8.0);
        addLineRule(s, "transportation", 0xffffffff, 6, 1.0, 18, 8.0).excludeFilter("class", "ferry");
        addPolygonRule(s, "building", 0xffd9d0c9);
        addPolygonRule(s, "buildings", 0xffd9d0c9);
        addSymbolRule(s, "place", "name", 0xff333333, 0xffffffff);
        addSymbolRule(s, "place_label", "name", 0xff333333, 0xffffffff);
        return s;
    }

    /// A dark basemap suited to night mode.
    public static MapStyle dark() {
        MapStyle s = new MapStyle("dark", 0xff121417);
        addPolygonRule(s, "water", 0xff1b2733);
        addPolygonRule(s, "ocean", 0xff1b2733);
        addPolygonRule(s, "landcover", 0xff1a1d20);
        addPolygonRule(s, "landuse", 0xff1d2024);
        addPolygonRule(s, "park", 0xff17251a);
        addLineRule(s, "waterway", 0xff1b2733, 6, 1.0, 16, 4.0);
        addLineRule(s, "road", 0xff3a4048, 6, 1.0, 18, 8.0);
        addLineRule(s, "transportation", 0xff3a4048, 6, 1.0, 18, 8.0).excludeFilter("class", "ferry");
        addPolygonRule(s, "building", 0xff20242a);
        addPolygonRule(s, "buildings", 0xff20242a);
        addSymbolRule(s, "place", "name", 0xffe8e8e8, 0xff000000);
        addSymbolRule(s, "place_label", "name", 0xffe8e8e8, 0xff000000);
        return s;
    }

    private static void addPolygonRule(MapStyle s, String sourceLayer, int color) {
        s.add(new StyleLayer(StyleLayer.TYPE_FILL).sourceLayer(sourceLayer).fillColor(color));
    }

    private static StyleLayer addLineRule(MapStyle s, String sourceLayer, int color,
                                          double z0, double w0, double z1, double w1) {
        StyleLayer sl = new StyleLayer(StyleLayer.TYPE_LINE).sourceLayer(sourceLayer).lineColor(color)
                .lineWidth(ZoomValue.stops(new double[]{z0, z1}, new double[]{w0, w1}));
        s.add(sl);
        return sl;
    }

    private static void addSymbolRule(MapStyle s, String sourceLayer, String field,
                                      int textColor, int haloColor) {
        s.add(new StyleLayer(StyleLayer.TYPE_SYMBOL).sourceLayer(sourceLayer).textField(field)
                .textColor(textColor).textHaloColor(haloColor)
                .textSize(ZoomValue.constant(13)));
    }

    // ---- JSON loading -----------------------------------------------------

    /// Parses a (subset of a) MapLibre GL style JSON document. Recognized:
    /// the top-level `layers` array with `type` of `background`/`fill`/
    /// `line`/`symbol`, each layer's `source-layer`, `minzoom`/`maxzoom`,
    /// a simple `["==", key, value]` filter, and the common paint/layout
    /// properties (`background-color`, `fill-color`, `line-color`,
    /// `line-width`, `text-field`, `text-color`, `text-size`). Unsupported
    /// constructs are ignored rather than failing.
    public static MapStyle fromJson(String json) {
        MapStyle style = new MapStyle("custom", 0xfff2efe9);
        try {
            Map root = new JSONParser().parseJSON(new CharArrayReader(json.toCharArray()));
            Object layersObj = root.get("layers");
            if (!(layersObj instanceof List)) {
                return style;
            }
            List layers = (List) layersObj;
            for (Object lo : layers) {
                if (!(lo instanceof Map)) {
                    continue;
                }
                StyleLayer parsed = parseLayer(style, (Map) lo);
                if (parsed != null) {
                    style.add(parsed);
                }
            }
        } catch (Throwable t) {
            // Malformed style: fall back to whatever parsed so far.
            return style;
        }
        return style;
    }

    private static StyleLayer parseLayer(MapStyle style, Map layer) {
        String type = str(layer.get("type"), "");
        Map paint = layer.get("paint") instanceof Map ? (Map) layer.get("paint") : null;
        Map layout = layer.get("layout") instanceof Map ? (Map) layer.get("layout") : null;
        if ("background".equals(type)) {
            if (paint != null) {
                style.backgroundColor = color(paint.get("background-color"), style.backgroundColor);
            }
            return null;
        }
        StyleLayer sl;
        if ("fill".equals(type)) {
            sl = new StyleLayer(StyleLayer.TYPE_FILL);
            if (paint != null) {
                sl.fillColor(color(paint.get("fill-color"), 0xff808080));
            }
        } else if ("line".equals(type)) {
            sl = new StyleLayer(StyleLayer.TYPE_LINE);
            if (paint != null) {
                sl.lineColor(color(paint.get("line-color"), 0xff808080));
                sl.lineWidth(ZoomValue.constant(number(paint.get("line-width"), 1)));
            }
        } else if ("symbol".equals(type)) {
            sl = new StyleLayer(StyleLayer.TYPE_SYMBOL);
            if (layout != null) {
                sl.textField(fieldName(str(layout.get("text-field"), "name")));
                sl.textSize(ZoomValue.constant(number(layout.get("text-size"), 13)));
            }
            if (paint != null) {
                sl.textColor(color(paint.get("text-color"), 0xff333333));
                sl.textHaloColor(color(paint.get("text-halo-color"), 0x00000000));
            }
        } else {
            return null;
        }
        sl.sourceLayer(str(layer.get("source-layer"), null));
        sl.zoomRange(number(layer.get("minzoom"), 0), number(layer.get("maxzoom"), 24));
        applyFilter(sl, layer.get("filter"));
        return sl;
    }

    private static void applyFilter(StyleLayer sl, Object filter) {
        if (filter instanceof List) {
            List f = (List) filter;
            if (f.size() == 3 && "==".equals(String.valueOf(f.get(0)))) {
                sl.filter(String.valueOf(f.get(1)), String.valueOf(f.get(2)));
            }
        }
    }

    private static String fieldName(String textField) {
        // MapLibre text-field is often "{name}"; strip the braces.
        if (textField == null) {
            return "name";
        }
        String s = textField;
        if (s.startsWith("{") && s.endsWith("}")) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static String str(Object o, String def) {
        return o == null ? def : o.toString();
    }

    private static double number(Object o, double def) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        if (o instanceof String) {
            try {
                return Double.parseDouble((String) o);
            } catch (NumberFormatException nfe) {
                return def;
            }
        }
        return def;
    }

    private static int color(Object o, int def) {
        if (!(o instanceof String)) {
            return def;
        }
        return CSSColor.parse((String) o, def);
    }
}
