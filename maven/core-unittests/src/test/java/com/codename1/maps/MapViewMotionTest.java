/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package com.codename1.maps;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.maps.vector.MapStyle;
import com.codename1.maps.vector.TileCallback;
import com.codename1.maps.vector.TileSource;
import com.codename1.ui.events.WheelEvent;
import com.codename1.ui.geom.Dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// How the map moves once the gesture that moved it is over, and what a zoom
/// is centred on.
class MapViewMotionTest extends UITestBase {

    @FormTest
    void aMapLetGoWhileMovingCarriesOnAndThenStops() throws Exception {
        MapView map = sizedMap();
        final int[] heard = {0};
        map.addCameraChangeListener(new CameraChangeListener() {
            public void cameraChanged(MapSurface source, CameraPosition position) {
                heard[0]++;
            }
        });
        map.pointerPressed(50, 150);
        for (int i = 1; i <= 8; i++) {
            Thread.sleep(8);
            map.pointerDragged(50 + i * 12, 150);
        }
        map.pointerReleased(146, 150);
        double released = map.getCenter().getLongitude();
        assertEquals(0, heard[0], "the camera has not come to rest yet");

        settle(map);
        assertTrue(map.getCenter().getLongitude() < released - 1e-6,
                "content thrown to the right keeps going right");
        assertEquals(1, heard[0], "and the listeners hear once, when it stops");
        double rest = map.getCenter().getLongitude();
        Thread.sleep(30);
        assertFalse(map.animate());
        assertEquals(rest, map.getCenter().getLongitude(), 0.0);
    }

    @FormTest
    void aMapPutDownStaysWhereItWasPut() throws Exception {
        MapView map = sizedMap();
        map.pointerPressed(50, 150);
        for (int i = 1; i <= 8; i++) {
            Thread.sleep(8);
            map.pointerDragged(50 + i * 12, 150);
        }
        // The finger rests before it lifts.
        Thread.sleep(200);
        map.pointerReleased(146, 150);
        double released = map.getCenter().getLongitude();
        Thread.sleep(30);
        assertFalse(map.animate());
        assertEquals(released, map.getCenter().getLongitude(), 0.0);
    }

    @FormTest
    void aFingerPutDownOnAMovingMapStopsIt() throws Exception {
        MapView map = sizedMap();
        map.pointerPressed(50, 150);
        for (int i = 1; i <= 8; i++) {
            Thread.sleep(8);
            map.pointerDragged(50 + i * 12, 150);
        }
        map.pointerReleased(146, 150);
        Thread.sleep(20);
        assertTrue(map.animate());
        map.pointerPressed(100, 100);
        double held = map.getCenter().getLongitude();
        Thread.sleep(30);
        assertFalse(map.animate());
        assertEquals(held, map.getCenter().getLongitude(), 0.0);
    }

    @FormTest
    void aDoubleTapGlidesOneLevelInAboutTheTap() throws Exception {
        MapView map = sizedMap();
        double zoom = map.getZoom();
        LatLng under = map.screenToLatLng(70, 220);
        map.pointerPressed(70, 220);
        map.pointerReleased(70, 220);
        map.pointerPressed(70, 220);
        map.pointerReleased(70, 220);
        assertTrue(map.getZoom() < zoom + 1, "glided to, not jumped to");

        settle(map);
        assertEquals(zoom + 1, map.getZoom(), 0.0);
        assertSamePlace(under, map.screenToLatLng(70, 220));
    }

    @FormTest
    void theWheelZoomsAboutThePointerOnlyWhenAskedTo() throws Exception {
        MapView map = sizedMap();
        double zoom = map.getZoom();
        assertFalse(map.isWheelZoom());
        map.mouseWheel(new WheelEvent(map, 40, 60, 0, 200, true, 0));
        assertEquals(zoom, map.getZoom(), 0.0, "a wheel pans unless told otherwise");

        map.setWheelZoom(true);
        LatLng under = map.screenToLatLng(40, 60);
        double step = 200 / (200 * map.getEngine().getPixelRatio());
        assertTrue(map.mouseWheel(new WheelEvent(map, 40, 60, 0, 200, true, 0)));
        // A second notch before the first has finished adds to it.
        assertTrue(map.mouseWheel(new WheelEvent(map, 40, 60, 0, 200, true, 0)));
        settle(map);
        assertEquals(zoom + 2 * step, map.getZoom(), 1e-9);
        assertSamePlace(under, map.screenToLatLng(40, 60));

        // At the end of the range the wheel is not the map's to keep.
        map.setZoom(map.getMaxZoom());
        assertFalse(map.mouseWheel(new WheelEvent(map, 40, 60, 0, 200, true, 0)));
        assertTrue(map.mouseWheel(new WheelEvent(map, 40, 60, 0, -200, true, 0)));
    }

    @FormTest
    void aPinchZoomsAboutTheFingersAndTravelsWithThem() {
        MapView map = sizedMap();
        double zoom = map.getZoom();
        LatLng under = map.screenToLatLng(60, 80);
        map.pinch(new int[]{40, 80}, new int[]{80, 80});
        assertTrue(map.pinch(1f));
        map.pinch(new int[]{20, 100}, new int[]{80, 80});
        assertTrue(map.pinch(2f));
        assertEquals(zoom + 1, map.getZoom(), 1e-6);
        assertSamePlace(under, map.screenToLatLng(60, 80));

        // The fingers move on together: the ground between them goes along.
        map.pinch(new int[]{60, 140}, new int[]{120, 120});
        assertTrue(map.pinch(2f));
        assertSamePlace(under, map.screenToLatLng(100, 120));
    }

    @FormTest
    void aPinchThatLeavesNoPointerBehindStillEnds() {
        MapView map = sizedMap();
        double zoom = map.getZoom();
        final int[] heard = {0};
        map.addCameraChangeListener(new CameraChangeListener() {
            public void cameraChanged(MapSurface source, CameraPosition position) {
                heard[0]++;
            }
        });
        // A trackpad: scales, and a release, and no pointer events at all.
        map.pinch(1f);
        map.pinch(2f);
        map.pinchReleased(0, 0);
        assertEquals(1, heard[0]);
        assertEquals(zoom + 1, map.getZoom(), 1e-6);

        map.pinch(1f);
        assertEquals(zoom + 1, map.getZoom(), 1e-6,
                "the next pinch starts from where the last one ended");
    }

    private static void assertSamePlace(LatLng expected, LatLng actual) {
        assertEquals(expected.getLatitude(), actual.getLatitude(), 0.05);
        assertEquals(expected.getLongitude(), actual.getLongitude(), 0.05);
    }

    private static void settle(MapView map) throws Exception {
        long deadline = System.currentTimeMillis() + 10000;
        Thread.sleep(10);
        while (map.animate()) {
            assertTrue(System.currentTimeMillis() < deadline, "the map never came to rest");
            Thread.sleep(10);
        }
    }

    private MapView sizedMap() {
        MapView map = new MapView(new NoTiles(), MapStyle.light());
        map.setSize(new Dimension(300, 300));
        map.setWidth(300);
        map.setHeight(300);
        map.getEngine().setViewport(300, 300);
        map.moveCamera(new LatLng(37.7749, -122.4194), 12);
        return map;
    }

    /// A source that serves nothing: none of this waits for a tile.
    private static final class NoTiles implements TileSource {

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
            return 18;
        }

        public String getAttribution() {
            return "";
        }

        public void fetchTile(int z, int x, int y, TileCallback callback) {
        }
    }
}
