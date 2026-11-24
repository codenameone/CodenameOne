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
package com.codename1.maps.geocoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.maps.LatLng;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class NominatimGeocoderTest {

    // Shaped exactly as the backend's OsmGeocoding writes it, and as
    // nominatim.openstreetmap.org does for format=jsonv2 (category, not class).
    private static final String SEARCH = "[{\"place_id\":61,\"osm_type\":\"node\",\"osm_id\":30,"
            + "\"lat\":\"10.0011000\",\"lon\":\"10.0011000\",\"display_name\":\"Cafe Central\","
            + "\"name\":\"Cafe Central\",\"class\":\"amenity\",\"type\":\"cafe\",\"importance\":0.4,"
            + "\"address\":{}},"
            + "{\"place_id\":62,\"lat\":\"10.0001\",\"lon\":\"10.0012\",\"display_name\":\"12 First Street, Testville\","
            + "\"name\":\"\",\"category\":\"place\",\"type\":\"house\","
            + "\"address\":{\"house_number\":\"12\",\"road\":\"First Street\",\"town\":\"Testville\",\"postcode\":\"98000\"}}]";

    @Test
    void parsesSearchResults() throws IOException {
        List places = NominatimGeocoder.parseSearch(SEARCH);
        assertEquals(2, places.size());
        Place cafe = (Place) places.get(0);
        assertEquals("Cafe Central", cafe.getName());
        assertEquals("amenity", cafe.getCategory());
        assertEquals("cafe", cafe.getType());
        assertEquals(10.0011, cafe.getLocation().getLatitude(), 1e-9);
        Place house = (Place) places.get(1);
        assertEquals("place", house.getCategory());
        assertEquals("12", house.getHouseNumber());
        assertEquals("First Street", house.getRoad());
        assertEquals("Testville", house.getCity());
        assertEquals("98000", house.getPostcode());
        assertEquals("12 First Street, Testville", house.toString());
    }

    @Test
    void emptyAndErrorAnswers() throws IOException {
        assertTrue(NominatimGeocoder.parseSearch("[]").isEmpty());
        assertTrue(NominatimGeocoder.parseReverse("{\"error\":\"Unable to geocode\"}").isEmpty());
        assertThrows(IOException.class, () -> NominatimGeocoder.parseSearch("{\"error\":\"bad\"}"));
        assertThrows(IOException.class, () -> NominatimGeocoder.parseSearch("[1,2]"));
        assertThrows(IOException.class, () -> NominatimGeocoder.parseReverse("{\"lat\":\"x\"}"));
        assertThrows(IOException.class, () -> NominatimGeocoder.parseSearch(null));
    }

    @Test
    void dropsResultsWithoutUsableCoordinates() throws IOException {
        List places = NominatimGeocoder.parseSearch(
                "[{\"lat\":\"95\",\"lon\":\"1\"},{\"lat\":1.5,\"lon\":2.5,\"display_name\":\"ok\"}]");
        assertEquals(1, places.size());
        assertEquals(2.5, ((Place) places.get(0)).getLocation().getLongitude(), 1e-9);
    }

    @Test
    void parsesReverse() throws IOException {
        List places = NominatimGeocoder.parseReverse("{\"lat\":\"-33.8567844\",\"lon\":\"151.2152967\","
                + "\"display_name\":\"Sydney Opera House\",\"address\":{\"city\":\"Sydney\"}}");
        assertEquals(1, places.size());
        Place p = (Place) places.get(0);
        assertEquals(-33.8567844, p.getLocation().getLatitude(), 1e-9);
        assertEquals("Sydney", p.getCity());
        assertEquals("", p.getHouseNumber());
    }

    @Test
    void buildsRequestUrls() {
        NominatimGeocoder g = new NominatimGeocoder("https://api.example.com/geocode/");
        assertEquals("https://api.example.com/geocode", g.getBaseUrl());
        String search = g.buildSearchUrl(" 12 main st ", 500);
        assertTrue(search.startsWith("https://api.example.com/geocode/search?format=jsonv2&addressdetails=1&limit=50&q="),
                search);
        assertTrue(search.endsWith("12%20main%20st") || search.endsWith("12+main+st"), search);
        assertEquals("https://api.example.com/geocode/reverse?format=jsonv2&addressdetails=1"
                + "&lat=-33.8567844&lon=151.2152967",
                g.buildReverseUrl(new LatLng(-33.8567844, 151.2152967)));
        assertEquals(NominatimGeocoder.PUBLIC_BASE_URL, g.setBaseUrl(null).getBaseUrl());
        assertThrows(IllegalArgumentException.class, () -> g.search("x", 1, null));
    }
}
