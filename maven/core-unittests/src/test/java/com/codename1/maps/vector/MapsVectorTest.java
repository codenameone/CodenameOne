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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for the pure-Java vector map engine internals. */
class MapsVectorTest {

    @Test
    void demoTileEncodesAndDecodes() throws Exception {
        byte[] bytes = DemoTileSource.buildTile();
        assertTrue(bytes.length > 0);
        VectorTile tile = MvtDecoder.decode(bytes);
        assertEquals(5, tile.getLayers().size());

        VectorLayer water = tile.getLayer("water");
        assertNotNull(water);
        assertEquals(4096, water.getExtent());
        assertEquals(1, water.getFeatures().size());
        VectorFeature waterFeature = (VectorFeature) water.getFeatures().get(0);
        assertEquals(VectorFeature.GEOM_POLYGON, waterFeature.getGeometryType());

        VectorLayer road = tile.getLayer("road");
        assertNotNull(road);
        assertEquals(2, road.getFeatures().size());
        assertEquals(
                VectorFeature.GEOM_LINESTRING,
                ((VectorFeature) road.getFeatures().get(0)).getGeometryType());

        VectorLayer place = tile.getLayer("place");
        assertNotNull(place);
        VectorFeature placeFeature = (VectorFeature) place.getFeatures().get(0);
        assertEquals(VectorFeature.GEOM_POINT, placeFeature.getGeometryType());
        assertEquals("CN1 City", placeFeature.getAttribute("name"));
    }

    @Test
    void polygonGeometryDecodesToExpectedCoordinates() throws Exception {
        VectorTile tile = MvtDecoder.decode(DemoTileSource.buildTile());
        VectorFeature water = (VectorFeature) tile.getLayer("water").getFeatures().get(0);
        int[] ring = (int[]) water.getParts().get(0);
        // Encoded ring: (0,0)(1500,0)(1500,4096)(0,4096).
        assertEquals(0, ring[0]);
        assertEquals(0, ring[1]);
        assertEquals(1500, ring[2]);
        assertEquals(0, ring[3]);
        assertEquals(1500, ring[4]);
        assertEquals(4096, ring[5]);
    }

    @Test
    void webMercatorRoundTrips() {
        double[] lats = {0, 37.7793, -33.8688, 51.5072, 85.0};
        double[] lons = {0, -122.4193, 151.2093, -0.1276, 179.0};
        for (double zoom = 1; zoom <= 18; zoom += 3) {
            for (int i = 0; i < lats.length; i++) {
                double wx = WebMercator.lonToWorldX(lons[i], zoom);
                double wy = WebMercator.latToWorldY(lats[i], zoom);
                double lon = WebMercator.worldXToLon(wx, zoom);
                double lat = WebMercator.worldYToLat(wy, zoom);
                assertEquals(lons[i], lon, 1e-6, "lon at zoom " + zoom);
                assertEquals(lats[i], lat, 1e-6, "lat at zoom " + zoom);
            }
        }
    }

    @Test
    void tileCacheEvictsLeastRecentlyUsed() {
        TileCache cache = new TileCache(2);
        cache.put("a", "A");
        cache.put("b", "B");
        // Touch "a" so "b" becomes the eviction candidate.
        assertEquals("A", cache.get("a"));
        cache.put("c", "C");
        assertEquals(2, cache.size());
        assertNull(cache.get("b"));
        assertEquals("A", cache.get("a"));
        assertEquals("C", cache.get("c"));
    }

    @Test
    void mapStyleParsesJsonSubset() {
        String json =
                "{\"layers\":["
                        + "{\"type\":\"background\",\"paint\":{\"background-color\":\"#102030\"}},"
                        + "{\"type\":\"fill\",\"source-layer\":\"water\","
                        + "\"paint\":{\"fill-color\":\"#0000ff\"}},"
                        + "{\"type\":\"line\",\"source-layer\":\"road\","
                        + "\"paint\":{\"line-color\":\"#ffffff\",\"line-width\":2}}"
                        + "]}";
        MapStyle style = MapStyle.fromJson(json);
        assertEquals(0xff102030, style.getBackgroundColor());
        List layers = style.getLayers();
        assertEquals(2, layers.size());
        assertEquals(StyleLayer.TYPE_FILL, ((StyleLayer) layers.get(0)).getType());
        assertEquals(StyleLayer.TYPE_LINE, ((StyleLayer) layers.get(1)).getType());
        assertEquals(0xff0000ff, ((StyleLayer) layers.get(0)).getFillColor());
    }

    @Test
    void colorParserHandlesCommonForms() {
        assertEquals(0xff0000ff, ColorParser.parse("#0000ff", 0));
        assertEquals(0xffffffff, ColorParser.parse("#fff", 0));
        assertEquals(0x80ff0000, ColorParser.parse("rgba(255,0,0,0.5)", 0));
        assertEquals(0xff112233, ColorParser.parse("#112233", 0));
    }
}
