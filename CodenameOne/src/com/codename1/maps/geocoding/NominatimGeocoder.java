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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import com.codename1.maps.LatLng;
import com.codename1.ui.CN;
import com.codename1.ui.events.ActionListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// A geocoder speaking Nominatim's JSON API: `/search?q=` for a forward
/// search and `/reverse?lat=&lon=` for the address at a point.
///
/// Point it at your own server. A Codename One backend answers this API from
/// an OpenStreetMap extract with `com.codename1.backend.osm.OsmGeocoding`:
///
/// ```java
/// NominatimGeocoder geocoder = new NominatimGeocoder("https://api.example.com/geocode");
/// geocoder.search("main street 12", 5, new GeocodeCallback() {
///     public void placesFound(List places) {
///         Place best = (Place) places.get(0);
///         Log.p(best.getDisplayName() + " at " + best.getLocation());
///     }
///     public void geocodeFailed(String message, Throwable error) {
///         Log.p(message);
///     }
/// });
/// ```
///
/// **Before you ship**: the default endpoint is the OpenStreetMap
/// Foundation's public Nominatim server, which its usage policy limits to one
/// request per second per application, forbids for search-as-you-type, and
/// may block without notice (https://operations.osmfoundation.org/policies/nominatim/).
/// It is there for development; an application with real users needs its
/// own server.
///
/// Every result is OpenStreetMap data and must be credited to
/// "OpenStreetMap contributors" where the user sees it.
public class NominatimGeocoder {

    /// The OpenStreetMap Foundation's public Nominatim server, used when no
    /// other base URL is configured. For development only -- see the class
    /// documentation.
    public static final String PUBLIC_BASE_URL = "https://nominatim.openstreetmap.org";

    // The OSMF services ask clients to identify themselves; the platform
    // default agent on Android is also blocked by some of them (see
    // OsrmRouteService.USER_AGENT).
    static final String USER_AGENT = "CodenameOne (+https://www.codenameone.com)";

    private static final int MAX_LIMIT = 50;

    private String baseUrl = PUBLIC_BASE_URL;

    /// Creates a geocoder pointing at the public Nominatim server.
    public NominatimGeocoder() {
    }

    /// Creates a geocoder pointing at a Nominatim-compatible endpoint.
    ///
    /// #### Parameters
    ///
    /// - `baseUrl`: for example `https://api.example.com/geocode`
    public NominatimGeocoder(String baseUrl) {
        setBaseUrl(baseUrl);
    }

    /// The endpoint requests go to, without a trailing slash.
    public String getBaseUrl() {
        return baseUrl;
    }

    /// Points this geocoder at a Nominatim-compatible endpoint. A `null` or
    /// empty value restores [#PUBLIC_BASE_URL]; a trailing slash is trimmed.
    ///
    /// #### Parameters
    ///
    /// - `baseUrl`: the endpoint
    ///
    /// #### Returns
    ///
    /// this geocoder
    public NominatimGeocoder setBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.length() == 0) {
            this.baseUrl = PUBLIC_BASE_URL;
            return this;
        }
        while (baseUrl.length() > 1 && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.baseUrl = baseUrl;
        return this;
    }

    /// Finds places matching a name or an address, best first.
    ///
    /// #### Parameters
    ///
    /// - `query`: free text, a place name or an address
    /// - `limit`: the most results wanted, 1 to 50
    /// - `callback`: receives the outcome on the event dispatch thread
    public void search(String query, int limit, GeocodeCallback callback) {
        requireCallback(callback);
        if (query == null || query.trim().length() == 0) {
            fail(callback, "A search needs a query", null);
            return;
        }
        newConnection(buildSearchUrl(query, limit), false, callback).start();
    }

    /// Finds the address or place at a point. The callback receives a list of
    /// at most one place, empty when the service knows nothing near it.
    ///
    /// #### Parameters
    ///
    /// - `location`: the point
    /// - `callback`: receives the outcome on the event dispatch thread
    public void reverse(LatLng location, GeocodeCallback callback) {
        requireCallback(callback);
        if (location == null || !isReal(location.getLatitude()) || !isReal(location.getLongitude())) {
            fail(callback, "A reverse lookup needs a usable coordinate", null);
            return;
        }
        newConnection(buildReverseUrl(location), true, callback).start();
    }

    private static void requireCallback(GeocodeCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback is required: geocoding is asynchronous, "
                    + "so there is no other way to report the result");
        }
    }

    private static boolean isReal(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    /// The URL a [#search] call requests. Package visible for tests.
    String buildSearchUrl(String query, int limit) {
        int l = Math.max(1, Math.min(limit, MAX_LIMIT));
        return baseUrl + "/search?format=jsonv2&addressdetails=1&limit=" + l
                + "&q=" + Util.encodeUrl(query.trim());
    }

    /// The URL a [#reverse] call requests. Package visible for tests.
    String buildReverseUrl(LatLng location) {
        StringBuilder sb = new StringBuilder(baseUrl);
        sb.append("/reverse?format=jsonv2&addressdetails=1&lat=");
        appendFixed(sb, location.getLatitude());
        sb.append("&lon=");
        appendFixed(sb, location.getLongitude());
        return sb.toString();
    }

    // Seven decimal places without going through a locale-dependent formatter.
    private static void appendFixed(StringBuilder sb, double value) {
        long scaled = Math.round(value * 10000000.0);
        if (scaled < 0) {
            sb.append('-');
            scaled = -scaled;
        }
        sb.append(scaled / 10000000L).append('.');
        String frac = String.valueOf(scaled % 10000000L);
        for (int i = frac.length(); i < 7; i++) {
            sb.append('0');
        }
        sb.append(frac);
    }

    GeocodeConnection newConnection(String url, boolean reverse, GeocodeCallback callback) {
        GeocodeConnection req = new GeocodeConnection(callback, reverse);
        req.setUrl(url);
        req.setPost(false);
        return req;
    }

    /// Parses a Nominatim `/search` response, an array of places.
    ///
    /// #### Parameters
    ///
    /// - `json`: the response body
    ///
    /// #### Returns
    ///
    /// the [Place] results in the order given, possibly empty
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the body is not a Nominatim result array
    public static List parseSearch(String json) throws IOException {
        Map root = parse(json);
        Object list = root.get("root");
        if (!(list instanceof List)) {
            if (root.isEmpty()) {
                return new ArrayList();
            }
            if (root.get("error") != null) {
                throw new IOException("The geocoder answered: " + root.get("error"));
            }
            throw new IOException("The geocoding response is not a list of places");
        }
        List out = new ArrayList();
        for (Object o : (List) list) {
            if (!(o instanceof Map)) {
                throw new IOException("The geocoding response holds a place that is not an object");
            }
            Place p = toPlace((Map) o);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    /// Parses a Nominatim `/reverse` response, one place or an error object.
    ///
    /// #### Parameters
    ///
    /// - `json`: the response body
    ///
    /// #### Returns
    ///
    /// a list holding the place, or empty when the service found none
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the body is not a Nominatim result
    public static List parseReverse(String json) throws IOException {
        Map root = parse(json);
        List out = new ArrayList();
        if (root.get("error") != null || root.isEmpty()) {
            // Nominatim's answer for a point with nothing near it, as an
            // error object or (from some servers) an empty array.
            return out;
        }
        Place p = toPlace(root);
        if (p == null) {
            throw new IOException("The geocoding response is not a place");
        }
        out.add(p);
        return out;
    }

    private static Map parse(String json) throws IOException {
        if (json == null) {
            throw new IOException("The geocoding response is empty");
        }
        String trimmed = json.trim();
        if ("[]".equals(trimmed)) {
            return new java.util.HashMap();
        }
        Map root = JSONParser.parseJSON(trimmed);
        if (root == null) {
            throw new IOException("The geocoding response could not be parsed");
        }
        return root;
    }

    // null when the object carries no usable coordinates.
    private static Place toPlace(Map m) {
        double lat = coordinate(m.get("lat"));
        double lon = coordinate(m.get("lon"));
        if (!isReal(lat) || !isReal(lon) || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            return null;
        }
        Object a = m.get("address");
        Map address = a instanceof Map ? (Map) a : new java.util.HashMap();
        String city = string(address.get("city"));
        if (city.length() == 0) {
            city = string(address.get("town"));
        }
        if (city.length() == 0) {
            city = string(address.get("village"));
        }
        String category = string(m.get("category"));
        if (category.length() == 0) {
            category = string(m.get("class"));
        }
        return new Place(string(m.get("name")), string(m.get("display_name")), new LatLng(lat, lon),
                category, string(m.get("type")), string(address.get("house_number")),
                string(address.get("road")), city, string(address.get("postcode")));
    }

    // Nominatim writes coordinates as strings; accept numbers too.
    private static double coordinate(Object v) {
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        if (v instanceof String) {
            try {
                return Double.parseDouble((String) v);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
        return Double.NaN;
    }

    private static String string(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static String describe(Throwable error, String fallback) {
        String message = error == null ? null : error.getMessage();
        return message == null || message.length() == 0 ? fallback : message;
    }

    private static void fail(final GeocodeCallback callback, final String message, final Throwable error) {
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                callback.geocodeFailed(message, error);
            }
        });
    }

    /// The network request. Delivers exactly one outcome, following the same
    /// rules as the routing client's connection (`OsrmRouteService`): not
    /// `failSilently`, every failure hook overridden, a completion backstop
    /// that counts redirect attempts, and the claim taken before any hop to
    /// the event dispatch thread.
    static final class GeocodeConnection extends ConnectionRequest
            implements ActionListener<NetworkEvent> {
        private final GeocodeCallback callback;
        private final boolean reverse;
        private boolean delivered;
        private List places;
        private String errorMessage;
        private Throwable error;
        private int outstandingAttempts = 1;

        GeocodeConnection(GeocodeCallback callback, boolean reverse) {
            this.callback = callback;
            this.reverse = reverse;
            setUserAgent(USER_AGENT);
            setReadResponseForErrors(false);
        }

        void start() {
            NetworkManager nm = NetworkManager.getInstance();
            nm.addProgressListener(this);
            try {
                nm.addToQueue(this);
            } catch (RuntimeException e) {
                detach();
                failLater("The geocoding request could not be sent: "
                        + describe(e, "the endpoint was rejected"), e);
            }
        }

        @Override
        public void retry() {
            synchronized (this) {
                outstandingAttempts++;
            }
            super.retry();
        }

        @Override
        public void actionPerformed(NetworkEvent n) {
            if (n.getConnectionRequest() != this) { //NOPMD CompareObjectsWithEquals
                return;
            }
            if (n.getProgressType() != NetworkEvent.PROGRESS_TYPE_COMPLETED) {
                return;
            }
            boolean finished;
            synchronized (this) {
                outstandingAttempts--;
                finished = outstandingAttempts <= 0;
            }
            if (!finished) {
                return;
            }
            detach();
            failLater("The geocoding request ended without a result", null);
        }

        @Override
        protected void readResponse(InputStream input) throws IOException {
            byte[] data = Util.readInputStream(input);
            try {
                String body = new String(data, "UTF-8");
                places = reverse ? parseReverse(body) : parseSearch(body);
            } catch (IOException e) {
                fail(e);
            } catch (RuntimeException e) {
                fail(e);
            }
        }

        private void fail(Exception e) {
            places = null;
            error = e;
            errorMessage = describe(e, "The geocoding response could not be read");
        }

        @Override
        protected void postResponse() {
            if (places != null) {
                if (!claim()) {
                    return;
                }
                detach();
                callback.placesFound(places);
            } else {
                if (!claim()) {
                    return;
                }
                detach();
                callback.geocodeFailed(errorMessage, error);
            }
        }

        @Override
        protected void handleException(Exception err) {
            failLater("The geocoding request failed: "
                    + describe(err, "the network could not be reached"), err);
        }

        @Override
        protected void handleErrorResponseCode(int code, String message) {
            failLater("The geocoding service returned HTTP " + code
                    + (message == null || message.length() == 0 ? "" : " (" + message + ")"), null);
        }

        private void detach() {
            NetworkManager.getInstance().removeProgressListener(this);
        }

        private synchronized boolean claim() {
            if (delivered) {
                return false;
            }
            delivered = true;
            return true;
        }

        private void failLater(final String message, final Throwable err) {
            if (!claim()) {
                return;
            }
            detach();
            CN.callSerially(new Runnable() {
                @Override
                public void run() {
                    callback.geocodeFailed(message, err);
                }
            });
        }
    }
}
