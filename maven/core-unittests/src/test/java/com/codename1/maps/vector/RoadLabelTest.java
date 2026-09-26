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
