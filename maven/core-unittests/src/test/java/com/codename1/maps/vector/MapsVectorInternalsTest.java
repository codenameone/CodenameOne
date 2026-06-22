/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.maps.vector;

import com.codename1.ui.CSSColor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.io.grpc.ProtoWriter;
import com.codename1.maps.LatLng;
import com.codename1.maps.MapBounds;
import com.codename1.ui.geom.Point;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for the internal pieces of the pure-Java vector map engine. */
class MapsVectorInternalsTest {

    // ---- ZoomValue --------------------------------------------------------

    @Test
    void zoomValueConstantAndStops() {
        assertEquals(3.0, ZoomValue.constant(3).eval(0), 1e-9);
        assertEquals(3.0, ZoomValue.constant(3).eval(20), 1e-9);

        ZoomValue z = ZoomValue.stops(new double[]{4, 16}, new double[]{1, 9});
        assertEquals(1.0, z.eval(2), 1e-9, "below first stop clamps");
        assertEquals(9.0, z.eval(18), 1e-9, "above last stop clamps");
        assertEquals(1.0, z.eval(4), 1e-9);
        assertEquals(9.0, z.eval(16), 1e-9);
        assertEquals(5.0, z.eval(10), 1e-9, "linear interpolation at midpoint");

        // Malformed stops fall back to a constant 0.
        assertEquals(0.0, ZoomValue.stops(new double[]{1}, new double[]{1, 2}).eval(5), 1e-9);
        assertEquals(0.0, ZoomValue.stops(new double[0], new double[0]).eval(5), 1e-9);
    }

    // ---- StyleLayer -------------------------------------------------------

    @Test
    void styleLayerZoomRangeAndFilter() {
        StyleLayer sl = new StyleLayer(StyleLayer.TYPE_FILL)
                .sourceLayer("water")
                .zoomRange(5, 12)
                .fillColor(0xff0000ff)
                .filter("class", "lake");
        assertEquals(StyleLayer.TYPE_FILL, sl.getType());
        assertEquals("water", sl.getSourceLayer());
        assertEquals(0xff0000ff, sl.getFillColor());
        assertFalse(sl.visibleAt(4));
        assertTrue(sl.visibleAt(8));
        assertFalse(sl.visibleAt(13));

        assertTrue(sl.accepts(feature("class", "lake")));
        assertFalse(sl.accepts(feature("class", "river")));
        assertFalse(sl.accepts(feature("other", "lake")));

        StyleLayer noFilter = new StyleLayer(StyleLayer.TYPE_LINE).sourceLayer("road");
        assertTrue(noFilter.accepts(feature("anything", "goes")));
    }

    @Test
    void styleLayerZoomDependentWidthAndSize() {
        StyleLayer line = new StyleLayer(StyleLayer.TYPE_LINE)
                .lineColor(0xffffffff)
                .lineWidth(ZoomValue.stops(new double[]{6, 18}, new double[]{1, 7}));
        assertEquals(1.0, line.lineWidthAt(6), 1e-9);
        assertEquals(7.0, line.lineWidthAt(18), 1e-9);
        assertEquals(4.0, line.lineWidthAt(12), 1e-9);

        StyleLayer sym = new StyleLayer(StyleLayer.TYPE_SYMBOL)
                .textField("name").textColor(0xff112233).textHaloColor(0xffffffff)
                .textSize(ZoomValue.constant(14));
        assertEquals("name", sym.getTextField());
        assertEquals(0xff112233, sym.getTextColor());
        assertEquals(0xffffffff, sym.getTextHaloColor());
        assertEquals(14.0, sym.textSizeAt(10), 1e-9);
    }

    private static VectorFeature feature(String key, String value) {
        java.util.HashMap attrs = new java.util.HashMap();
        attrs.put(key, value);
        return new VectorFeature(0, VectorFeature.GEOM_POLYGON, attrs, new ArrayList());
    }

    // ---- IntArray ---------------------------------------------------------

    @Test
    void intArrayGrowsAndTrims() {
        IntArray a = new IntArray(2);
        for (int i = 0; i < 50; i++) {
            a.add(i * 3);
        }
        assertEquals(50, a.size());
        assertEquals(0, a.get(0));
        assertEquals(147, a.get(49));
        int[] arr = a.toArray();
        assertEquals(50, arr.length);
        assertEquals(147, arr[49]);
        a.clear();
        assertEquals(0, a.size());
    }

    // ---- MapStyle built-ins ----------------------------------------------

    @Test
    void builtInStylesAreNonEmptyAndDistinct() {
        MapStyle light = MapStyle.light();
        MapStyle dark = MapStyle.dark();
        assertEquals("light", light.getName());
        assertEquals("dark", dark.getName());
        assertTrue(light.getLayers().size() > 3);
        assertTrue(dark.getLayers().size() > 3);
        assertNotNull(light.getLayers());
        // Backgrounds differ between light and dark.
        assertTrue(light.getBackgroundColor() != dark.getBackgroundColor());
        // Both target a water source-layer.
        assertTrue(hasSourceLayer(light, "water"));
        assertTrue(hasSourceLayer(dark, "water"));
    }

    private static boolean hasSourceLayer(MapStyle style, String name) {
        List layers = style.getLayers();
        for (int i = 0; i < layers.size(); i++) {
            StyleLayer sl = (StyleLayer) layers.get(i);
            if (name.equals(sl.getSourceLayer())) {
                return true;
            }
        }
        return false;
    }

    // ---- CSSColor ---------------------------------------------------------

    @Test
    void colorParserForms() {
        assertEquals(0xffaabbcc, CSSColor.parse("#aabbcc", 0));
        assertEquals(0xffaabbcc, CSSColor.parse("#abc", 0));
        assertEquals(0x80ffffff, CSSColor.parse("#ffffff80", 0));
        assertEquals(0xff010203, CSSColor.parse("rgb(1,2,3)", 0));
        assertEquals(0xff0a0b0c, CSSColor.parse("  #0A0B0C  ", 0));
        assertEquals(123, CSSColor.parse("not-a-color", 123));
        assertEquals(123, CSSColor.parse(null, 123));
        assertEquals(123, CSSColor.parse("hsl(1,2,3)", 123));
    }

    @Test
    void cssColorSupportsNamedPercentAndShortAlpha() {
        // Forms the old maps-local ColorParser could not handle, now available
        // to the map style engine because it shares the canonical CSS parser.
        assertEquals(0xff0000ff, CSSColor.parse("blue", 0));
        assertEquals(0xffff0000, CSSColor.parse("#f00f", 0));
        assertEquals(0xff00ff00, CSSColor.parse("rgb(0%,100%,0%)", 0));
    }

    // ---- Tile sources -----------------------------------------------------

    @Test
    void openFreeMapSourceIsKeylessVector() {
        MvtTileSource s = MvtTileSource.openFreeMap();
        assertTrue(s.isVector());
        assertEquals(0, s.getMinZoom());
        assertEquals(14, s.getMaxZoom());
        assertEquals(WebMercator.TILE_SIZE, s.getTileSize());
        assertTrue(s.getAttribution().contains("OpenStreetMap"));
    }

    @Test
    void rasterOpenStreetMapSourceIsKeylessRaster() {
        RasterTileSource s = RasterTileSource.openStreetMap();
        assertFalse(s.isVector());
        assertTrue(s.getMaxZoom() >= 18);
        assertTrue(s.getAttribution().contains("OpenStreetMap"));
    }

    // ---- WebMercator ------------------------------------------------------

    @Test
    void webMercatorWorldSizeDoublesPerZoom() {
        assertEquals(256.0, WebMercator.worldSize(0), 1e-6);
        assertEquals(512.0, WebMercator.worldSize(1), 1e-6);
        assertEquals(256.0 * 1024, WebMercator.worldSize(10), 1e-3);
        // Equator projects to the vertical center at any zoom.
        assertEquals(WebMercator.worldSize(5) / 2, WebMercator.latToWorldY(0, 5), 1e-6);
        // Prime meridian projects to the horizontal center.
        assertEquals(WebMercator.worldSize(5) / 2, WebMercator.lonToWorldX(0, 5), 1e-6);
    }

    // ---- MvtDecoder value types ------------------------------------------

    @Test
    void mvtDecoderReadsAllValueTypes() throws Exception {
        VectorTile tile = MvtDecoder.decode(buildValueTile());
        VectorLayer layer = tile.getLayer("test");
        assertNotNull(layer);
        VectorFeature f = (VectorFeature) layer.getFeatures().get(0);
        assertEquals(VectorFeature.GEOM_POINT, f.getGeometryType());
        Map attrs = f.getAttributes();
        assertEquals("hi", attrs.get("s"));
        assertEquals(new Float(1.5f), attrs.get("f"));
        assertEquals(new Double(2.5), attrs.get("d"));
        assertEquals(new Long(7), attrs.get("i"));
        assertEquals(new Long(8), attrs.get("u"));
        assertEquals(Boolean.TRUE, attrs.get("b"));
        int[] geom = (int[]) f.getParts().get(0);
        assertEquals(10, geom[0]);
        assertEquals(20, geom[1]);
    }

    @Test
    void webMercatorIsInvertibleAcrossZooms() {
        double[] lons = {-179.9, -122.4194, -13.0, 0.0, 13.4050, 122.4194, 179.9};
        double[] lats = {-85.0, -37.8136, -33.9, 0.0, 37.8136, 51.5, 85.0};
        for (int z = 0; z <= 18; z += 3) {
            for (double lon : lons) {
                double wx = WebMercator.lonToWorldX(lon, z);
                assertEquals(lon, WebMercator.worldXToLon(wx, z), 1e-6);
            }
            for (double lat : lats) {
                double wy = WebMercator.latToWorldY(lat, z);
                assertEquals(lat, WebMercator.worldYToLat(wy, z), 1e-6);
            }
        }
        // World doubles each zoom; the equator/prime-meridian sit at the centre.
        assertEquals(256.0 * (1 << 7), WebMercator.worldSize(7), 1e-3);
        assertEquals(WebMercator.worldSize(9) / 2, WebMercator.lonToWorldX(0, 9), 1e-6);
        assertEquals(WebMercator.worldSize(9) / 2, WebMercator.latToWorldY(0, 9), 1e-6);
    }

    // ---- Pixel ratio (high-DPI scaling) ----------------------------------

    @Test
    void pixelRatioScalesViewportSpanAndKeepsCenter() {
        VectorMapEngine e = new VectorMapEngine(MvtTileSource.openFreeMap(), MapStyle.light());
        e.setCenter(new LatLng(37.806, -122.412));
        e.setZoom(13);
        e.setViewport(768, 768);

        e.setPixelRatio(1.0);
        MapBounds b1 = e.getVisibleBounds();
        double span1 = b1.getNorthEast().getLongitude() - b1.getSouthWest().getLongitude();

        e.setPixelRatio(3.0);
        MapBounds b3 = e.getVisibleBounds();
        double span3 = b3.getNorthEast().getLongitude() - b3.getSouthWest().getLongitude();

        // 3x the pixel ratio shows a third of the geographic span at the same
        // zoom (this is what stops a retina map from spanning the whole grid
        // and floating in the background).
        assertEquals(span1 / 3.0, span3, span1 * 0.02);

        // The camera target always projects to the viewport centre regardless
        // of pixel ratio.
        Point c = e.latLngToScreen(new LatLng(37.806, -122.412));
        assertEquals(384, c.getX());
        assertEquals(384, c.getY());

        // screenToLatLng is the inverse of latLngToScreen under scaling.
        LatLng back = e.screenToLatLng(600, 200);
        Point round = e.latLngToScreen(back);
        assertEquals(600, round.getX(), 1);
        assertEquals(200, round.getY(), 1);
    }

    @Test
    void mvtDecoderHandlesEmptyTile() throws Exception {
        VectorTile tile = MvtDecoder.decode(new byte[0]);
        assertEquals(0, tile.getLayers().size());
    }

    private static byte[] buildValueTile() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ProtoWriter tile = new ProtoWriter(out);

        ByteArrayOutputStream lbuf = new ByteArrayOutputStream();
        ProtoWriter layer = new ProtoWriter(lbuf);
        layer.writeString(1, "test");

        // one POINT feature at (10,20) tagging each key to each value index.
        ByteArrayOutputStream fbuf = new ByteArrayOutputStream();
        ProtoWriter f = new ProtoWriter(fbuf);
        List tags = new ArrayList();
        for (int i = 0; i < 6; i++) {
            tags.add(new Integer(i));
            tags.add(new Integer(i));
        }
        f.writePackedInt32(2, tags);
        f.writeInt32(3, VectorFeature.GEOM_POINT);
        List geom = new ArrayList();
        geom.add(new Integer((1 & 0x7) | (1 << 3)));
        geom.add(new Integer(ProtoWriter.zigZag32(10)));
        geom.add(new Integer(ProtoWriter.zigZag32(20)));
        f.writePackedInt32(4, geom);
        layer.writeBytes(2, fbuf.toByteArray());

        String[] keys = {"s", "f", "d", "i", "u", "b"};
        for (int i = 0; i < keys.length; i++) {
            layer.writeString(3, keys[i]);
        }
        layer.writeBytes(4, value(1, "hi"));
        layer.writeBytes(4, valueFloat(1.5f));
        layer.writeBytes(4, valueDouble(2.5));
        layer.writeBytes(4, valueInt64(7));
        layer.writeBytes(4, valueUInt64(8));
        layer.writeBytes(4, valueBool(true));
        layer.writeInt32(5, 4096);

        tile.writeBytes(3, lbuf.toByteArray());
        return out.toByteArray();
    }

    private static byte[] value(int field, String s) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        new ProtoWriter(b).writeString(field, s);
        return b.toByteArray();
    }

    private static byte[] valueFloat(float v) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        new ProtoWriter(b).writeFloat(2, v);
        return b.toByteArray();
    }

    private static byte[] valueDouble(double v) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        new ProtoWriter(b).writeDouble(3, v);
        return b.toByteArray();
    }

    private static byte[] valueInt64(long v) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        new ProtoWriter(b).writeInt64(4, v);
        return b.toByteArray();
    }

    private static byte[] valueUInt64(long v) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        new ProtoWriter(b).writeUInt64(5, v);
        return b.toByteArray();
    }

    private static byte[] valueBool(boolean v) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        new ProtoWriter(b).writeBool(7, v);
        return b.toByteArray();
    }
}
