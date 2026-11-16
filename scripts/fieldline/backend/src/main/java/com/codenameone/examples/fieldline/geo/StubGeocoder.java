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
package com.codenameone.examples.fieldline.geo;

import com.codenameone.examples.fieldline.api.PlaceDto;

import java.util.ArrayList;
import java.util.List;

/// A geocoder that knows ten places in Portland, where the demonstration
/// company works, and asks nobody. What the tests run
/// against, so they neither depend on a public service nor lean on one.
public class StubGeocoder implements Geocoder {
    private static final Object[][] PLACES = {
        {"Union Station", "800 NW 6th Ave, Portland", 45.5289, -122.6767},
        {"Pioneer Courthouse Square", "701 SW 6th Ave, Portland", 45.5189, -122.6793},
        {"Powell's City of Books", "1005 W Burnside St, Portland", 45.5231, -122.6814},
        {"Pittock Mansion", "3229 NW Pittock Dr, Portland", 45.5252, -122.7163},
        {"Moda Center", "1 N Center Ct St, Portland", 45.5316, -122.6668},
        {"Oregon Convention Center", "777 NE Martin Luther King Jr Blvd, Portland", 45.5287, -122.6630},
        {"Lloyd Center", "2201 Lloyd Center, Portland", 45.5320, -122.6537},
        {"OMSI", "1945 SE Water Ave, Portland", 45.5084, -122.6655},
        {"Providence Park", "1844 SW Morrison St, Portland", 45.5215, -122.6917},
        {"Portland International Airport", "7000 NE Airport Way, Portland", 45.5887, -122.5975},
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
