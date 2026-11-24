/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.osm;

import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OsmExtractTest {
    @TempDir
    static File dir;
    static OsmExtract extract;
    static OsmRouting routing;
    static OsmGeocoding geocoding;

    @BeforeAll
    static void load() throws Exception {
        File f = new File(dir, "town.osm.pbf");
        OsmFixture.write(f.getPath(), true, "zlib");
        extract = OsmExtract.load(f.getPath());
        routing = new OsmRouting(extract, "/osrm");
        geocoding = new OsmGeocoding(extract, "/geo/");
    }

    private static HttpServer.Response get(HttpServer.Handler h, String target) throws Exception {
        return h.handle(HttpServer.Request.of("GET", target, new HashMap<String, String>(), null));
    }

    private static Object json(HttpServer.Response r) throws Exception {
        return Json.parse(new String(r.getBody(), "UTF-8"));
    }

    private static Map route(String profile, String coords) throws Exception {
        HttpServer.Response r = get(routing, "/osrm/route/v1/" + profile + "/" + coords + "?steps=true");
        assertEquals(200, r.getStatus(), new String(r.getBody(), "UTF-8"));
        Map doc = (Map) json(r);
        assertEquals("Ok", doc.get("code"));
        return (Map) ((List) doc.get("routes")).get(0);
    }

    private static double num(Object o) {
        return ((Number) o).doubleValue();
    }

    @Test
    void loadsTheRoadsAndThePlaces() {
        assertEquals(10, extract.getRoadVertexCount());
        // Cafe, address, town, and the seven named streets.
        assertEquals(10, extract.getPlaceCount());
    }

    @Test
    void drivesAcrossTheGridWithTurnByTurnSteps() throws Exception {
        Map route = route("driving", "10,10;10.002,10.002");
        double distance = num(route.get("distance"));
        // Two blocks east and two north: 4 x ~111 m (east-west shrinks by cos 10).
        assertTrue(distance > 430 && distance < 450, "distance " + distance);
        List legs = (List) route.get("legs");
        assertEquals(1, legs.size());
        List steps = (List) ((Map) legs.get(0)).get("steps");
        Map first = (Map) ((Map) steps.get(0)).get("maneuver");
        Map last = (Map) ((Map) steps.get(steps.size() - 1)).get("maneuver");
        assertEquals("depart", first.get("type"));
        assertEquals("arrive", last.get("type"));
        boolean turned = false;
        for (Object s : steps) {
            Map m = (Map) ((Map) s).get("maneuver");
            if ("turn".equals(m.get("type"))) {
                turned = true;
                String modifier = (String) m.get("modifier");
                assertTrue("left".equals(modifier) || "right".equals(modifier), modifier);
            }
        }
        assertTrue(turned);
        List points = decode((String) route.get("geometry"));
        double[] start = (double[]) points.get(0);
        double[] end = (double[]) points.get(points.size() - 1);
        assertEquals(10.0, start[0], 1e-5);
        assertEquals(10.0, start[1], 1e-5);
        assertEquals(10.002, end[0], 1e-5);
        assertEquals(10.002, end[1], 1e-5);
    }

    @Test
    void carsKeepToTheOneWayAndWalkersDoNot() throws Exception {
        // Middle Avenue runs north only. South along it is 222 m on foot; a
        // car has to go around by an outer avenue.
        double walk = num(route("walking", "10.001,10.002;10.001,10").get("distance"));
        double drive = num(route("driving", "10.001,10.002;10.001,10").get("distance"));
        assertTrue(walk > 215 && walk < 230, "walk " + walk);
        assertTrue(drive > 400, "drive " + drive);
        double north = num(route("driving", "10.001,10;10.001,10.002").get("distance"));
        assertTrue(north > 215 && north < 230, "north " + north);
    }

    @Test
    void theFootpathIsForWalkersOnly() throws Exception {
        HttpServer.Response r = get(routing, "/osrm/route/v1/walking/10,10;10.004,10.002");
        Map doc = (Map) json(r);
        Map wp = (Map) ((List) doc.get("waypoints")).get(1);
        assertEquals(10.004, num(((List) wp.get("location")).get(0)), 1e-6);
        r = get(routing, "/osrm/route/v1/driving/10,10;10.004,10.002");
        wp = (Map) ((List) ((Map) json(r)).get("waypoints")).get(1);
        // The car snaps to the corner of the grid instead.
        assertEquals(10.002, num(((List) wp.get("location")).get(0)), 1e-6);
    }

    @Test
    void routesThroughEveryWaypoint() throws Exception {
        Map route = route("cycling", "10,10;10.002,10;10.002,10.002");
        assertEquals(2, ((List) route.get("legs")).size());
    }

    @Test
    void answersOsrmErrorCodes() throws Exception {
        assertError("/osrm/route/v1/driving/10,10", "InvalidQuery");
        assertError("/osrm/route/v1/driving/10,10;x,y", "InvalidQuery");
        assertError("/osrm/route/v1/driving/10,10;10,91", "InvalidQuery");
        assertError("/osrm/route/v1/flying/10,10;10,10.001", "InvalidOptions");
        assertError("/osrm/route/v1/driving/10,10;10,10.001?geometries=geojson", "InvalidOptions");
        assertError("/osrm/route/v1/driving/10,10;20,20", "NoSegment");
        StringBuilder many = new StringBuilder("10,10");
        for (int i = 0; i < 30; i++) {
            many.append(";10,10");
        }
        assertError("/osrm/route/v1/driving/" + many, "TooBig");
        assertNull(get(routing, "/other/route/v1/driving/10,10;10,10.001"));
        assertEquals(405, routing.handle(HttpServer.Request.of("POST", "/osrm/route/v1/driving/10,10;10,10",
                new HashMap<String, String>(), null)).getStatus());
    }

    private static void assertError(String target, String code) throws Exception {
        HttpServer.Response r = get(routing, target);
        assertEquals(400, r.getStatus());
        assertEquals(code, ((Map) json(r)).get("code"));
    }

    @Test
    void searchesByNameAccentsAndCaseIgnored() throws Exception {
        List found = (List) json(get(geocoding, "/geo/search?q=CAFE&format=json"));
        assertEquals(1, found.size());
        Map cafe = (Map) found.get(0);
        assertEquals(OsmFixture.CAFE, cafe.get("display_name"));
        assertEquals("amenity", cafe.get("class"));
        assertEquals("10.0011000", cafe.get("lat"));
        found = (List) json(get(geocoding, "/geo/search?q=cafe%20cent"));
        assertEquals(1, found.size());
    }

    @Test
    void searchesAddressesAndRanksTheTownFirst() throws Exception {
        List found = (List) json(get(geocoding, "/geo/search?q=12%20first"));
        assertEquals(1, found.size());
        Map address = (Map) ((Map) found.get(0)).get("address");
        assertEquals("12", address.get("house_number"));
        assertEquals("First Street", address.get("road"));
        found = (List) json(get(geocoding, "/geo/search?q=testville"));
        assertEquals("town", ((Map) found.get(0)).get("type"));
        found = (List) json(get(geocoding, "/geo/search?q=street&limit=2"));
        assertEquals(2, found.size());
        assertEquals(0, ((List) json(get(geocoding, "/geo/search?q=nowhere"))).size());
    }

    @Test
    void reverseFindsTheNearestAddress() throws Exception {
        Map hit = (Map) json(get(geocoding, "/geo/reverse?lat=10.0001&lon=10.00121"));
        assertEquals("12 First Street, Testville", hit.get("display_name"));
        hit = (Map) json(get(geocoding, "/geo/reverse?lat=10.0015&lon=10.0021"));
        assertEquals("highway", hit.get("class"));
        hit = (Map) json(get(geocoding, "/geo/reverse?lat=11&lon=11"));
        assertEquals("Unable to geocode", hit.get("error"));
    }

    @Test
    void geocoderRefusesBadRequests() throws Exception {
        assertEquals(400, get(geocoding, "/geo/search").getStatus());
        assertEquals(400, get(geocoding, "/geo/search?q=x&format=xml").getStatus());
        assertEquals(400, get(geocoding, "/geo/search?q=x&limit=-1").getStatus());
        assertEquals(400, get(geocoding, "/geo/reverse?lat=95&lon=0").getStatus());
        // East of 90 degrees is a valid longitude (Sydney), and 181 is not.
        assertEquals(200, get(geocoding, "/geo/reverse?lat=-33.86&lon=151.2").getStatus());
        assertEquals(400, get(geocoding, "/geo/reverse?lat=0&lon=181").getStatus());
        assertEquals(400, get(geocoding, "/geo/reverse?lat=1").getStatus());
        assertNull(get(geocoding, "/geo/searchx?q=a"));
    }

    @Test
    void refusesFilesItCannotRead() throws Exception {
        File lz4 = new File(dir, "lz4.osm.pbf");
        OsmFixture.write(lz4.getPath(), true, "lz4");
        IOException err = assertThrows(IOException.class, () -> OsmExtract.load(lz4.getPath()));
        assertTrue(err.getMessage().contains("zlib"), err.getMessage());
        File town = new File(dir, "town.osm.pbf");
        byte[] all = java.nio.file.Files.readAllBytes(town.toPath());
        for (int cut : new int[] {3, 20, all.length / 2, all.length - 1}) {
            File t = new File(dir, "cut" + cut + ".osm.pbf");
            java.nio.file.Files.write(t.toPath(), java.util.Arrays.copyOf(all, cut));
            assertThrows(IOException.class, () -> OsmExtract.load(t.getPath()), "cut at " + cut);
        }
        assertThrows(IOException.class, () -> OsmExtract.load(new File(dir, "missing").getPath()));
        File raw = new File(dir, "raw.osm.pbf");
        OsmFixture.write(raw.getPath(), true, "raw");
        assertEquals(10, OsmExtract.load(raw.getPath()).getRoadVertexCount());
    }

    @Test
    void encodesPolylinesLikeGoogle() {
        List pts = new java.util.ArrayList();
        pts.add(new int[] {385000000, -1202000000});
        pts.add(new int[] {407000000, -1209500000});
        pts.add(new int[] {432520000, -1264530000});
        assertEquals("_p~iF~ps|U_ulLnnqC_mqNvxq`@", Polyline.encode(pts, 5));
        assertEquals("right", Leg.modifier(0, 90));
        assertEquals("sharp left", Leg.modifier(10, 210));
        assertEquals("uturn", Leg.modifier(0, 180));
        assertEquals("slight left", Leg.modifier(350, 310));
    }

    private static List decode(String s) {
        List out = new java.util.ArrayList();
        int i = 0;
        long lat = 0;
        long lon = 0;
        while (i < s.length()) {
            long[] v = new long[2];
            for (int k = 0; k < 2; k++) {
                long result = 0;
                int shift = 0;
                int b;
                do {
                    b = s.charAt(i++) - 63;
                    result |= (long) (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                v[k] = (result & 1) != 0 ? ~(result >> 1) : result >> 1;
            }
            lat += v[0];
            lon += v[1];
            out.add(new double[] {lat / 1e5, lon / 1e5});
        }
        return out;
    }
}
