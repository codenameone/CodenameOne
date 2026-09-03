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

/// What a map does with a wheel it cannot act on.
///
/// Web Mercator stops at its latitude limits, and a map that has reached one has nothing
/// left to give a downward gesture. Holding on to it anyway is how a page carrying a map
/// stops scrolling at exactly the latitude a reader is most likely to be looking at.
class MapViewWheelTest extends UITestBase {

    @FormTest
    void aGestureTheMapCannotActOnGoesToThePage() {
        MapView map = sizedMap();

        // North until Mercator refuses to go further. Pure vertical, so nothing but the
        // latitude is in question while the map is being put where the test needs it.
        int guard = 0;
        while (map.mouseWheel(wheel(map, 0, 400)) && guard++ < 500) {
            // panning
        }
        assertTrue(guard < 500, "the map has to reach its latitude limit");
        assertFalse(map.mouseWheel(wheel(map, 0, 400)),
                "and a wheel it cannot act on is not its to keep");

        // The real case: the same pinned gesture with the sideways jitter every trackpad
        // swipe carries. The longitude can still move, and counting that as a pan is what
        // stopped the page under the map from scrolling.
        double lon = map.getCenter().getLongitude();
        double lat = map.getCenter().getLatitude();
        assertFalse(map.mouseWheel(wheel(map, -12, 400)),
                "a downward gesture at the limit belongs to the page, jitter or not");
        assertEquals(lon, map.getCenter().getLongitude(), 0.0,
                "and a gesture handed on leaves the map where it was");
        assertEquals(lat, map.getCenter().getLatitude(), 0.0, "on both axes");
    }

    @FormTest
    void aSidewaysGestureAtTheLatitudeLimitIsStillTheMapsToPan() {
        MapView map = sizedMap();
        int guard = 0;
        while (map.mouseWheel(wheel(map, 0, 400)) && guard++ < 500) {
            // panning
        }

        // Pinned north, but this gesture is sideways: the axis that owns it can move, so
        // the map keeps it. Passing everything on at the limit would be the same bug the
        // other way round.
        double lon = map.getCenter().getLongitude();
        assertTrue(map.mouseWheel(wheel(map, 400, 12)),
                "the axis that owns the gesture can move, so the map acts on it");
        assertTrue(Math.abs(map.getCenter().getLongitude() - lon) > 0.0,
                "and it actually panned");
    }

    private MapView sizedMap() {
        MapView map = new MapView(new NoTiles(), MapStyle.light());
        map.setSize(new Dimension(300, 300));
        map.setWidth(300);
        map.setHeight(300);
        return map;
    }

    private static WheelEvent wheel(MapView map, int deltaX, int deltaY) {
        return new WheelEvent(map, 150, 150, deltaX, deltaY, false, 0);
    }

    /// A source that serves nothing. The map's arithmetic -- which is all this asks about
    /// -- does not wait for a tile.
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
