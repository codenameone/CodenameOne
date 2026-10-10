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

import com.codename1.backend.Json;
import com.codename1.backend.Web;
import com.codenameone.examples.wayline.api.PlaceDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Geocoding by Photon, the open-source geocoder over OpenStreetMap data.
///
/// The address defaults to the public instance, which needs no key and asks in
/// return for moderate use. Run your own, or point `wayline.geocoder.url` at
/// another Photon, before sending it a real service's traffic.
public class PhotonGeocoder implements Geocoder {
    private final String base;

    public PhotonGeocoder(String base) {
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    @Override
    public List<PlaceDto> search(String text, double lat, double lng) throws IOException {
        String url = base + "/api?limit=8&q=" + UrlText.encode(text);
        if (Geo.isPoint(lat, lng)) {
            url += "&lat=" + lat + "&lon=" + lng;
        }
        return places(url);
    }

    @Override
    public PlaceDto reverse(double lat, double lng) throws IOException {
        List<PlaceDto> found = places(base + "/reverse?lat=" + lat + "&lon=" + lng);
        return found.isEmpty() ? null : found.get(0);
    }

    private static List<PlaceDto> places(String url) throws IOException {
        Web.Result answer = Web.get(url);
        if (!answer.isSuccess()) {
            throw new IOException("The geocoder answered " + answer.getStatus());
        }
        List<PlaceDto> out = new ArrayList<PlaceDto>();
        Object features = Json.parseObject(answer.getBodyAsString()).get("features");
        if (!(features instanceof List)) {
            return out;
        }
        List list = (List) features;
        for (int iter = 0; iter < list.size(); iter++) {
            PlaceDto place = place(list.get(iter));
            if (place != null) {
                out.add(place);
            }
        }
        return out;
    }

    /// One GeoJSON feature as a place, or null when it has no usable point.
    private static PlaceDto place(Object feature) {
        if (!(feature instanceof Map)) {
            return null;
        }
        Object geometry = ((Map) feature).get("geometry");
        Object properties = ((Map) feature).get("properties");
        if (!(geometry instanceof Map) || !(properties instanceof Map)) {
            return null;
        }
        Object coordinates = ((Map) geometry).get("coordinates");
        if (!(coordinates instanceof List) || ((List) coordinates).size() < 2) {
            return null;
        }
        Object lng = ((List) coordinates).get(0);
        Object lat = ((List) coordinates).get(1);
        if (!(lng instanceof Number) || !(lat instanceof Number)) {
            return null;
        }
        Map p = (Map) properties;
        String street = text(p, "street");
        if (street.length() > 0 && text(p, "housenumber").length() > 0) {
            street = text(p, "housenumber") + " " + street;
        }
        String name = text(p, "name");
        if (name.length() == 0) {
            name = street.length() > 0 ? street : text(p, "city");
        }
        if (name.length() == 0) {
            return null;
        }
        StringBuilder address = new StringBuilder();
        append(address, name.equals(street) ? "" : street);
        append(address, text(p, "city"));
        append(address, text(p, "country"));
        PlaceDto place = new PlaceDto();
        place.name = name;
        place.address = address.toString();
        place.lat = ((Number) lat).doubleValue();
        place.lng = ((Number) lng).doubleValue();
        return place;
    }

    private static String text(Map properties, String key) {
        Object value = properties.get(key);
        return value instanceof String ? (String) value : "";
    }

    private static void append(StringBuilder to, String part) {
        if (part.length() > 0) {
            if (to.length() > 0) {
                to.append(", ");
            }
            to.append(part);
        }
    }
}
