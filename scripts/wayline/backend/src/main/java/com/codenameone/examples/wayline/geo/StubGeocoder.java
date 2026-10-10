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
package com.codenameone.examples.wayline.geo;

import com.codenameone.examples.wayline.api.PlaceDto;

import java.util.ArrayList;
import java.util.List;

/// A geocoder that knows ten places and asks nobody. What the tests run
/// against, so they neither depend on a public service nor lean on one.
public class StubGeocoder implements Geocoder {
    private static final Object[][] PLACES = {
        {"Ferry Building", "1 Ferry Building, San Francisco", 37.7955, -122.3937},
        {"Pier 39", "The Embarcadero, San Francisco", 37.8087, -122.4098},
        {"Coit Tower", "1 Telegraph Hill Blvd, San Francisco", 37.8024, -122.4058},
        {"Ghirardelli Square", "900 North Point St, San Francisco", 37.8059, -122.4229},
        {"Lombard Street", "Lombard St, San Francisco", 37.8021, -122.4187},
        {"Palace of Fine Arts", "3601 Lyon St, San Francisco", 37.8029, -122.4484},
        {"Union Square", "333 Post St, San Francisco", 37.7880, -122.4075},
        {"Oracle Park", "24 Willie Mays Plaza, San Francisco", 37.7786, -122.3893},
        {"Fort Mason", "2 Marina Blvd, San Francisco", 37.8066, -122.4310},
        {"Transamerica Pyramid", "600 Montgomery St, San Francisco", 37.7952, -122.4028},
    };

    @Override
    public List<PlaceDto> search(String text, double lat, double lng) {
        List<PlaceDto> out = new ArrayList<PlaceDto>();
        String wanted = text == null ? "" : text.trim();
        for (int iter = 0; iter < PLACES.length; iter++) {
            if (contains((String) PLACES[iter][0], wanted)) {
                out.add(place(iter));
            }
        }
        return out;
    }

    @Override
    public PlaceDto reverse(double lat, double lng) {
        int nearest = 0;
        double best = Double.MAX_VALUE;
        for (int iter = 0; iter < PLACES.length; iter++) {
            double meters = Geo.meters(lat, lng, ((Double) PLACES[iter][2]).doubleValue(),
                    ((Double) PLACES[iter][3]).doubleValue());
            if (meters < best) {
                best = meters;
                nearest = iter;
            }
        }
        return place(nearest);
    }

    private static PlaceDto place(int index) {
        PlaceDto place = new PlaceDto();
        place.name = (String) PLACES[index][0];
        place.address = (String) PLACES[index][1];
        place.lat = ((Double) PLACES[index][2]).doubleValue();
        place.lng = ((Double) PLACES[index][3]).doubleValue();
        return place;
    }

    /// Whether `text` contains `part`, whatever the case of either. Compared a
    /// character at a time and not by folding both, which would follow the
    /// server's locale.
    private static boolean contains(String text, String part) {
        for (int at = 0; at + part.length() <= text.length(); at++) {
            if (text.regionMatches(true, at, part, 0, part.length())) {
                return true;
            }
        }
        return false;
    }
}
