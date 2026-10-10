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
import com.codename1.ui.plaf.UIManager;

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

    /// A clean light basemap (sensible default for most apps): muted land,
    /// soft water and parks, and roads that read by their importance -- main
    /// roads wider and faintly tinted, every road edged so that it stands off
    /// the land, side streets and buildings only once the map is close enough
    /// for them to mean something. Settlement names are joined by the names of
    /// main roads and parks from zoom 12, side streets from zoom 14 and points
    /// of interest from zoom 16, when the tile source supplies those names.
    ///
    /// Every colour falls back to the value baked in here but is overridable
    /// through a theme constant (a CSS color string) so an app can recolour the
    /// map without supplying a whole style: `mapLightWaterColor`,
    /// `mapLightLandcoverColor`, `mapLightLanduseColor`, `mapLightParkColor`,
    /// `mapLightRoadColor`, `mapLightMajorRoadColor`, `mapLightRoadCasingColor`,
    /// `mapLightBuildingColor`, `mapLightBackgroundColor`, `mapLightLabelColor`,
    /// `mapLightRoadLabelColor`, `mapLightLabelHaloColor`. e.g. a theme constant
    /// `mapLightWaterColor=#1e88e5`.
    public static MapStyle light() {
        Palette p = new Palette();
        p.background = themeColor("mapLightBackgroundColor", 0xfff5f3ee);
        p.water = themeColor("mapLightWaterColor", 0xffabd3f0);
        p.landcover = themeColor("mapLightLandcoverColor", 0xffe1edd6);
        p.landuse = themeColor("mapLightLanduseColor", 0xfff1efe9);
        p.park = themeColor("mapLightParkColor", 0xffd2e8c4);
        p.road = themeColor("mapLightRoadColor", 0xffffffff);
        p.majorRoad = themeColor("mapLightMajorRoadColor", 0xfffdf0c4);
        p.casing = themeColor("mapLightRoadCasingColor", 0xffdcd8cf);
        p.majorCasing = themeColor("mapLightRoadCasingColor", 0xffe3cc8c);
        p.path = 0xffe6e1d6;
        p.rail = 0xffd2cec6;
        p.building = themeColor("mapLightBuildingColor", 0xffeae7e0);
        p.label = themeColor("mapLightLabelColor", 0xff2f343b);
        p.roadLabel = themeColor("mapLightRoadLabelColor", 0xff6a6f78);
        p.halo = themeColor("mapLightLabelHaloColor", p.background);
        return build("light", p);
    }

    /// A dark basemap suited to night mode. Mirrors [#light()], with the same
    /// order of importance in dark tones: each colour is overridable via a
    /// theme constant (`mapDarkWaterColor`, `mapDarkLandcoverColor`,
    /// `mapDarkLanduseColor`, `mapDarkParkColor`, `mapDarkRoadColor`,
    /// `mapDarkMajorRoadColor`, `mapDarkRoadCasingColor`,
    /// `mapDarkBuildingColor`, `mapDarkBackgroundColor`, `mapDarkLabelColor`,
    /// `mapDarkRoadLabelColor`, `mapDarkLabelHaloColor`).
    public static MapStyle dark() {
        Palette p = new Palette();
        p.background = themeColor("mapDarkBackgroundColor", 0xff16191d);
        p.water = themeColor("mapDarkWaterColor", 0xff0d1b29);
        p.landcover = themeColor("mapDarkLandcoverColor", 0xff18211b);
        p.landuse = themeColor("mapDarkLanduseColor", 0xff181b20);
        p.park = themeColor("mapDarkParkColor", 0xff18281c);
        p.road = themeColor("mapDarkRoadColor", 0xff323942);
        p.majorRoad = themeColor("mapDarkMajorRoadColor", 0xff505966);
        p.casing = themeColor("mapDarkRoadCasingColor", 0xff0e1013);
        p.majorCasing = themeColor("mapDarkRoadCasingColor", 0xff0e1013);
        p.path = 0xff22272e;
        p.rail = 0xff2a2f36;
        p.building = themeColor("mapDarkBuildingColor", 0xff1c2026);
        p.label = themeColor("mapDarkLabelColor", 0xffe4e7eb);
        p.roadLabel = themeColor("mapDarkRoadLabelColor", 0xff9aa2ad);
        p.halo = themeColor("mapDarkLabelHaloColor", p.background);
        return build("dark", p);
    }

    // The colours of a built-in style.
    private static final class Palette {
        private int background;
        private int water;
        private int landcover;
        private int landuse;
        private int park;
        private int road;
        private int majorRoad;
        private int casing;
        private int majorCasing;
        private int path;
        private int rail;
        private int building;
        private int label;
        private int roadLabel;
        private int halo;
    }

    // The classes of road, in the vocabularies of the two tile schemas in use
    // (OpenMapTiles' `transportation`, and `road` of the Mapbox Streets one).
    // A class that is in none of the lists is drawn as a side street: a road
    // the style has never heard of is still a road.
    private static final String[] MAJOR_ROADS = {"motorway", "trunk", "primary", "secondary", "tertiary",
        "motorway_link", "trunk_link", "primary_link", "secondary_link", "tertiary_link", "main"};
    // The few that carry a city's traffic, and are tinted to say so.
    private static final String[] HIGHWAYS = {"motorway", "trunk", "primary",
        "motorway_link", "trunk_link", "primary_link"};
    // Alleys, driveways and the aisles of car parks: roads, but at the scale
    // of a neighbourhood a rash of stubs off every street.
    private static final String[] SERVICE_ROADS = {"service", "driveway", "parking_aisle"};
    private static final String[] PATHS = {"path", "track", "footway", "cycleway", "steps", "pedestrian"};
    private static final String[] RAILS = {"rail", "transit", "major_rail", "minor_rail"};
    // Neither a street nor drawn as one: the lists above, and what is no road
    // at all. A ferry route is a line across open water, a pier is an area.
    private static final String[] NOT_STREETS = {"motorway", "trunk", "primary", "secondary", "tertiary",
        "motorway_link", "trunk_link", "primary_link", "secondary_link", "tertiary_link", "main",
        "path", "track", "footway", "cycleway", "steps", "pedestrian",
        "rail", "transit", "major_rail", "minor_rail",
        "service", "driveway", "parking_aisle",
        "ferry", "pier", "aerialway", "golf", "bridge"};
    private static final String[] ROAD_LAYERS = {"transportation", "road"};
    private static final String[] ROAD_NAME_LAYERS = {"transportation_name", "road", "road_label"};

    private static MapStyle build(String name, Palette p) {
        MapStyle s = new MapStyle(name, p.background);
        addPolygonRule(s, "landcover", p.landcover);
        addPolygonRule(s, "landuse", p.landuse);
        addPolygonRule(s, "park", p.park);
        addPolygonRule(s, "water", p.water);
        addPolygonRule(s, "ocean", p.water);
        addLineRule(s, "waterway", p.water, WATERWAY_ZOOMS, WATERWAY_WIDTHS);
        // Under the roads, and not before the map is close enough to tell one
        // from the next: at city scale they are a texture, and the strongest
        // thing on the map if they are given any weight.
        s.add(new StyleLayer(StyleLayer.TYPE_FILL).sourceLayer("building").fillColor(p.building)
                .zoomRange(15, 24));
        s.add(new StyleLayer(StyleLayer.TYPE_FILL).sourceLayer("buildings").fillColor(p.building)
                .zoomRange(15, 24));
        for (String layer : ROAD_LAYERS) {
            addLineRule(s, layer, p.path, PATH_ZOOMS, PATH_WIDTHS).filterIn("class", PATHS)
                    .linesOnly().zoomRange(15, 24);
            addLineRule(s, layer, p.rail, PATH_ZOOMS, PATH_WIDTHS).filterIn("class", RAILS)
                    .linesOnly().zoomRange(13, 24);
            addLineRule(s, layer, p.road, SERVICE_ZOOMS, SERVICE_WIDTHS).filterIn("class", SERVICE_ROADS)
                    .linesOnly().zoomRange(16, 24);
        }
        // Every edge before any road, so that where two roads meet neither is
        // edged across the other.
        for (String layer : ROAD_LAYERS) {
            addLineRule(s, layer, p.casing, STREET_ZOOMS, STREET_CASINGS).excludeIn("class", NOT_STREETS)
                    .linesOnly().zoomRange(15, 24);
            addLineRule(s, layer, p.casing, MAJOR_ZOOMS, MAJOR_CASINGS).filterIn("class", MAJOR_ROADS)
                    .linesOnly().zoomRange(12, 24);
            addLineRule(s, layer, p.majorCasing, MAJOR_ZOOMS, MAJOR_CASINGS).filterIn("class", HIGHWAYS)
                    .linesOnly().zoomRange(12, 24);
        }
        for (String layer : ROAD_LAYERS) {
            addLineRule(s, layer, p.road, STREET_ZOOMS, STREET_WIDTHS).excludeIn("class", NOT_STREETS)
                    .linesOnly().zoomRange(12, 24);
        }
        for (String layer : ROAD_LAYERS) {
            addLineRule(s, layer, p.road, MAJOR_ZOOMS, MAJOR_WIDTHS).filterIn("class", MAJOR_ROADS)
                    .linesOnly();
        }
        for (String layer : ROAD_LAYERS) {
            addLineRule(s, layer, p.majorRoad, MAJOR_ZOOMS, MAJOR_WIDTHS).filterIn("class", HIGHWAYS)
                    .linesOnly();
        }
        addBasemapLabels(s, p.label, p.roadLabel, p.halo);
        return s;
    }

    /// Resolves a map colour from a theme constant (a CSS color string parsed
    /// by [CSSColor]) so apps can recolour the built-in styles from their
    /// theme, falling back to `defaultArgb` when the constant is absent.
    private static int themeColor(String constant, int defaultArgb) {
        try {
            UIManager m = UIManager.getInstance();
            String v = m == null ? null : m.getThemeConstant(constant, null);
            return v == null ? defaultArgb : CSSColor.parse(v, defaultArgb);
        } catch (Throwable t) {
            // No theme context yet (very early startup / headless) -> built-in
            // default; the style must never fail to build over a missing theme.
            return defaultArgb;
        }
    }

    private static void addPolygonRule(MapStyle s, String sourceLayer, int color) {
        s.add(new StyleLayer(StyleLayer.TYPE_FILL).sourceLayer(sourceLayer).fillColor(color));
    }

    // Line widths in logical pixels. Roads keep widening past zoom 18, where
    // the map is overzoomed and a street should read as a street, not a
    // hairline; roughly doubling per level matches the ground they cover. An
    // edge is the same road drawn first, a little wider, in a darker colour.
    private static final double[] STREET_ZOOMS = {12, 14, 16, 18, 20, 22};
    private static final double[] STREET_WIDTHS = {0.5, 1.2, 4, 9, 20, 40};
    private static final double[] STREET_CASINGS = {0.5, 1.2, 5.5, 10.5, 22, 42};
    private static final double[] MAJOR_ZOOMS = {6, 10, 12, 14, 16, 18, 20, 22};
    private static final double[] MAJOR_WIDTHS = {0.6, 1.2, 1.8, 3, 6.5, 13, 28, 52};
    private static final double[] MAJOR_CASINGS = {0.6, 1.2, 2.8, 4.4, 8, 14.5, 30, 54};
    private static final double[] SERVICE_ZOOMS = {16, 18, 20, 22};
    private static final double[] SERVICE_WIDTHS = {1.2, 4, 10, 20};
    private static final double[] PATH_ZOOMS = {13, 16, 20};
    private static final double[] PATH_WIDTHS = {0.6, 1.2, 4};
    private static final double[] WATERWAY_ZOOMS = {6, 12, 16, 20};
    private static final double[] WATERWAY_WIDTHS = {0.5, 1, 3, 10};

    private static StyleLayer addLineRule(MapStyle s, String sourceLayer, int color,
                                          double[] zooms, double[] widths) {
        StyleLayer sl = new StyleLayer(StyleLayer.TYPE_LINE).sourceLayer(sourceLayer).lineColor(color)
                .lineWidth(ZoomValue.stops(zooms, widths));
        s.add(sl);
        return sl;
    }

    // Names are considered in the order they are listed, and one that is in
    // the way of another already placed is left out, so the order is the
    // order of importance: settlements, then main roads, then parks, and only
    // close in the side streets and points of interest that would otherwise
    // crowd everything else off the map. Ferry routes share the street-name
    // layer but their lines are not drawn (see the line rules), so their names
    // are left out too: a name laid along an invisible route reads as text
    // floating on open water.
    //
    // Sizes are in logical pixels, about the size of the small print of an
    // interface: a map is read around its names, not through them.
    private static void addBasemapLabels(MapStyle s, int label, int roadLabel, int halo) {
        addSymbolRule(s, "place", "name", label, halo, 13);
        addSymbolRule(s, "place_label", "name", label, halo, 13);
        for (String layer : ROAD_NAME_LAYERS) {
            addSymbolRule(s, layer, "name", roadLabel, halo, 12).zoomRange(12, 24)
                    .filterIn("class", MAJOR_ROADS);
        }
        addSymbolRule(s, "park", "name", roadLabel, halo, 11).zoomRange(12, 24);
        for (String layer : ROAD_NAME_LAYERS) {
            addSymbolRule(s, layer, "name", roadLabel, halo, 11).zoomRange(14, 24)
                    .excludeIn("class", NOT_STREETS);
        }
        addSymbolRule(s, "poi", "name", roadLabel, halo, 11).zoomRange(16, 24);
    }

    private static StyleLayer addSymbolRule(MapStyle s, String sourceLayer, String field,
                                      int textColor, int haloColor, double size) {
        StyleLayer layer = new StyleLayer(StyleLayer.TYPE_SYMBOL).sourceLayer(sourceLayer).textField(field)
                .textColor(textColor).textHaloColor(haloColor)
                .textSize(ZoomValue.constant(size));
        s.add(layer);
        return layer;
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
