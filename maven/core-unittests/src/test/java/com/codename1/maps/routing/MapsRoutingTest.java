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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.maps.LatLng;
import com.codename1.maps.MapBounds;
import com.codename1.maps.Polyline;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Unit tests for the routing model, the OSRM request/response handling and the Routing facade. */
class MapsRoutingTest {

    /** The Google specification sample: (38.5, -120.2), (40.7, -120.95), (43.252, -126.453). */
    private static final String GEOMETRY = "_p~iF~ps|U_ulLnnqC_mqNvxq`@";

    @AfterEach
    void restoreDefaultService() {
        Routing.setService(null);
    }

    // ---- Request ----------------------------------------------------------

    @Test
    void requestDefaultsToDrivingWithSteps() {
        RouteRequest req = new RouteRequest(new LatLng(1, 2), new LatLng(3, 4));
        assertEquals(TravelMode.DRIVING, req.getTravelMode());
        assertTrue(req.isSteps());
        assertFalse(req.isAlternatives());
        assertTrue(req.getWaypoints().isEmpty());

        assertSame(req, req.setTravelMode(null));
        assertEquals(TravelMode.DRIVING, req.getTravelMode(), "a null mode falls back to driving");
    }

    @Test
    void requestKeepsWaypointsInVisitingOrderAndIgnoresNull() {
        RouteRequest req = new RouteRequest(new LatLng(1, 2), new LatLng(3, 4))
                .addWaypoint(new LatLng(5, 6))
                .addWaypoint(null)
                .addWaypoint(new LatLng(7, 8));
        assertEquals(2, req.getWaypoints().size());
        assertEquals(new LatLng(5, 6), req.getWaypoints().get(0));
        assertEquals(new LatLng(7, 8), req.getWaypoints().get(1));
    }

    @Test
    void waypointsCannotBeCorruptedThroughTheGetter() {
        // Handing out the live list would let a caller slip past addWaypoint's
        // null filtering and blow up later inside buildUrl.
        RouteRequest req = new RouteRequest(new LatLng(1, 2), new LatLng(3, 4));
        assertThrows(UnsupportedOperationException.class, () -> req.getWaypoints().add("not a LatLng"));
        assertTrue(req.getWaypoints().isEmpty());
    }

    // ---- OSRM request URL -------------------------------------------------

    @Test
    void buildsOsrmUrlWithWaypointsInLonLatOrder() {
        OsrmRouteService service = new OsrmRouteService("https://osrm.example.com/");
        RouteRequest req = new RouteRequest(new LatLng(38.8977, -77.0365), new LatLng(38.8894, -77.0352))
                .addWaypoint(new LatLng(38.8899, -77.0091))
                .setTravelMode(TravelMode.CYCLING)
                .setAlternatives(true)
                .setSteps(false);

        assertEquals("https://osrm.example.com/route/v1/cycling/"
                        + "-77.036500,38.897700;-77.009100,38.889900;-77.035200,38.889400"
                        + "?overview=full&geometries=polyline&steps=false&alternatives=true",
                service.buildUrl(req));
    }

    @Test
    void buildUrlFormatsCoordinatesWithoutScientificNotation() {
        // A coordinate near the prime meridian is where Double.toString would
        // emit "1.0E-5" and OSRM would reject the URL.
        OsrmRouteService service = new OsrmRouteService();
        String url = service.buildUrl(new RouteRequest(new LatLng(0.00001, -0.00002), new LatLng(0, 0)));
        assertTrue(url.indexOf("-0.000020,0.000010;0.000000,0.000000") > 0, url);
        assertTrue(url.startsWith(OsrmRouteService.DEMO_BASE_URL), url);
    }

    @Test
    void baseUrlTrimsTrailingSlashesAndFallsBackToTheDemoServer() {
        assertEquals("https://osrm.example.com",
                new OsrmRouteService("https://osrm.example.com///").getBaseUrl());
        assertEquals(OsrmRouteService.DEMO_BASE_URL, new OsrmRouteService("").getBaseUrl());
        assertEquals(OsrmRouteService.DEMO_BASE_URL, new OsrmRouteService(null).getBaseUrl());
        assertTrue(new OsrmRouteService().isAvailable());
        assertEquals("osrm", new OsrmRouteService().getId());
    }

    // ---- OSRM response parsing -------------------------------------------

    @Test
    void parsesRouteGeometryDistanceAndDuration() throws IOException {
        List routes = OsrmRouteService.parseResponse(sampleResponse());
        assertEquals(1, routes.size());

        Route route = (Route) routes.get(0);
        assertEquals(1234.5, route.getDistanceMeters(), 1e-6);
        assertEquals(678.9, route.getDurationSeconds(), 1e-6);
        assertEquals("Main Street", route.getSummary());
        assertEquals(3, route.getPoints().size());

        LatLng first = (LatLng) route.getPoints().get(0);
        assertEquals(38.5, first.getLatitude(), 1e-6);
        assertEquals(-120.2, first.getLongitude(), 1e-6);
    }

    @Test
    void parsesLegsAndTurnByTurnSteps() throws IOException {
        Route route = (Route) OsrmRouteService.parseResponse(sampleResponse()).get(0);
        assertEquals(1, route.getLegs().size());

        RouteLeg leg = (RouteLeg) route.getLegs().get(0);
        assertEquals("Main Street", leg.getSummary());
        assertEquals(1234.5, leg.getDistanceMeters(), 1e-6);
        assertEquals(3, leg.getSteps().size());

        RouteStep depart = (RouteStep) leg.getSteps().get(0);
        assertEquals("Head out on Main Street", depart.getInstruction());
        assertEquals("Main Street", depart.getRoadName());
        assertEquals(100.0, depart.getDistanceMeters(), 1e-6);

        RouteStep turn = (RouteStep) leg.getSteps().get(1);
        assertEquals("Turn left onto Elm Street", turn.getInstruction());
        assertNotNull(turn.getStart());
        // OSRM reports [longitude, latitude]; the model must not swap them.
        assertEquals(40.7, turn.getStart().getLatitude(), 1e-6);
        assertEquals(-120.95, turn.getStart().getLongitude(), 1e-6);

        RouteStep arrive = (RouteStep) leg.getSteps().get(2);
        assertEquals("Arrive at your destination", arrive.getInstruction());
        assertEquals("", arrive.getRoadName());
    }

    @Test
    void routeExposesDrawableGeometryAndBounds() throws IOException {
        Route route = (Route) OsrmRouteService.parseResponse(sampleResponse()).get(0);

        Polyline pl = route.toPolyline();
        assertEquals(3, pl.getPoints().size());
        // Each call yields an independent polyline so styling one is isolated.
        assertNotSame(pl, route.toPolyline());

        MapBounds bounds = route.getBounds();
        assertNotNull(bounds);
        assertEquals(38.5, bounds.getSouthWest().getLatitude(), 1e-6);
        assertEquals(-126.453, bounds.getSouthWest().getLongitude(), 1e-6);
        assertEquals(43.252, bounds.getNorthEast().getLatitude(), 1e-6);
        assertEquals(-120.2, bounds.getNorthEast().getLongitude(), 1e-6);
    }

    @Test
    void emptyRouteHasNoBoundsAndNoGeometry() {
        Route route = new Route(null, null, 0, 0, null);
        assertNull(route.getBounds());
        assertTrue(route.getPoints().isEmpty());
        assertTrue(route.getLegs().isEmpty());
        assertEquals("", route.getSummary());
        assertTrue(route.toPolyline().getPoints().isEmpty());
    }

    @Test
    void parsesAlternativeRoutes() throws IOException {
        String json = "{\"code\":\"Ok\",\"routes\":["
                + "{\"geometry\":\"" + GEOMETRY + "\",\"distance\":100,\"duration\":10,\"legs\":[]},"
                + "{\"geometry\":\"" + GEOMETRY + "\",\"distance\":200,\"duration\":20,\"legs\":[]}]}";
        List routes = OsrmRouteService.parseResponse(json);
        assertEquals(2, routes.size());
        assertEquals(100.0, ((Route) routes.get(0)).getDistanceMeters(), 1e-6);
        assertEquals(200.0, ((Route) routes.get(1)).getDistanceMeters(), 1e-6);
    }

    @Test
    void reportsServiceLevelRoutingFailures() {
        IOException noRoute = assertThrows(IOException.class,
                () -> OsrmRouteService.parseResponse("{\"code\":\"NoRoute\"}"));
        assertEquals("No route connects those points for the selected travel mode",
                noRoute.getMessage());

        IOException noSegment = assertThrows(IOException.class,
                () -> OsrmRouteService.parseResponse("{\"code\":\"NoSegment\"}"));
        assertEquals("One of the points is too far from any road", noSegment.getMessage());

        // A message from the service wins over our generic phrasing.
        IOException withMessage = assertThrows(IOException.class,
                () -> OsrmRouteService.parseResponse("{\"code\":\"InvalidValue\",\"message\":\"bad radius\"}"));
        assertEquals("bad radius", withMessage.getMessage());
    }

    @Test
    void reportsMalformedAndEmptyResponses() {
        assertThrows(IOException.class, () -> OsrmRouteService.parseResponse(null));
        assertThrows(IOException.class, () -> OsrmRouteService.parseResponse(""));
        assertThrows(IOException.class, () -> OsrmRouteService.parseResponse("{\"code\":\"Ok\"}"));
        assertThrows(IOException.class,
                () -> OsrmRouteService.parseResponse("{\"code\":\"Ok\",\"routes\":[]}"));
    }

    @Test
    void nonObjectRouteLegAndStepEntriesRaiseIoException() {
        // parseResponse is public, so a caller feeding it a body from its own
        // transport must be able to rely on the documented IOException. A
        // non-object entry used to escape as an unchecked NullPointerException
        // or ClassCastException straight through the catch.
        assertThrows(IOException.class,
                () -> OsrmRouteService.parseResponse("{\"code\":\"Ok\",\"routes\":[null]}"));
        assertThrows(IOException.class,
                () -> OsrmRouteService.parseResponse("{\"code\":\"Ok\",\"routes\":[7]}"));
        assertThrows(IOException.class, () -> OsrmRouteService.parseResponse(
                "{\"code\":\"Ok\",\"routes\":[{\"geometry\":\"" + GEOMETRY + "\",\"legs\":[null]}]}"));
        assertThrows(IOException.class, () -> OsrmRouteService.parseResponse(
                "{\"code\":\"Ok\",\"routes\":[{\"geometry\":\"" + GEOMETRY
                        + "\",\"legs\":[{\"steps\":[\"turn left\"]}]}]}"));
    }

    // ---- Routing facade ---------------------------------------------------

    @Test
    void routingDefaultsToTheKeylessOsrmService() {
        assertInstanceOf(OsrmRouteService.class, Routing.getService());
    }

    @Test
    void routingDelegatesToTheInstalledService() {
        RecordingService service = new RecordingService();
        Routing.setService(service);
        assertSame(service, Routing.getService());

        LatLng origin = new LatLng(38.8977, -77.0365);
        LatLng destination = new LatLng(38.8894, -77.0352);
        Routing.findRoute(origin, destination, service.callback);

        assertNotNull(service.lastRequest);
        assertEquals(origin, service.lastRequest.getOrigin());
        assertEquals(destination, service.lastRequest.getDestination());
        assertEquals(TravelMode.DRIVING, service.lastRequest.getTravelMode());
    }

    @Test
    void settingANullServiceRestoresTheDefault() {
        Routing.setService(new RecordingService());
        Routing.setService(null);
        assertInstanceOf(OsrmRouteService.class, Routing.getService());
    }

    // ---- Fixtures ---------------------------------------------------------

    private static String sampleResponse() {
        return "{\"code\":\"Ok\",\"routes\":[{"
                + "\"geometry\":\"" + GEOMETRY + "\","
                + "\"distance\":1234.5,\"duration\":678.9,"
                + "\"legs\":[{\"summary\":\"Main Street\",\"distance\":1234.5,\"duration\":678.9,"
                + "\"steps\":["
                + "{\"name\":\"Main Street\",\"distance\":100.0,\"duration\":20.0,"
                + "\"geometry\":\"" + GEOMETRY + "\","
                + "\"maneuver\":{\"type\":\"depart\",\"location\":[-120.2,38.5]}},"
                + "{\"name\":\"Elm Street\",\"distance\":900.0,\"duration\":300.0,"
                + "\"maneuver\":{\"type\":\"turn\",\"modifier\":\"left\",\"location\":[-120.95,40.7]}},"
                + "{\"name\":\"\",\"distance\":0,\"duration\":0,"
                + "\"maneuver\":{\"type\":\"arrive\",\"location\":[-126.453,43.252]}}"
                + "]}]}]}";
    }

    /** A stand-in service that records the request instead of hitting the network. */
    private static final class RecordingService implements RouteService {

        private RouteRequest lastRequest;
        private final RouteCallback callback = new RouteCallback() {
            @Override
            public void routesFound(List routes) {
            }

            @Override
            public void routeFailed(String message, Throwable error) {
            }
        };

        @Override
        public String getId() {
            return "recording";
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public void findRoutes(RouteRequest request, RouteCallback cb) {
            lastRequest = request;
            cb.routesFound(new ArrayList());
        }
    }
}
