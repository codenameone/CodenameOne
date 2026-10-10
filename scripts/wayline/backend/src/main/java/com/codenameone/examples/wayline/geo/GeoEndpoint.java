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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.wayline.api.GeoApiServer;
import com.codenameone.examples.wayline.api.PlaceDto;

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
        if (found == null) {
            // A pin can be dropped anywhere, and a ride can start there: an
            // address the service does not know is a point without a name.
            found = new PlaceDto();
            found.name = "Dropped pin";
            found.address = "";
        }
        // The point asked about, not the centre of whatever was found near it.
        found.lat = lat;
        found.lng = lng;
        return found;
    }
}
