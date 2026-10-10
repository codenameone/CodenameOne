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
package com.codename1.maps.vector;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.maps.LatLng;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import org.mockito.invocation.Invocation;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.spy;

/// What the map asks its source for, and what it shows while it waits.
///
/// A view that has just moved wants every tile it shows. Handed to the source
/// all at once they are fetched in the order they were asked for -- the ones a
/// drag left behind included, each ahead of whatever the view is showing now.
class TileQueueTest extends UITestBase {

    @FormTest
    void onlyAFewTilesAreAskedForAtOnceAndTheMiddleOfTheViewGoesFirst() {
        HoldingSource source = new HoldingSource(14);
        VectorMapEngine engine = engine(source, 14);
        assertFalse(engine.hasRenderedVisibleTiles());
        assertEquals(6, source.requests.size(), "asked for " + source.requests);
        assertEquals(tileAt(SAN_FRANCISCO, 14), source.requests.get(0), "the middle first");
        assertTrue(engine.hasPendingTiles());

        // Asking again changes nothing: they are on their way or in the queue.
        assertFalse(engine.hasRenderedVisibleTiles());
        assertEquals(6, source.requests.size());

        // And each answer makes room for one more.
        source.answerOne();
        long deadline = System.currentTimeMillis() + 4000;
        while (source.requests.size() < 7) {
            assertTrue(System.currentTimeMillis() < deadline, "an answer made no room");
            flushSerialCalls();
        }
        assertEquals(7, source.requests.size());
    }

    @FormTest
    void tilesTheViewLeftBehindAreNeverAskedFor() {
        HoldingSource source = new HoldingSource(14);
        VectorMapEngine engine = engine(source, 14);
        engine.hasRenderedVisibleTiles();
        List first = new ArrayList(source.requests);

        // Somewhere else entirely, before the queue behind those six was sent.
        engine.setCenter(LONDON);
        engine.hasRenderedVisibleTiles();
        source.failAll();
        flushSerialCalls();

        List later = source.requests.subList(first.size(), source.requests.size());
        assertFalse(later.isEmpty(), "the new view is fetched once there is room");
        assertEquals(tileAt(LONDON, 14), later.get(0), "its middle first");
        for (Object request : later) {
            assertTrue(((String) request).startsWith("14/81"), request + " is not in the new view");
        }
        // Nothing waits for a tile that was dropped unsent: back where it was,
        // the map asks again for what it never got.
        while (!source.held.isEmpty()) {
            source.failAll();
            flushSerialCalls();
        }
        engine.setCenter(SAN_FRANCISCO);
        int before = source.requests.size();
        engine.hasRenderedVisibleTiles();
        assertTrue(source.requests.size() > before, "a dropped tile is asked for again");
    }

    @FormTest
    void aSourceThatAnswersAtOnceIsDrainedWithoutRecursion() {
        // Every tile fails before fetchTile returns, as an offline source's does.
        final List requests = new ArrayList();
        VectorMapEngine engine = engine(new Source(14) {
            public void fetchTile(int z, int x, int y, TileCallback callback) {
                requests.add(z + "/" + x + "/" + y);
                callback.tileFailed(z, x, y);
            }
        }, 14);
        assertFalse(engine.hasRenderedVisibleTiles());
        assertTrue(requests.size() > 6, "the whole view was asked for: " + requests);
        assertFalse(engine.hasPendingTiles());
    }

    @FormTest
    void aTileThatFailedIsAskedForAgainAfterAWhile() throws Exception {
        HoldingSource source = new HoldingSource(14);
        VectorMapEngine engine = engine(source, 14);
        engine.setViewport(200, 200);
        engine.hasRenderedVisibleTiles();
        while (!source.held.isEmpty()) {
            source.failAll();
            flushSerialCalls();
        }
        int asked = source.requests.size();

        engine.hasRenderedVisibleTiles();
        assertEquals(asked, source.requests.size(), "not at once");

        Thread.sleep(2200);
        engine.hasRenderedVisibleTiles();
        assertTrue(source.requests.size() > asked, "one lost request is not a hole for good");
    }

    @FormTest
    void aTileStillLoadingShowsTheShallowerTileThatCoversIt() {
        // Level 13 is answered and level 14 is not.
        HoldingSource source = new HoldingSource(14);
        source.answersTo = 13;
        VectorMapEngine engine = engine(source, 13);
        engine.setViewport(200, 200);
        long deadline = System.currentTimeMillis() + 4000;
        while (!engine.hasRenderedVisibleTiles()) {
            assertTrue(System.currentTimeMillis() < deadline, "level 13 never rendered");
            flushSerialCalls();
        }

        TestCodenameOneImplementation drawing = spy(implementation);
        Graphics g = graphics(drawing);
        VectorMapEngine empty = engine(new HoldingSource(14), 14);
        empty.paintTiles(g, 0, 0, 200, 200);
        assertEquals(0, imagesDrawn(drawing), "nothing to show where nothing has arrived");

        engine.setZoom(14);
        engine.paintTiles(g, 0, 0, 200, 200);
        assertTrue(engine.hasPendingTiles(), "level 14 is still on its way");
        assertTrue(imagesDrawn(drawing) > 0, "the level above stands in for it");
    }

    // However the port in use draws an image at a size.
    private static int imagesDrawn(TestCodenameOneImplementation drawing) {
        int drawn = 0;
        for (Invocation invocation : mockingDetails(drawing).getInvocations()) {
            if (invocation.getMethod().getName().startsWith("drawImage")) {
                drawn++;
            }
        }
        return drawn;
    }

    private static final LatLng SAN_FRANCISCO = new LatLng(37.7749, -122.4194);
    private static final LatLng LONDON = new LatLng(51.5074, -0.1278);

    private static VectorMapEngine engine(TileSource source, int zoom) {
        VectorMapEngine engine = new VectorMapEngine(source, MapStyle.light());
        engine.setCenter(SAN_FRANCISCO);
        engine.setZoom(zoom);
        engine.setViewport(1024, 1024);
        return engine;
    }

    private static String tileAt(LatLng where, int zoom) {
        int x = (int) (WebMercator.lonToWorldX(where.getLongitude(), zoom) / 256);
        int y = (int) (WebMercator.latToWorldY(where.getLatitude(), zoom) / 256);
        return zoom + "/" + x + "/" + y;
    }

    private Graphics graphics(TestCodenameOneImplementation drawing) {
        Graphics graphics = Image.createImage(1024, 1024).getGraphics();
        try {
            java.lang.reflect.Field field = Graphics.class.getDeclaredField("impl");
            field.setAccessible(true);
            field.set(graphics, drawing);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return graphics;
    }

    private abstract static class Source implements TileSource {
        private final int deepest;

        Source(int deepest) {
            this.deepest = deepest;
        }

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
            return deepest;
        }

        public String getAttribution() {
            return "";
        }
    }

    // Holds every request until the test answers it.
    private static final class HoldingSource extends Source {
        private final List requests = new ArrayList();
        private final List held = new ArrayList();
        private final DemoTileSource bytes = new DemoTileSource();
        // Levels up to this one are answered as they are asked for.
        private int answersTo = -1;

        HoldingSource(int deepest) {
            super(deepest);
        }

        public void fetchTile(int z, int x, int y, TileCallback callback) {
            requests.add(z + "/" + x + "/" + y);
            if (z <= answersTo) {
                bytes.fetchTile(z, x, y, callback);
                return;
            }
            held.add(new Object[]{new int[]{z, x, y}, callback});
        }

        void answerOne() {
            Object[] h = (Object[]) held.remove(0);
            int[] a = (int[]) h[0];
            bytes.fetchTile(a[0], a[1], a[2], (TileCallback) h[1]);
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
}
