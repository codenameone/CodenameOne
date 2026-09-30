/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.maps.vector;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.maps.LatLng;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Stroke;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RoadLabelTest extends UITestBase {
    private static final int TEXT = 0xff333333;
    private static final int HALO = 0xffffffff;

    private TestCodenameOneImplementation drawing;

    @FormTest
    void roadNameIsDrawnAlongTheRoadAtItsDirection() {
        Graphics g = graphics(true);
        // A road running down and to the right at 30 degrees.
        double c = Math.cos(Math.PI / 6);
        double s = Math.sin(Math.PI / 6);
        double[] road = {100, 100, 100 + 400 * c, 100 + 400 * s};
        assertTrue(new LabelEngine().placeAlongLine(g, "Main Street", 13, TEXT, HALO, road, 1000,
                0, 0, 800, 800));
        verify(drawing, times(5)).drawString(any(), eq("Main Street"), anyInt(), anyInt());
        float[] dir = direction(lastTransform());
        assertEquals(c, dir[0], 1e-3);
        assertEquals(s, dir[1], 1e-3);
    }

    @FormTest
    void roadNameStaysUprightOnARoadDrawnRightToLeft() {
        Graphics g = graphics(true);
        double[] road = {500, 200, 100, 200};
        assertTrue(new LabelEngine().placeAlongLine(g, "Main Street", 13, TEXT, HALO, road, 1000,
                0, 0, 800, 800));
        float[] dir = direction(lastTransform());
        assertEquals(1, dir[0], 1e-3);
        assertEquals(0, dir[1], 1e-3);
    }

    @FormTest
    void aGentleBendPlacesEachGlyphOnTheCurve() {
        Graphics g = graphics(true);
        List points = new ArrayList();
        for (int i = 0; i <= 20; i++) {
            double a = Math.PI * i / 40;
            points.add(Double.valueOf(300 + 300 * Math.sin(a)));
            points.add(Double.valueOf(100 + 300 * (1 - Math.cos(a))));
        }
        double[] road = new double[points.size()];
        for (int i = 0; i < road.length; i++) {
            road[i] = ((Double) points.get(i)).doubleValue();
        }
        assertTrue(new LabelEngine().placeAlongLine(g, "Curved Road Name", 13, TEXT, HALO, road, 5000,
                0, 0, 800, 800));
        verify(drawing, never()).drawString(any(), eq("Curved Road Name"), anyInt(), anyInt());
        verify(drawing, atLeastOnce()).drawString(any(), eq("C"), anyInt(), anyInt());
    }

    @FormTest
    void aSharpCornerUnderTheNameDropsIt() {
        Graphics g = graphics(true);
        // The corner sits exactly under the middle of the name.
        double[] road = {100, 100, 300, 100, 300, 300};
        assertFalse(new LabelEngine().placeAlongLine(g, "Corner Street", 13, TEXT, HALO, road, 1000,
                0, 0, 800, 800));
        verify(drawing, never()).drawString(any(), anyString(), anyInt(), anyInt());
        // The same length of straight road holds the name: the corner is the reason.
        double[] straight = {100, 500, 500, 500};
        assertTrue(new LabelEngine().placeAlongLine(g, "Corner Street", 13, TEXT, HALO, straight, 1000,
                0, 0, 800, 800));
    }

    @FormTest
    void aRoadShorterThanItsNameIsNotLabelled() {
        Graphics g = graphics(true);
        double[] road = {100, 100, 120, 100};
        assertFalse(new LabelEngine().placeAlongLine(g, "A Rather Long Name", 13, TEXT, HALO, road, 1000,
                0, 0, 800, 800));
    }

    @FormTest
    void aLongRoadRepeatsItsNameAndTheSameNameKeepsItsDistance() {
        Graphics g = graphics(true);
        LabelEngine engine = new LabelEngine();
        double[] road = {0, 100, 2000, 100};
        assertTrue(engine.placeAlongLine(g, "Long Road", 13, TEXT, HALO, road, 300, 0, 0, 2000, 800));
        // Each copy is the text plus four halo passes.
        ArgumentCaptor<Integer> x = ArgumentCaptor.forClass(Integer.class);
        verify(drawing, atLeastOnce()).drawString(any(), eq("Long Road"), x.capture(), anyInt());
        List starts = new ArrayList(new HashSet(x.getAllValues()));
        assertTrue(starts.size() / 3 >= 5, "expected repeats, got " + starts);

        // The next tile's piece of the same road, a little further along,
        // must not stack a second name next to the first.
        double[] neighbour = {850, 180, 1150, 180};
        assertFalse(engine.placeAlongLine(g, "Long Road", 13, TEXT, HALO, neighbour, 300, 0, 0, 2000, 800));
        assertTrue(engine.placeAlongLine(g, "Other Road", 13, TEXT, HALO, neighbour, 300, 0, 0, 2000, 800));
    }

    @FormTest
    void withoutAffineTransformsTheNameIsHorizontalAtTheMidpoint() {
        Graphics g = graphics(false);
        double[] road = {100, 100, 500, 300};
        assertTrue(new LabelEngine().placeAlongLine(g, "Main Street", 13, TEXT, HALO, road, 1000,
                0, 0, 800, 800));
        verify(drawing, never()).setTransform(any(), any(Transform.class));
        verify(drawing, times(5)).drawString(any(), eq("Main Street"), anyInt(), anyInt());
    }

    @FormTest
    void roadCandidatesCarryTheRoadInWorldPixels() {
        VectorTile tile = new VectorTile(Arrays.asList(
                layer("transportation_name", named("Main Street", VectorFeature.GEOM_LINESTRING,
                        new int[]{160, 320, 1760, 320})),
                layer("place", named("Town", VectorFeature.GEOM_POINT, new int[]{100, 100}))));
        List labels = TileRenderer.extractLabels(tile, MapStyle.light(), 13, 2, 3, 256);
        LabelCandidate road = null;
        LabelCandidate town = null;
        for (Object o : labels) {
            LabelCandidate c = (LabelCandidate) o;
            if ("Main Street".equals(c.text)) {
                road = c;
            } else {
                town = c;
            }
        }
        assertNotNull(road);
        assertNotNull(town);
        assertNull(town.path);
        assertEquals(4, road.path.length);
        assertEquals(2 * 256 + 10, road.path[0], 1e-9);
        assertEquals(3 * 256 + 20, road.path[1], 1e-9);
        assertEquals(2 * 256 + 110, road.path[2], 1e-9);
        assertEquals(3 * 256 + 20, road.path[3], 1e-9);
    }

    @FormTest
    void scriptsThatNeedShapingAreDrawnWholeOnACurve() {
        String[] shaped = {
            "\u0634\u0627\u0631\u0639 \u0627\u0644\u0645\u0644\u0643",  // Arabic: letters join
            "\u05e8\u05d7\u05d5\u05d1 \u05d4\u05e8\u05e6\u05dc",        // Hebrew: right to left
            "Cafe\u0301 Street Upper",                                 // combining acute accent
            "Street \ud835\udc00 North",                               // a surrogate pair
            "\u306f\u3099\u3057\u901a\u308a",                            // kana + combining dakuten
            "\u6f22\u302a\u5b57\u901a\u308a"                             // ideographic tone mark
        };
        for (String name : shaped) {
            assertFalse(LabelEngine.drawsCharByChar(name), name);
            Graphics g = graphics(true);
            assertTrue(new LabelEngine().placeAlongLine(g, name, 13, TEXT, HALO, gentleArc(), 5000,
                    0, 0, 800, 800), name);
            // One shaped string (text plus its four halo passes), never a lone char.
            verify(drawing, times(5)).drawString(any(), eq(name), anyInt(), anyInt());
            ArgumentCaptor<String> drawn = ArgumentCaptor.forClass(String.class);
            verify(drawing, times(5)).drawString(any(), drawn.capture(), anyInt(), anyInt());
            for (String s : drawn.getAllValues()) {
                assertEquals(name, s);
            }
        }
    }

    @FormTest
    void aShapedNameIsDroppedWhereTheRoadBendsAwayFromIt() {
        Graphics g = graphics(true);
        // A tight arc: the chord across the name leaves the road by more than
        // half a line, so drawing it whole would put it beside the road.
        double[] road = arc(40, 60, Math.PI / 2);
        String name = "\u0634\u0627\u0631\u0639 \u0627\u0644\u0645\u0644\u0643";
        assertFalse(new LabelEngine().placeAlongLine(g, name, 13, TEXT, HALO, road, 5000,
                0, 0, 800, 800));
        verify(drawing, never()).drawString(any(), anyString(), anyInt(), anyInt());
    }

    @FormTest
    void plainScriptsStillFollowTheCurveGlyphByGlyph() {
        String[] plain = {"Curved Road Name", "Stra\u00dfe", "\u0443\u043b\u0438\u0446\u0430",
            "\u9280\u5ea7\u901a\u308a"};
        for (String name : plain) {
            assertTrue(LabelEngine.drawsCharByChar(name), name);
        }
    }

    @FormTest
    void overzoomedLabelsFollowTheZoomOnScreen() {
        MapStyle style = MapStyle.fromJson("{\"layers\":["
                + "{\"type\":\"symbol\",\"source-layer\":\"poi\",\"minzoom\":16,"
                + "\"layout\":{\"text-field\":\"{name}\"}},"
                + "{\"type\":\"symbol\",\"source-layer\":\"place\",\"maxzoom\":14,"
                + "\"layout\":{\"text-field\":\"{name}\"}}]}");
        VectorTile tile = new VectorTile(Arrays.asList(
                layer("poi", named("Cafe", VectorFeature.GEOM_POINT, new int[]{160, 320})),
                layer("place", named("Town", VectorFeature.GEOM_POINT, new int[]{800, 800}))));
        // Shown at the source's own zoom 14: the place, not the zoom-16 poi.
        assertEquals(Arrays.asList("Town"), texts(TileRenderer.extractLabels(tile, style, 14, 14, 2, 3, 256)));
        // Overzoomed to 16 from that same zoom-14 tile: the poi appears, the
        // place that ends at 14 does not, and the anchor stays in zoom-14 pixels.
        List shown = TileRenderer.extractLabels(tile, style, 16, 14, 2, 3, 256);
        assertEquals(Arrays.asList("Cafe"), texts(shown));
        LabelCandidate cafe = (LabelCandidate) shown.get(0);
        assertEquals(14, cafe.tileZoom);
        assertEquals(2 * 256 + 10, cafe.worldX, 1e-9);
        assertEquals(3 * 256 + 20, cafe.worldY, 1e-9);
    }

    private static List texts(List labels) {
        List out = new ArrayList();
        for (Object o : labels) {
            out.add(((LabelCandidate) o).text);
        }
        return out;
    }

    // Bends ~23 degrees over 400px: a curve, but one a whole name can follow
    // without leaving the road by more than half a line.
    private static double[] gentleArc() {
        return arc(1000, 100, 0.4);
    }

    private static double[] arc(double radius, double offset, double sweep) {
        double[] road = new double[42];
        for (int i = 0; i <= 20; i++) {
            double a = sweep * i / 20;
            road[i * 2] = offset + radius * Math.sin(a);
            road[i * 2 + 1] = 100 + radius * (1 - Math.cos(a));
        }
        return road;
    }

    @FormTest
    void ferryRoutesAreNotLabelledBecauseTheirLinesAreNotDrawn() {
        VectorFeature ferry = named("Sausalito - San Francisco Ferry Building",
                VectorFeature.GEOM_LINESTRING, new int[]{100, 100, 3000, 3000});
        ferry.getAttributes().put("class", "ferry");
        VectorFeature street = named("Bay Street", VectorFeature.GEOM_LINESTRING,
                new int[]{100, 2000, 3000, 2000});
        street.getAttributes().put("class", "minor");
        for (String source : new String[]{"transportation_name", "road", "road_label"}) {
            VectorTile tile = new VectorTile(Arrays.asList(layer(source, ferry, street)));
            for (MapStyle style : new MapStyle[]{MapStyle.light(), MapStyle.dark()}) {
                List labels = TileRenderer.extractLabels(tile, style, 14, 2, 3, 256);
                assertEquals(1, labels.size(), source);
                assertEquals("Bay Street", ((LabelCandidate) labels.get(0)).text);
            }
        }
    }

    @FormTest
    void anOverzoomedPieceDrawsOnlyItsQuarterOfTheTileAtFullScale() {
        // A line inside the top-left quarter of the tile only, clear of the
        // centre, where its round cap would reach into the other quarters.
        VectorTile tile = new VectorTile(Arrays.asList(
                layer("road", new VectorFeature(0, VectorFeature.GEOM_LINESTRING, new HashMap(),
                        Arrays.asList(new Object[]{new int[]{0, 0, 1536, 1536}})))));
        Graphics g = graphics(true);
        TileRenderer.renderTile(g, tile, MapStyle.light(), 15, 256, 1, 1, 1);
        verify(drawing, never()).drawShape(any(), any(Shape.class), any(Stroke.class));

        g = graphics(true);
        TileRenderer.renderTile(g, tile, MapStyle.light(), 15, 256, 0, 0, 1);
        ArgumentCaptor<Shape> shape = ArgumentCaptor.forClass(Shape.class);
        verify(drawing).drawShape(any(), shape.capture(), any(Stroke.class));
        Rectangle bounds = shape.getValue().getBounds();
        assertEquals(0, bounds.getX());
        assertEquals(0, bounds.getY());
        // Twice the scale of the whole tile: 1536 of 4096 units is 192 of 256px.
        assertEquals(192, bounds.getWidth(), 1);
        assertEquals(192, bounds.getHeight(), 1);
    }

    @FormTest
    void aVectorMapZoomsPastItsDeepestTilesAndARasterMapDoesNot() {
        VectorMapEngine vector = new VectorMapEngine(new RecordingSource(true, 14), MapStyle.light());
        assertEquals(20, vector.getMaxZoom(), 0);
        vector.setZoom(25);
        assertEquals(20, vector.getZoom(), 0);

        assertEquals(22, new VectorMapEngine(new RecordingSource(true, 18), MapStyle.light()).getMaxZoom(), 0);

        VectorMapEngine raster = new VectorMapEngine(new RecordingSource(false, 19), MapStyle.light());
        assertEquals(19, raster.getMaxZoom(), 0);
        raster.setZoom(21);
        assertEquals(19, raster.getZoom(), 0);
    }

    @FormTest
    void overzoomedTilesFetchTheirDeepestAncestorOnce() {
        RecordingSource source = new RecordingSource(true, 14);
        VectorMapEngine engine = new VectorMapEngine(source, MapStyle.light());
        engine.setCenter(new LatLng(37.4225, -122.1096));
        engine.setZoom(17);
        engine.setViewport(1024, 1024);
        assertFalse(engine.hasRenderedVisibleTiles());
        assertFalse(source.requests.isEmpty());
        for (Object r : source.requests) {
            assertTrue(((String) r).startsWith("14/"), "fetched " + r);
        }
        assertEquals(new HashSet(source.requests).size(), source.requests.size(),
                "each ancestor is fetched once: " + source.requests);
        // A 1024px view at z17 covers at most 5x5 tiles, which is at most 2x2 z14 tiles.
        assertTrue(source.requests.size() <= 4, "fetched " + source.requests);
    }

    @FormTest
    void zoomingInWhileTheDeepestTileLoadsReusesItsFetch() {
        HoldingSource source = new HoldingSource();
        VectorMapEngine engine = new VectorMapEngine(source, MapStyle.light());
        engine.setCenter(new LatLng(37.4225, -122.1096));
        engine.setViewport(512, 512);
        engine.setZoom(14);
        assertFalse(engine.hasRenderedVisibleTiles());
        List first = new ArrayList(source.requests);
        assertFalse(first.isEmpty());
        // Past the source's deepest zoom while those tiles are still loading.
        engine.setZoom(16);
        assertFalse(engine.hasRenderedVisibleTiles());
        assertEquals(first, source.requests, "no second download of a tile already loading");
        // When the deepest tiles land, the zoom-16 pieces are cut from them.
        source.release();
        awaitRendered(engine);
    }

    @FormTest
    void zoomingOutWhileAnOverzoomFetchLoadsReusesItToo() {
        HoldingSource source = new HoldingSource();
        VectorMapEngine engine = new VectorMapEngine(source, MapStyle.light());
        engine.setCenter(new LatLng(37.4225, -122.1096));
        engine.setViewport(512, 512);
        engine.setZoom(16);
        assertFalse(engine.hasRenderedVisibleTiles());
        List first = new ArrayList(source.requests);
        assertFalse(first.isEmpty());
        engine.setZoom(14);
        assertFalse(engine.hasRenderedVisibleTiles());
        for (Object r : source.requests.subList(first.size(), source.requests.size())) {
            assertFalse(first.contains(r), "fetched " + r + " twice");
        }
        source.release();
        awaitRendered(engine);
    }

    @FormTest
    void aFailedDeepestTileFailsThePiecesWaitingOnIt() {
        HoldingSource source = new HoldingSource();
        VectorMapEngine engine = new VectorMapEngine(source, MapStyle.light());
        engine.setCenter(new LatLng(37.4225, -122.1096));
        engine.setViewport(512, 512);
        engine.setZoom(14);
        engine.hasRenderedVisibleTiles();
        engine.setZoom(16);
        engine.hasRenderedVisibleTiles();
        source.failAll();
        flushSerialCalls();
        assertFalse(engine.hasPendingTiles(), "a piece still waits on a tile that failed");
    }

    @FormTest
    void withoutAffineTheNameGoesOnTheVisiblePartOfALongRoad() {
        Graphics g = graphics(false);
        // An overzoomed road: thousands of pixels long, its middle far off
        // screen, crossing the 800x800 view only near its start.
        double[] road = {-200, 400, 20000, 400};
        assertTrue(new LabelEngine().placeAlongLine(g, "Main Street", 13, TEXT, HALO, road, 1000,
                0, 0, 800, 800));
        ArgumentCaptor<Integer> x = ArgumentCaptor.forClass(Integer.class);
        verify(drawing, atLeastOnce()).drawString(any(), eq("Main Street"), x.capture(), anyInt());
        for (Integer left : x.getAllValues()) {
            assertTrue(left > 0 && left < 800, "drawn at x=" + left);
        }
        // And nothing when no part of it is in view.
        Graphics other = graphics(false);
        assertFalse(new LabelEngine().placeAlongLine(other, "Main Street", 13, TEXT, HALO,
                new double[]{2000, 400, 20000, 400}, 1000, 0, 0, 800, 800));
    }

    @FormTest
    void overzoomStopsAtTwentyTwoButNeverBelowTheSourcesOwnDeepestLevel() {
        assertEquals(22, new VectorMapEngine(new RecordingSource(true, 18), MapStyle.light()).getMaxZoom(), 0);
        assertEquals(24, new VectorMapEngine(new RecordingSource(true, 24), MapStyle.light()).getMaxZoom(), 0);
    }

    private void awaitRendered(VectorMapEngine engine) {
        long deadline = System.currentTimeMillis() + 5000;
        while (!engine.hasRenderedVisibleTiles()) {
            assertTrue(System.currentTimeMillis() < deadline, "tiles never rendered");
            flushSerialCalls();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        }
        assertFalse(engine.hasPendingTiles());
    }

    // A vector source with data to zoom 14 that holds every request until the
    // test releases it, then answers with real tile bytes.
    private static final class HoldingSource implements TileSource {
        private final List requests = new ArrayList();
        private final List held = new ArrayList();
        private final DemoTileSource bytes = new DemoTileSource();

        public boolean isVector() {
            return true;
        }

        public int getTileSize() {
            return 256;
        }

        public int getMinZoom() {
            return 0;
        }

        public int getMaxZoom() {
            return 14;
        }

        public String getAttribution() {
            return "";
        }

        public void fetchTile(int z, int x, int y, TileCallback callback) {
            requests.add(z + "/" + x + "/" + y);
            held.add(new Object[]{new int[]{z, x, y}, callback});
        }

        void release() {
            List now = new ArrayList(held);
            held.clear();
            for (Object o : now) {
                Object[] h = (Object[]) o;
                int[] a = (int[]) h[0];
                bytes.fetchTile(a[0], a[1], a[2], (TileCallback) h[1]);
            }
        }

        void failAll() {
            List now = new ArrayList(held);
            held.clear();
            for (Object o : now) {
                Object[] h = (Object[]) o;
                int[] a = (int[]) h[0];
                ((TileCallback) h[1]).tileFailed(a[0], a[1], a[2]);
            }
        }
    }

    private static float[] direction(Transform t) {
        float[] origin = t.transformPoint(new float[]{0, 0});
        float[] unit = t.transformPoint(new float[]{1, 0});
        return new float[]{unit[0] - origin[0], unit[1] - origin[1]};
    }

    private Transform lastTransform() {
        ArgumentCaptor<Transform> t = ArgumentCaptor.forClass(Transform.class);
        verify(drawing, atLeastOnce()).setTransform(any(), t.capture());
        List all = t.getAllValues();
        // The last call restores the caller's transform; the one before it
        // is the rotation the name was drawn with.
        return (Transform) all.get(all.size() - 2);
    }

    private Graphics graphics(boolean affine) {
        Graphics graphics = Image.createImage(800, 800).getGraphics();
        drawing = spy(implementation);
        drawing.setShapeSupported(true);
        doReturn(Boolean.valueOf(affine)).when(drawing).isAffineSupported();
        try {
            java.lang.reflect.Field field = Graphics.class.getDeclaredField("impl");
            field.setAccessible(true);
            field.set(graphics, drawing);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return graphics;
    }

    private static VectorFeature named(String name, int geometry, int[]... parts) {
        Map attributes = new HashMap();
        attributes.put("name", name);
        return new VectorFeature(0, geometry, attributes, Arrays.asList((Object[]) parts));
    }

    private static VectorLayer layer(String name, VectorFeature... features) {
        return new VectorLayer(name, 4096, Arrays.asList(features));
    }

    private static final class RecordingSource implements TileSource {
        private final boolean vector;
        private final int maxZoom;
        private final List requests = new ArrayList();

        RecordingSource(boolean vector, int maxZoom) {
            this.vector = vector;
            this.maxZoom = maxZoom;
        }

        public boolean isVector() {
            return vector;
        }

        public int getTileSize() {
            return 256;
        }

        public int getMinZoom() {
            return 0;
        }

        public int getMaxZoom() {
            return maxZoom;
        }

        public String getAttribution() {
            return "";
        }

        public void fetchTile(int z, int x, int y, TileCallback callback) {
            // Never answers: the test is about what gets asked for.
            requests.add(z + "/" + x + "/" + y);
        }
    }
}
