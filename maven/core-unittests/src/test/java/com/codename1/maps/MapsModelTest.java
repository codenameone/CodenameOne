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
package com.codename1.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.maps.spi.MapProvider;
import com.codename1.maps.spi.MapProviderRegistry;
import com.codename1.maps.vector.WebMercator;
import com.codename1.ui.PeerComponent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for the modern maps value types, object model and provider SPI. */
class MapsModelTest {

    // ---- LatLng -----------------------------------------------------------

    @Test
    void latLngClampsLatitudeAndWrapsLongitude() {
        assertEquals(90.0, new LatLng(120, 0).getLatitude(), 1e-9);
        assertEquals(-90.0, new LatLng(-120, 0).getLatitude(), 1e-9);
        assertEquals(-170.0, new LatLng(0, 190).getLongitude(), 1e-9);
        assertEquals(170.0, new LatLng(0, -190).getLongitude(), 1e-9);
        assertEquals(0.0, new LatLng(0, 360).getLongitude(), 1e-9);
    }

    @Test
    void latLngEqualsAndHashCode() {
        LatLng a = new LatLng(37.7749, -122.4194);
        LatLng b = new LatLng(37.7749, -122.4194);
        LatLng c = new LatLng(40.0, -122.4194);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, "not a latlng");
    }

    @Test
    void latLngHaversineDistance() {
        // San Francisco to New York is ~4130 km.
        double d = new LatLng(37.7749, -122.4194).distanceTo(new LatLng(40.7128, -74.0060));
        assertTrue(d > 4_100_000 && d < 4_200_000, "distance was " + d);
        assertEquals(0.0, new LatLng(10, 20).distanceTo(new LatLng(10, 20)), 1e-3);
    }

    @Test
    void latLngCoordRoundTrip() {
        LatLng a = new LatLng(51.5074, -0.1278);
        Coord c = a.toCoord();
        assertFalse(c.isProjected());
        assertEquals(a, LatLng.fromCoord(c));
        assertEquals(a, LatLng.create(51.5074, -0.1278));
    }

    // ---- MapBounds --------------------------------------------------------

    @Test
    void mapBoundsNormalizesCornersAndContains() {
        MapBounds b = new MapBounds(new LatLng(40, 10), new LatLng(30, -10));
        assertEquals(30.0, b.getSouthWest().getLatitude(), 1e-9);
        assertEquals(-10.0, b.getSouthWest().getLongitude(), 1e-9);
        assertEquals(40.0, b.getNorthEast().getLatitude(), 1e-9);
        assertEquals(10.0, b.getNorthEast().getLongitude(), 1e-9);
        assertTrue(b.contains(new LatLng(35, 0)));
        assertFalse(b.contains(new LatLng(50, 0)));
        assertEquals(35.0, b.getCenter().getLatitude(), 1e-9);
        assertEquals(10.0, b.getLatitudeSpan(), 1e-9);
        assertEquals(20.0, b.getLongitudeSpan(), 1e-9);
    }

    @Test
    void mapBoundsFromCoordinatesAndExtend() {
        assertNull(MapBounds.fromCoordinates(new ArrayList()));
        List pts = new ArrayList();
        pts.add(new LatLng(10, 10));
        pts.add(new LatLng(-5, 20));
        pts.add(new LatLng(3, -8));
        MapBounds b = MapBounds.fromCoordinates(pts);
        assertEquals(-5.0, b.getSouthWest().getLatitude(), 1e-9);
        assertEquals(-8.0, b.getSouthWest().getLongitude(), 1e-9);
        assertEquals(10.0, b.getNorthEast().getLatitude(), 1e-9);
        assertEquals(20.0, b.getNorthEast().getLongitude(), 1e-9);
        MapBounds extended = b.extend(new LatLng(40, 40));
        assertTrue(extended.contains(new LatLng(40, 40)));
        assertTrue(extended.contains(new LatLng(-5, -8)));
    }

    // ---- CameraPosition ---------------------------------------------------

    @Test
    void cameraPositionAccessorsAndWithers() {
        LatLng t = new LatLng(1, 2);
        CameraPosition p = new CameraPosition(t, 5);
        assertSame(t, p.getTarget());
        assertEquals(5.0, p.getZoom(), 1e-9);
        assertEquals(0.0, p.getBearing(), 1e-9);
        assertEquals(0.0, p.getTilt(), 1e-9);
        CameraPosition p2 = new CameraPosition(t, 5, 90, 30);
        assertEquals(90.0, p2.getBearing(), 1e-9);
        assertEquals(30.0, p2.getTilt(), 1e-9);
        assertEquals(8.0, p.withZoom(8).getZoom(), 1e-9);
        LatLng t2 = new LatLng(3, 4);
        assertSame(t2, p.withTarget(t2).getTarget());
    }

    // ---- Object model -----------------------------------------------------

    @Test
    void markerOptionsBuildsMarkerWithDefaults() {
        Marker m = new MarkerOptions(new LatLng(1, 2)).title("t").snippet("s").build();
        assertEquals(new LatLng(1, 2), m.getPosition());
        assertEquals("t", m.getTitle());
        assertEquals("s", m.getSnippet());
        assertEquals(0.5f, m.getAnchorU(), 1e-6);
        assertEquals(1.0f, m.getAnchorV(), 1e-6);
        assertFalse(m.isDraggable());
        assertTrue(m.isVisible());
        assertNull(m.getIcon());
        m.setVisible(false);
        assertFalse(m.isVisible());
        m.setPosition(new LatLng(3, 4));
        assertEquals(new LatLng(3, 4), m.getPosition());
    }

    @Test
    void markerOptionsAnchorAndDraggable() {
        Marker m = new MarkerOptions().position(new LatLng(0, 0)).anchor(0.25f, 0.75f).draggable(true).build();
        assertEquals(0.25f, m.getAnchorU(), 1e-6);
        assertEquals(0.75f, m.getAnchorV(), 1e-6);
        assertTrue(m.isDraggable());
    }

    @Test
    void polylineAccessors() {
        Polyline pl = new Polyline().addPoint(new LatLng(0, 0)).addPoint(new LatLng(1, 1));
        assertEquals(2, pl.getPoints().size());
        pl.setStrokeColor(0x123456).setStrokeWidth(7).setStrokeAlpha(128).setVisible(false);
        assertEquals(0x123456, pl.getStrokeColor());
        assertEquals(7, pl.getStrokeWidth());
        assertEquals(128, pl.getStrokeAlpha());
        assertFalse(pl.isVisible());
        Polyline fromArray = new Polyline(new LatLng[]{new LatLng(0, 0), new LatLng(2, 2), new LatLng(3, 3)});
        assertEquals(3, fromArray.getPoints().size());
    }

    @Test
    void polygonAccessors() {
        Polygon pg = new Polygon(new LatLng[]{new LatLng(0, 0), new LatLng(0, 1), new LatLng(1, 1)});
        assertEquals(3, pg.getPoints().size());
        pg.setFillColor(0x80ff0000).setStrokeColor(0x00ff00).setStrokeWidth(4).setVisible(false);
        assertEquals(0x80ff0000, pg.getFillColor());
        assertEquals(0x00ff00, pg.getStrokeColor());
        assertEquals(4, pg.getStrokeWidth());
        assertFalse(pg.isVisible());
    }

    @Test
    void circleAccessors() {
        Circle c = new Circle(new LatLng(5, 6), 1000);
        assertEquals(new LatLng(5, 6), c.getCenter());
        assertEquals(1000.0, c.getRadiusMeters(), 1e-9);
        c.setCenter(new LatLng(7, 8)).setRadiusMeters(2000).setFillColor(0x11223344)
                .setStrokeColor(0x556677).setStrokeWidth(3).setVisible(false);
        assertEquals(new LatLng(7, 8), c.getCenter());
        assertEquals(2000.0, c.getRadiusMeters(), 1e-9);
        assertEquals(0x11223344, c.getFillColor());
        assertEquals(0x556677, c.getStrokeColor());
        assertEquals(3, c.getStrokeWidth());
        assertFalse(c.isVisible());
    }

    @Test
    void mapObjectIdsAreUniqueAndEqualityById() {
        Marker a = new MarkerOptions(new LatLng(0, 0)).build();
        Marker b = new MarkerOptions(new LatLng(0, 0)).build();
        assertNotEquals(a.getId(), b.getId());
        assertNotEquals(a, b);
        assertEquals(a, a);
        assertEquals(a.getId(), a.hashCode());
    }

    // ---- Provider SPI registry -------------------------------------------

    @Test
    void mapProviderRegistrySelectionRules() {
        StubProvider unavailable = new StubProvider("test-unavailable", false);
        StubProvider available = new StubProvider("test-google", true);
        StubProvider availableApple = new StubProvider("test-apple", true);

        MapProviderRegistry.register(unavailable);
        assertNull(MapProviderRegistry.getProvider(), "only an unavailable provider is registered");
        assertFalse(MapProviderRegistry.hasProvider());

        MapProviderRegistry.register(available);
        assertSame(available, MapProviderRegistry.getProvider());
        assertTrue(MapProviderRegistry.hasProvider());

        MapProviderRegistry.register(availableApple);
        MapProviderRegistry.setPreferredProvider("test-apple");
        assertSame(availableApple, MapProviderRegistry.getProvider());

        // Re-registering the same id replaces the instance.
        StubProvider replacement = new StubProvider("test-apple", true);
        MapProviderRegistry.register(replacement);
        assertSame(replacement, MapProviderRegistry.getProvider());

        // Preferring an unknown id falls back to the first available provider.
        MapProviderRegistry.setPreferredProvider("does-not-exist");
        assertNotNull(MapProviderRegistry.getProvider());
        assertTrue(MapProviderRegistry.getProvider().isAvailable());

        // A provider whose isAvailable throws is treated as absent, not fatal.
        MapProviderRegistry.setPreferredProvider("test-throws");
        MapProviderRegistry.register(new StubProvider("test-throws", true) {
            public boolean isAvailable() {
                throw new RuntimeException("native init failed");
            }
        });
        assertNotNull(MapProviderRegistry.getProvider());
    }

    /** A no-op MapProvider used to exercise the registry without any native peer. */
    // ---- NativeMap bounds fitting ----------------------------------------

    @Test
    void nativeMapSolvesTheZoomThatFitsBounds() {
        // The whole world spans exactly one 256pt tile at zoom 0, so a 256px
        // viewport fits it at zoom 0 and every doubling adds one level.
        MapBounds world = new MapBounds(new LatLng(-85.05112878, -180),
                new LatLng(85.05112878, 180));
        assertEquals(0.0, NativeMap.zoomToFit(world, 256, 256, 0, 1.0), 1e-6);
        assertEquals(1.0, NativeMap.zoomToFit(world, 512, 512, 0, 1.0), 1e-6);
        assertEquals(2.0, NativeMap.zoomToFit(world, 1024, 1024, 0, 1.0), 1e-6);

        // Padding shrinks the usable viewport, and so the zoom.
        assertEquals(0.0, NativeMap.zoomToFit(world, 512, 512, 128, 1.0), 1e-6);

        // Native zoom counts logical points, so a 2x density screen fits the
        // same region at one level less than its device-pixel size suggests.
        assertEquals(0.0, NativeMap.zoomToFit(world, 512, 512, 0, 2.0), 1e-6);

        // The tighter of the two axes wins: a wide, short viewport is limited
        // by its height here.
        double fit = NativeMap.zoomToFit(world, 2048, 256, 0, 1.0);
        assertEquals(0.0, fit, 1e-6);
    }

    @Test
    void mercatorCenterIsTheProjectedMidpointNotTheMean() {
        // Mercator stretches away from the equator, so a band from 0 to 80
        // is visually centred near 57, not 40. Centring on the mean leaves the
        // northern edge outside a viewport the fitted zoom just sized for it.
        assertEquals(57.045, WebMercator.centerLatitude(0, 80), 1e-3);
        assertEquals(-57.045, WebMercator.centerLatitude(-80, 0), 1e-3);

        // Symmetric bands still centre on the equator, and a degenerate band
        // on itself.
        assertEquals(0.0, WebMercator.centerLatitude(-45, 45), 1e-9);
        assertEquals(37.7749, WebMercator.centerLatitude(37.7749, 37.7749), 1e-6);

        // Near the equator the distortion is negligible, so it stays close to
        // the arithmetic mean.
        assertEquals(1.0, WebMercator.centerLatitude(0, 2), 1e-3);
    }

    @Test
    void nativeMapReportsNoFitWhenThereIsNothingToFitTo() {
        MapBounds world = new MapBounds(new LatLng(-85, -180), new LatLng(85, 180));
        // Before layout there is no viewport to solve against.
        assertTrue(Double.isNaN(NativeMap.zoomToFit(world, 0, 0, 0, 1.0)));
        assertTrue(Double.isNaN(NativeMap.zoomToFit(world, 256, 0, 0, 1.0)));
        assertTrue(Double.isNaN(NativeMap.zoomToFit(null, 256, 256, 0, 1.0)));
        assertTrue(Double.isNaN(NativeMap.zoomToFit(world, 256, 256, 0, 0)));

        // A single point has no extent, so no zoom "fits" it.
        MapBounds point = new MapBounds(new LatLng(37.7749, -122.4194),
                new LatLng(37.7749, -122.4194));
        assertTrue(Double.isNaN(NativeMap.zoomToFit(point, 256, 256, 0, 1.0)));
    }

    @Test
    void nativeMapZoomsFurtherForASmallerRegion() {
        // A tighter region must fit at a higher zoom than a wider one; this is
        // the property that was missing when fitBounds only recentered.
        MapBounds city = new MapBounds(new LatLng(37.70, -122.52), new LatLng(37.83, -122.35));
        MapBounds block = new MapBounds(new LatLng(37.7740, -122.4200), new LatLng(37.7760, -122.4180));
        double cityZoom = NativeMap.zoomToFit(city, 800, 800, 0, 1.0);
        double blockZoom = NativeMap.zoomToFit(block, 800, 800, 0, 1.0);
        assertTrue(blockZoom > cityZoom, "block " + blockZoom + " should out-zoom city " + cityZoom);
    }

    private static class StubProvider implements MapProvider {
        private final String id;
        private final boolean available;

        StubProvider(String id, boolean available) {
            this.id = id;
            this.available = available;
        }

        public String getId() {
            return id;
        }

        public boolean isAvailable() {
            return available;
        }

        public PeerComponent createPeer(NativeMap host, int mapId) {
            return null;
        }

        public void deinitialize(int mapId) {
        }

        public void setCamera(int mapId, double lat, double lon, float zoom, float bearing, float tilt) {
        }

        public double getLatitude(int mapId) {
            return 0;
        }

        public double getLongitude(int mapId) {
            return 0;
        }

        public float getZoom(int mapId) {
            return 0;
        }

        public float getMaxZoom(int mapId) {
            return 0;
        }

        public float getMinZoom(int mapId) {
            return 0;
        }

        public long addMarker(int mapId, byte[] icon, double lat, double lon, String title,
                              String snippet, float anchorU, float anchorV) {
            return 0;
        }

        public long beginPath(int mapId) {
            return 0;
        }

        public void addToPath(int mapId, long pathId, double lat, double lon) {
        }

        public long finishPolyline(int mapId, long pathId, int strokeColor, int strokeWidth) {
            return 0;
        }

        public long finishPolygon(int mapId, long pathId, int fillColor, int strokeColor, int strokeWidth) {
            return 0;
        }

        public long addCircle(int mapId, double lat, double lon, double radiusMeters, int fillColor,
                              int strokeColor, int strokeWidth) {
            return 0;
        }

        public void removeElement(int mapId, long elementId) {
        }

        public void removeAllElements(int mapId) {
        }

        public void calcScreenPosition(int mapId, double lat, double lon) {
        }

        public int getScreenX(int mapId) {
            return 0;
        }

        public int getScreenY(int mapId) {
            return 0;
        }

        public void calcLatLongPosition(int mapId, int x, int y) {
        }

        public double getScreenLat(int mapId) {
            return 0;
        }

        public double getScreenLon(int mapId) {
            return 0;
        }

        public void setShowMyLocation(int mapId, boolean show) {
        }

        public void setRotateGestureEnabled(int mapId, boolean enabled) {
        }

        public void setMapType(int mapId, int type) {
        }
    }
}
