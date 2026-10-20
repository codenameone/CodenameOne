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
package com.codenameone.examples.savor.geo;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.savor.api.GeoApiServer;
import com.codenameone.examples.savor.api.PlaceDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The server's half of `GeoApi`.
@Component
public class GeoEndpoint implements GeoApiServer {
    private final Geocoder geocoder;

    public GeoEndpoint(Geocoder geocoder) {
        this.geocoder = geocoder;
    }

    @Override
    public List<PlaceDto> search(String q, double lat, double lng) throws Exception {
        String text = q == null ? "" : q.trim();
        // Two letters match half the map; nothing useful comes back, and each
        // keystroke would be a request to the service behind this.
        if (text.length() < 3 || text.length() > 120) {
            return new ArrayList<PlaceDto>();
        }
        try {
            return geocoder.search(text, lat, lng);
        } catch (IOException failed) {
            System.err.println("geocoder search failed: " + failed.getMessage());
            throw new ResponseStatusException(502, "Search is unavailable right now");
        }
    }

    @Override
    public PlaceDto reverse(double lat, double lng) throws Exception {
        if (!Geo.isPoint(lat, lng)) {
            throw new ResponseStatusException(400, "That is not a point on the map");
        }
        PlaceDto found;
        try {
            found = geocoder.reverse(lat, lng);
        } catch (IOException failed) {
            System.err.println("geocoder reverse failed: " + failed.getMessage());
            found = null;
        }
        // The point asked about, not the centre of whatever was found near it.
        if (found == null) {
            // A pin can be dropped anywhere, and a delivery can start there: an
            // address the service does not know is a point without a name.
            return new PlaceDto("Dropped pin", "", lat, lng);
        }
        return new PlaceDto(found.name(), found.address(), lat, lng);
    }
}
