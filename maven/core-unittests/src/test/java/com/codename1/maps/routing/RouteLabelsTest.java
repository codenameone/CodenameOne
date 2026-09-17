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
package com.codename1.maps.routing;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.maps.LatLng;
import com.codename1.maps.MapBounds;
import com.codename1.maps.MapView;
import com.codename1.maps.Marker;
import com.codename1.maps.MarkerOptions;
import com.codename1.maps.Polyline;
import com.codename1.maps.vector.DemoTileSource;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

class RouteLabelsTest extends UITestBase {
    @AfterEach
    void restoreRouteService() {
        Routing.setService(null);
    }

    @FormTest
    void showRouteAddsDefaultStopLabelsBeforeReportingSuccess() {
        RouteRequest request = new RouteRequest(new LatLng(1, 2), new LatLng(3, 4))
                .addWaypoint(new LatLng(2, 3)).addWaypoint(new LatLng(2.5, 3.5));
        Routing.setService(successfulService());
        final RecordingMap map = new RecordingMap();
        Routing.showRoute(map, request, new RouteCallback() {
            @Override
            public void routesFound(List routes) {
                assertEquals(1, map.polylines);
                assertEquals(1, map.fits);
                assertEquals(4, map.markers.size());
            }

            @Override
            public void routeFailed(String message, Throwable error) {
                throw new AssertionError(message, error);
            }
        });
        flushSerialCalls();
        assertEquals("Start", map.markers.get(0).getLabel());
        assertEquals("Stop 1", map.markers.get(1).getLabel());
        assertEquals("Stop 2", map.markers.get(2).getLabel());
        assertEquals("Destination", map.markers.get(3).getLabel());
        assertEquals(request.getOrigin(), map.markers.get(0).getPosition());
        assertEquals(request.getWaypoints().get(0), map.markers.get(1).getPosition());
        assertEquals(request.getDestination(), map.markers.get(3).getPosition());
        for (Marker marker : map.markers) {
            assertEquals(marker.getLabel(), marker.getTitle(), "native providers retain the name as a title");
            assertTrue(map.bounds.contains(marker.getPosition()), "requested stops may be off the snapped route");
        }
    }

    @FormTest
    void customNamesAndNullWaypointsStayAligned() {
        RouteRequest request = new RouteRequest(new LatLng(1, 2), new LatLng(3, 4))
                .setOriginLabel("Home").setDestinationLabel("Museum")
                .addWaypoint(null, "Ignored").addWaypoint(new LatLng(2, 3), "Cafe")
                .addWaypoint(new LatLng(2.5, 3.5), " ");
        assertEquals(2, request.getWaypointLabels().size());
        assertThrows(UnsupportedOperationException.class, () -> request.getWaypointLabels().add("bad"));
        Routing.setService(successfulService());
        RecordingMap map = new RecordingMap();
        Routing.showRoute(map, request, null);
        flushSerialCalls();
        assertEquals("Home", map.markers.get(0).getLabel());
        assertEquals("Cafe", map.markers.get(1).getLabel());
        assertEquals("Stop 2", map.markers.get(2).getLabel());
        assertEquals("Museum", map.markers.get(3).getLabel());
    }

    @FormTest
    void labelsCanBeDisabledAndFailuresAddNoMarkers() {
        Routing.setService(successfulService());
        RecordingMap map = new RecordingMap();
        Routing.showRoute(map, new RouteRequest(new LatLng(1, 2), new LatLng(3, 4))
                .setShowStopLabels(false), null);
        flushSerialCalls();
        assertEquals(1, map.polylines);
        assertTrue(map.markers.isEmpty());
        Routing.setService(failingService());
        Routing.showRoute(map, new RouteRequest(new LatLng(1, 2), new LatLng(3, 4)), null);
        flushSerialCalls();
        assertTrue(map.markers.isEmpty());
        assertEquals(1, map.polylines);
    }

    @FormTest
    void asynchronousRouteUsesTheSubmittedNamesAndStops() {
        final RouteRequest[] received = new RouteRequest[1];
        final RouteCallback[] pending = new RouteCallback[1];
        Routing.setService(new RouteService() {
            public String getId() { return "deferred"; }
            public boolean isAvailable() { return true; }
            public void findRoutes(RouteRequest request, RouteCallback callback) {
                received[0] = request;
                pending[0] = callback;
            }
        });
        RouteRequest request = new RouteRequest(new LatLng(1, 2), new LatLng(3, 4))
                .setOriginLabel("Home").addWaypoint(new LatLng(2, 3), "Cafe")
                .setTravelMode(TravelMode.CYCLING).setAlternatives(true).setSteps(false);
        RecordingMap map = new RecordingMap();
        Routing.showRoute(map, request, null);
        request.setOriginLabel("Changed").addWaypoint(new LatLng(2.5, 3.5), "Later")
                .setShowStopLabels(false);
        assertNotSame(request, received[0]);
        assertEquals(TravelMode.CYCLING, received[0].getTravelMode());
        assertTrue(received[0].isAlternatives());
        assertFalse(received[0].isSteps());
        assertEquals(1, received[0].getWaypoints().size());
        pending[0].routesFound(java.util.Arrays.asList(sampleRoute()));
        flushSerialCalls();
        assertEquals(3, map.markers.size());
        assertEquals("Home", map.markers.get(0).getLabel());
        assertEquals("Cafe", map.markers.get(1).getLabel());
    }

    private static Route sampleRoute() {
        return new Route(java.util.Arrays.asList(new LatLng(1.1, 2.1), new LatLng(2.9, 3.9)),
                null, 1000, 100, "Main Street");
    }


    private RouteService successfulService() {
        return new RouteService() {
            public String getId() { return "success"; }
            public boolean isAvailable() { return true; }
            public void findRoutes(RouteRequest request, RouteCallback callback) {
                callback.routesFound(java.util.Arrays.asList(sampleRoute()));
            }
        };
    }

    private RouteService failingService() {
        return new RouteService() {
            public String getId() { return "failure"; }
            public boolean isAvailable() { return true; }
            public void findRoutes(RouteRequest request, RouteCallback callback) {
                callback.routeFailed("No route", null);
            }
        };
    }

    private static class RecordingMap extends MapView {
        private int polylines;
        private int fits;
        private MapBounds bounds;
        private final List<Marker> markers = new ArrayList<Marker>();

        RecordingMap() {
            super(new DemoTileSource());
        }

        @Override
        public Polyline addPolyline(Polyline polyline) {
            polylines++;
            return super.addPolyline(polyline);
        }

        @Override
        public Marker addMarker(MarkerOptions options) {
            Marker marker = super.addMarker(options);
            markers.add(marker);
            return marker;
        }

        @Override
        public void fitBounds(MapBounds bounds, int padding) {
            fits++;
            this.bounds = bounds;
            super.fitBounds(bounds, padding);
        }
    }
}
