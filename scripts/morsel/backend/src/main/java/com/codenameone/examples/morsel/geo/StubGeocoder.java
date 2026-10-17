/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.morsel.geo;

import com.codenameone.examples.morsel.api.PlaceDto;

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
        return new PlaceDto((String) PLACES[index][0], (String) PLACES[index][1],
                ((Double) PLACES[index][2]).doubleValue(),
                ((Double) PLACES[index][3]).doubleValue());
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
