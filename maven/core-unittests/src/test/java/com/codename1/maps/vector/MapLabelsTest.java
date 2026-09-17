/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.maps.vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapLabelsTest {
    @Test
    void builtInStylesIncludeStreetParkAndLandmarkNamesAtDetailZooms() {
        VectorTile tile = new VectorTile(Arrays.asList(
                layer("place", named("Town", VectorFeature.GEOM_POINT, new int[]{100, 100})),
                layer("transportation_name", named("Main Street", VectorFeature.GEOM_LINESTRING,
                        new int[]{100, 200, 300, 200})),
                layer("park", named("City Park", VectorFeature.GEOM_POINT, new int[]{800, 800})),
                layer("poi", named("Museum", VectorFeature.GEOM_POINT, new int[]{1500, 1500}))));
        for (MapStyle style : new MapStyle[]{MapStyle.light(), MapStyle.dark()}) {
            assertEquals(Arrays.asList("Town"), texts(labels(tile, style, 11)));
            assertEquals(Arrays.asList("Town", "Main Street", "City Park"), texts(labels(tile, style, 12)));
            List<LabelCandidate> detail = labels(tile, style, 14);
            assertEquals(Arrays.asList("Town", "Main Street", "City Park", "Museum"), texts(detail));
            for (LabelCandidate label : detail) {
                assertEquals(detail.get(0).textColor, label.textColor);
                assertEquals(detail.get(0).haloColor, label.haloColor);
            }
        }
    }

    @Test
    void alternateRoadLayersSupplyNames() {
        for (String source : new String[]{"road", "road_label"}) {
            VectorTile tile = new VectorTile(Arrays.asList(layer(source,
                    named("High Street", VectorFeature.GEOM_LINESTRING, new int[]{0, 100, 4096, 100}))));
            assertEquals(Arrays.asList("High Street"), texts(labels(tile, MapStyle.light(), 13)));
        }
    }

    @Test
    void curvedRoadLabelStaysOnTheRoadAndUsesWorldCoordinates() {
        // Dense vertices near the start must not pull the label off the bend.
        VectorTile tile = new VectorTile(Arrays.asList(layer("transportation_name",
                named("Bent Street", VectorFeature.GEOM_LINESTRING,
                        new int[]{0, 0, 100, 0, 200, 0, 1000, 0, 1000, 3000}))));
        LabelCandidate label = labels(tile, MapStyle.light(), 13).get(0);
        assertEquals(2 * 256 + 1000 / 16.0, label.worldX, 1e-9);
        assertEquals(3 * 256 + 1000 / 16.0, label.worldY, 1e-9);
        assertEquals(13, label.tileZoom);
    }

    @Test
    void multipartRoadUsesLongestPartAndSkipsZeroLengthSegments() {
        VectorTile tile = new VectorTile(Arrays.asList(layer("transportation_name",
                named("Long Street", VectorFeature.GEOM_LINESTRING,
                        new int[]{0, 0, 20, 0}, new int[]{100, 200, 100, 200, 1100, 200}))));
        LabelCandidate label = labels(tile, MapStyle.light(), 13).get(0);
        assertEquals(2 * 256 + 600 / 16.0, label.worldX, 1e-9);
        assertEquals(3 * 256 + 200 / 16.0, label.worldY, 1e-9);
    }

    @Test
    void absentNamesDegenerateRoadsAndBufferedAnchorsProduceNoLabels() {
        VectorTile tile = new VectorTile(Arrays.asList(layer("transportation_name",
                named(null, VectorFeature.GEOM_LINESTRING, new int[]{0, 0, 100, 0}),
                named("  ", VectorFeature.GEOM_LINESTRING, new int[]{0, 0, 100, 0}),
                named("Empty", VectorFeature.GEOM_LINESTRING, new int[0]),
                named("Zero", VectorFeature.GEOM_LINESTRING, new int[]{100, 100, 100, 100}),
                named("Outside", VectorFeature.GEOM_LINESTRING, new int[]{-300, 100, -100, 100}))));
        assertTrue(labels(tile, MapStyle.light(), 13).isEmpty());
    }

    @Test
    void customStyleKeepsControlOfLabelSelection() {
        MapStyle style = MapStyle.fromJson("{\"layers\":[{\"type\":\"symbol\","
                + "\"source-layer\":\"transportation_name\",\"minzoom\":13,"
                + "\"filter\":[\"==\",\"class\",\"primary\"],"
                + "\"layout\":{\"text-field\":\"{ref}\"}}]}");
        VectorFeature road = named("Main Street", VectorFeature.GEOM_LINESTRING,
                new int[]{0, 100, 1000, 100});
        road.getAttributes().put("ref", "A1");
        road.getAttributes().put("class", "primary");
        VectorTile tile = new VectorTile(Arrays.asList(layer("transportation_name", road)));
        assertTrue(labels(tile, style, 12).isEmpty());
        assertEquals(Arrays.asList("A1"), texts(labels(tile, style, 13)));
        road.getAttributes().put("class", "secondary");
        assertTrue(labels(tile, style, 13).isEmpty());
    }

    private static VectorFeature named(String name, int geometry, int[]... parts) {
        Map attributes = new HashMap();
        if (name != null) {
            attributes.put("name", name);
        }
        return new VectorFeature(0, geometry, attributes, Arrays.asList(parts));
    }

    private static VectorLayer layer(String name, VectorFeature... features) {
        return new VectorLayer(name, 4096, Arrays.asList(features));
    }

    private static List<LabelCandidate> labels(VectorTile tile, MapStyle style, int zoom) {
        return TileRenderer.extractLabels(tile, style, zoom, 2, 3, 256);
    }

    private static List<String> texts(List<LabelCandidate> labels) {
        List<String> result = new java.util.ArrayList<String>();
        for (LabelCandidate label : labels) {
            result.add(label.text);
        }
        return result;
    }
}
