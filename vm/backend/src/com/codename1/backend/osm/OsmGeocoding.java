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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Answers place searches and reverse lookups in the shape of Nominatim's
/// JSON API, from the places, addresses and named streets of an extract.
///
/// Mounted at a prefix, it answers:
///
/// - `GET {prefix}/search?q=...&limit=10`: a JSON array of the best matches.
///   Every word of the query must start a word of the place's name or
///   address; case and Latin accents are ignored. `limit` is capped at 50.
/// - `GET {prefix}/reverse?lat=...&lon=...`: the nearest address or named
///   place within 75 meters, otherwise the nearest street within a
///   kilometre, as one JSON object; `{"error": "Unable to geocode"}` when
///   there is none.
///
/// `format` may be `json` or `jsonv2` or absent; anything else is a 400. Each
/// result carries `place_id`, `osm_type`, `osm_id`, `lat` and `lon` (as
/// strings, as Nominatim writes them), `display_name`, `name`, `class`,
/// `type`, `importance` and an `address` object.
///
/// This is a geocoder for the extract's own area, not a replacement for a
/// full Nominatim installation: there is no address interpolation, no
/// administrative hierarchy and no spelling correction.
public final class OsmGeocoding implements HttpServer.Handler {
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final int MAX_QUERY = 256;

    private final OsmExtract extract;
    private final String prefix;

    /// - `extract`: the loaded extract
    /// - `prefix`: the URL path the handler is mounted at, "" for the root
    public OsmGeocoding(OsmExtract extract, String prefix) {
        this.extract = extract;
        String p = prefix == null ? "" : prefix;
        this.prefix = p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
    }

    @Override
    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
        String path = request.pathFrom(0);
        boolean search = path.equals(prefix + "/search");
        boolean reverse = path.equals(prefix + "/reverse");
        if (!search && !reverse) {
            return null;
        }
        if (!"GET".equalsIgnoreCase(request.getMethod()) && !"HEAD".equalsIgnoreCase(request.getMethod())) {
            return HttpServer.Response.text(405, "method not allowed");
        }
        String format = request.queryParam("format");
        if (format != null && !"json".equals(format) && !"jsonv2".equals(format)) {
            return error(400, "Only format=json and format=jsonv2 are supported");
        }
        Map headers = new LinkedHashMap();
        headers.put("Access-Control-Allow-Origin", "*");
        if (search) {
            String q = request.queryParam("q");
            if (q == null || q.trim().length() == 0) {
                return error(400, "Missing the q parameter");
            }
            if (q.length() > MAX_QUERY) {
                return error(400, "The query is too long");
            }
            int limit = DEFAULT_LIMIT;
            String l = request.queryParam("limit");
            if (l != null) {
                limit = parseLimit(l);
                if (limit < 0) {
                    return error(400, "limit must be a whole number");
                }
            }
            List found = extract.places.search(q, Math.max(1, Math.min(limit, MAX_LIMIT)));
            List out = new ArrayList();
            for (Object e : found) {
                out.add(toJson((GeocodeIndex.Entry) e));
            }
            return json(Json.write(out), headers);
        }
        int[] point = new int[2];
        if (!parse(request.queryParam("lat"), 900000000L, point, 0)
                || !parse(request.queryParam("lon"), 1800000000L, point, 1)) {
            return error(400, "lat and lon must be decimal degrees");
        }
        GeocodeIndex.Entry e = extract.places.reverse(point[0], point[1]);
        if (e == null) {
            Map none = new LinkedHashMap();
            none.put("error", "Unable to geocode");
            return json(Json.write(none), headers);
        }
        return json(Json.write(toJson(e)), headers);
    }

    private static HttpServer.Response json(String body, Map headers) throws Exception {
        return new HttpServer.Response(200, "application/json; charset=utf-8", body.getBytes("UTF-8"),
                headers);
    }

    private static int parseLimit(String s) {
        if (s.length() == 0 || s.length() > 4) {
            return -1;
        }
        int v = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            v = v * 10 + (c - '0');
        }
        return v;
    }

    private static boolean parse(String s, long bound, int[] out, int at) {
        if (s == null) {
            return false;
        }
        // In the longitude slot, which takes +-180; the bound narrows it.
        int[] parsed = OsmRouting.parseCoordinates(s + ",0");
        if (parsed == null || parsed.length != 2) {
            return false;
        }
        long v = parsed[1];
        if (v < -bound || v > bound) {
            return false;
        }
        out[at] = parsed[1];
        return true;
    }

    private static HttpServer.Response error(int status, String message) {
        Map doc = new LinkedHashMap();
        doc.put("error", message);
        return HttpServer.Response.json(status, Json.write(doc));
    }

    static Map toJson(GeocodeIndex.Entry e) {
        Map m = new LinkedHashMap();
        // Stable for one extract: the OSM id with the element type folded in.
        m.put("place_id", Long.valueOf(e.osmId * 2 + (e.osmType == 'w' ? 1 : 0)));
        m.put("licence", "Data (c) OpenStreetMap contributors, ODbL 1.0. https://www.openstreetmap.org/copyright");
        m.put("osm_type", e.osmType == 'w' ? "way" : "node");
        m.put("osm_id", Long.valueOf(e.osmId));
        m.put("lat", degrees(e.lat));
        m.put("lon", degrees(e.lon));
        m.put("display_name", e.display);
        m.put("name", e.name == null ? "" : e.name);
        m.put("class", e.cls);
        m.put("type", e.type);
        m.put("importance", Double.valueOf(e.importance / 100.0));
        Map address = new LinkedHashMap();
        if (e.houseNumber != null) {
            address.put("house_number", e.houseNumber);
        }
        if (e.street != null) {
            address.put("road", e.street);
        }
        if (e.city != null) {
            address.put("city", e.city);
        }
        if (e.postcode != null) {
            address.put("postcode", e.postcode);
        }
        m.put("address", address);
        return m;
    }

    /// Degrees times 1e7 as a decimal string with seven places.
    static String degrees(int e7) {
        StringBuilder sb = new StringBuilder();
        long v = e7;
        if (v < 0) {
            sb.append('-');
            v = -v;
        }
        sb.append(v / 10000000L).append('.');
        String frac = String.valueOf(v % 10000000L);
        for (int i = frac.length(); i < 7; i++) {
            sb.append('0');
        }
        return sb.append(frac).toString();
    }
}
