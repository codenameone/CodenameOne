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

import com.codename1.backend.Json;
import com.codename1.backend.Web;
import com.codenameone.examples.morsel.api.PlaceDto;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Geocoding by Photon, the open-source geocoder over OpenStreetMap data.
///
/// The address defaults to the public instance, which needs no key and asks in
/// return for moderate use. Run your own, or point `morsel.geocoder.url` at
/// another Photon, before sending it a real service's traffic.
public class PhotonGeocoder implements Geocoder {
    private final String base;

    public PhotonGeocoder(String base) {
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    @Override
    public List<PlaceDto> search(String text, double lat, double lng) throws IOException {
        String url = base + "/api?limit=8&q=" + URLEncoder.encode(text, "UTF-8");
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
        return new PlaceDto(name, address.toString(), ((Number) lat).doubleValue(),
                ((Number) lng).doubleValue());
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
