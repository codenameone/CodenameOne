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
package com.codename1.maps.routing;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import com.codename1.maps.LatLng;
import com.codename1.maps.PolylineCodec;
import com.codename1.ui.CN;
import com.codename1.ui.events.ActionListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The keyless [RouteService] Codename One uses by default: routing over the
/// OpenStreetMap road network through the
/// [OSRM](https://project-osrm.org) HTTP API.
///
/// Like the OpenFreeMap basemap behind [com.codename1.maps.MapView], it needs
/// no API key and no signup, so a road-following route works out of the box in
/// the simulator and on device.
///
/// **Before you ship**: the default endpoint is OSRM's *public demo server*.
/// It is provided for development and demos, has no SLA, is rate limited and
/// may refuse long routes. For production point this service at your own OSRM
/// instance (or any OSRM-compatible endpoint) and nothing else in your code
/// changes:
///
/// ```java
/// Routing.setService(new OsrmRouteService("https://osrm.example.com"));
/// ```
///
/// Because the demo server hosts the car profile, [TravelMode#WALKING] and
/// [TravelMode#CYCLING] are accepted but routed over driving data there. A
/// self-hosted instance with the matching profiles honors them properly.
public class OsrmRouteService implements RouteService {

    /// The OSRM public demo server, used when no other base URL is configured.
    /// Suitable for development and demos only -- see the class documentation.
    public static final String DEMO_BASE_URL = "https://router.project-osrm.org";

    private String baseUrl = DEMO_BASE_URL;

    /// Creates a service pointing at the OSRM public demo server.
    public OsrmRouteService() {
    }

    /// Creates a service pointing at an OSRM-compatible endpoint, for example
    /// `https://osrm.example.com`.
    public OsrmRouteService(String baseUrl) {
        setBaseUrl(baseUrl);
    }

    /// The endpoint routes are requested from, without a trailing slash.
    public String getBaseUrl() {
        return baseUrl;
    }

    /// Points this service at an OSRM-compatible endpoint. A `null` or empty
    /// value restores [#DEMO_BASE_URL]; a trailing slash is trimmed.
    public OsrmRouteService setBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.length() == 0) {
            this.baseUrl = DEMO_BASE_URL;
            return this;
        }
        while (baseUrl.length() > 1 && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.baseUrl = baseUrl;
        return this;
    }

    /// {@inheritDoc}
    @Override
    public String getId() {
        return "osrm";
    }

    /// {@inheritDoc}
    ///
    /// Always true -- OSRM needs no credentials.
    @Override
    public boolean isAvailable() {
        return true;
    }

    /// {@inheritDoc}
    @Override
    public void findRoutes(RouteRequest request, final RouteCallback callback) {
        if (callback == null) {
            return;
        }
        if (request == null || request.getOrigin() == null || request.getDestination() == null) {
            fail(callback, "A route request needs both an origin and a destination", null);
            return;
        }
        RouteConnection req = new RouteConnection(callback);
        req.setUrl(buildUrl(request));
        req.setPost(false);
        req.start();
    }

    /// Builds the OSRM request URL for `request`. Package visible so the shape
    /// of the query can be asserted without hitting the network.
    String buildUrl(RouteRequest request) {
        StringBuilder sb = new StringBuilder(baseUrl);
        sb.append("/route/v1/").append(request.getTravelMode().getId()).append('/');
        appendCoordinate(sb, request.getOrigin());
        for (Object waypoint : request.getWaypoints()) {
            sb.append(';');
            appendCoordinate(sb, (LatLng) waypoint);
        }
        sb.append(';');
        appendCoordinate(sb, request.getDestination());
        sb.append("?overview=full&geometries=polyline");
        sb.append("&steps=").append(request.isSteps() ? "true" : "false");
        sb.append("&alternatives=").append(request.isAlternatives() ? "true" : "false");
        return sb.toString();
    }

    /// Parses an OSRM `route` service response into [Route] objects, best
    /// first.
    ///
    /// Exposed so an app that fetches from an OSRM-compatible endpoint through
    /// its own transport (a proxy, a cached response, a bundled fixture) can
    /// reuse the same parsing.
    ///
    /// #### Parameters
    ///
    /// - `json`: the raw response body
    ///
    /// #### Returns
    ///
    /// the routes found, never empty
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the body is malformed, or when OSRM reported that
    ///   it could not route the request
    public static List parseResponse(String json) throws IOException {
        if (json == null || json.length() == 0) {
            throw new IOException("Empty routing response");
        }
        Map root = JSONParser.parseJSON(json);
        String code = string(root.get("code"));
        if (code.length() > 0 && !"Ok".equals(code)) {
            throw new IOException(describeErrorCode(code, string(root.get("message"))));
        }
        Object routesObj = root.get("routes");
        if (!(routesObj instanceof List) || ((List) routesObj).isEmpty()) {
            throw new IOException("The routing service returned no route");
        }
        List routes = new ArrayList();
        for (Object routeObj : (List) routesObj) {
            routes.add(parseRoute((Map) routeObj));
        }
        return routes;
    }

    private static Route parseRoute(Map json) {
        List points = PolylineCodec.decode(string(json.get("geometry")));
        List legs = new ArrayList();
        String summary = "";
        Object legsObj = json.get("legs");
        if (legsObj instanceof List) {
            for (Object legObj : (List) legsObj) {
                Map legJson = (Map) legObj;
                RouteLeg leg = parseLeg(legJson);
                legs.add(leg);
                if (summary.length() == 0) {
                    summary = leg.getSummary();
                }
            }
        }
        return new Route(points, legs, number(json.get("distance")),
                number(json.get("duration")), summary);
    }

    private static RouteLeg parseLeg(Map json) {
        List steps = new ArrayList();
        Object stepsObj = json.get("steps");
        if (stepsObj instanceof List) {
            for (Object stepObj : (List) stepsObj) {
                steps.add(parseStep((Map) stepObj));
            }
        }
        return new RouteLeg(string(json.get("summary")), number(json.get("distance")),
                number(json.get("duration")), steps);
    }

    private static RouteStep parseStep(Map json) {
        String roadName = string(json.get("name"));
        String type = "";
        String modifier = "";
        LatLng start = null;
        Object maneuverObj = json.get("maneuver");
        if (maneuverObj instanceof Map) {
            Map maneuver = (Map) maneuverObj;
            type = string(maneuver.get("type"));
            modifier = string(maneuver.get("modifier"));
            start = parseLocation(maneuver.get("location"));
        }
        return new RouteStep(describeManeuver(type, modifier, roadName), roadName,
                number(json.get("distance")), number(json.get("duration")), start,
                PolylineCodec.decode(string(json.get("geometry"))));
    }

    /// OSRM encodes locations as a `[longitude, latitude]` pair -- note the
    /// order, which is the reverse of [LatLng].
    private static LatLng parseLocation(Object location) {
        if (!(location instanceof List) || ((List) location).size() < 2) {
            return null;
        }
        List pair = (List) location;
        return new LatLng(number(pair.get(1)), number(pair.get(0)));
    }

    /// Turns an OSRM maneuver into a readable instruction. OSRM itself returns
    /// only the structured maneuver, leaving the phrasing to the client.
    private static String describeManeuver(String type, String modifier, String roadName) {
        String onto = roadName.length() > 0 ? " onto " + roadName : "";
        if ("depart".equals(type)) {
            return roadName.length() > 0 ? "Head out on " + roadName : "Start";
        }
        if ("arrive".equals(type)) {
            return "Arrive at your destination";
        }
        if ("turn".equals(type) || "end of road".equals(type)) {
            return turnPhrase(modifier) + onto;
        }
        if ("continue".equals(type) || "new name".equals(type)) {
            return "Continue" + onto;
        }
        if ("merge".equals(type)) {
            return "Merge" + onto;
        }
        if ("on ramp".equals(type)) {
            return "Take the ramp" + onto;
        }
        if ("off ramp".equals(type)) {
            return "Take the exit" + onto;
        }
        if ("fork".equals(type)) {
            return "Keep " + sideOf(modifier) + onto;
        }
        if ("roundabout".equals(type) || "rotary".equals(type)) {
            return "Enter the roundabout and exit" + onto;
        }
        if (type.length() == 0) {
            return "Continue" + onto;
        }
        return capitalize(type) + onto;
    }

    private static String turnPhrase(String modifier) {
        if ("uturn".equals(modifier)) {
            return "Make a U-turn";
        }
        if ("straight".equals(modifier) || modifier.length() == 0) {
            return "Continue straight";
        }
        return "Turn " + modifier;
    }

    private static String sideOf(String modifier) {
        if (modifier.indexOf("right") >= 0) {
            return "right";
        }
        if (modifier.indexOf("left") >= 0) {
            return "left";
        }
        return "going";
    }

    private static String capitalize(String s) {
        if (s.length() == 0) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private static String describeErrorCode(String code, String message) {
        if (message.length() > 0) {
            return message;
        }
        if ("NoRoute".equals(code)) {
            return "No route connects those points for the selected travel mode";
        }
        if ("NoSegment".equals(code)) {
            return "One of the points is too far from any road";
        }
        if ("TooBig".equals(code)) {
            return "The routing request is too large for the service";
        }
        return "The routing service reported: " + code;
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static double number(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException err) {
            // A non-numeric distance/duration is treated as unknown.
            return 0;
        }
    }

    /// OSRM takes `longitude,latitude` in the path. Formatted by hand to six
    /// decimals (about 10cm) so no locale or scientific notation can leak into
    /// the URL.
    private static void appendCoordinate(StringBuilder sb, LatLng coord) {
        appendFixed(sb, coord.getLongitude());
        sb.append(',');
        appendFixed(sb, coord.getLatitude());
    }

    private static void appendFixed(StringBuilder sb, double value) {
        long scaled = Math.round(Math.abs(value) * 1000000.0);
        if (value < 0 && scaled != 0) {
            sb.append('-');
        }
        sb.append(scaled / 1000000L).append('.');
        String fraction = Long.toString(scaled % 1000000L);
        for (int i = fraction.length(); i < 6; i++) {
            sb.append('0');
        }
        sb.append(fraction);
    }

    private static void fail(final RouteCallback callback, final String message,
                             final Throwable error) {
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                callback.routeFailed(message, error);
            }
        });
    }

    /// The network request, kept as a named class so the single-delivery
    /// guarantee of [RouteService#findRoutes] is enforced in one place.
    ///
    /// Note that the request is deliberately *not* marked `failSilently`:
    /// [com.codename1.io.NetworkManager] swallows transport exceptions
    /// outright for silent requests, so [#handleException] would never run and
    /// the caller would wait forever for a callback that never came. Since
    /// every failure hook here is overridden, nothing reaches the default
    /// retry dialog either way.
    private static final class RouteConnection extends ConnectionRequest
            implements ActionListener<NetworkEvent> {

        private final RouteCallback callback;
        private boolean delivered;
        private List routes;
        private String errorMessage;
        private Throwable error;

        RouteConnection(RouteCallback callback) {
            this.callback = callback;
        }

        /// Queues the request, listening for its completion so the callback is
        /// delivered even along paths that reach neither [#postResponse] nor a
        /// failure hook -- a killed request, or an app-wide error listener that
        /// consumes the event before it reaches this request.
        void start() {
            NetworkManager nm = NetworkManager.getInstance();
            nm.addProgressListener(this);
            nm.addToQueue(this);
        }

        /// The completion backstop. Progress events are dispatched to the EDT
        /// through the same serial queue as [#postResponse], and this hops once
        /// more, so any real result already queued is delivered first and wins.
        @Override
        public void actionPerformed(NetworkEvent n) {
            if (n.getConnectionRequest() != this
                    || n.getProgressType() != NetworkEvent.PROGRESS_TYPE_COMPLETED) {
                return;
            }
            NetworkManager.getInstance().removeProgressListener(this);
            failLater("The routing request did not complete", null);
        }

        @Override
        protected void readResponse(InputStream input) throws IOException {
            byte[] data = Util.readInputStream(input);
            try {
                routes = parseResponse(new String(data, "UTF-8"));
            } catch (Throwable t) {
                routes = null;
                // An IOException from parseResponse is our own "the service
                // answered but declined to route" signal, whose message is the
                // whole story; anything else is a genuine failure worth
                // handing to the caller.
                error = t instanceof IOException ? null : t;
                errorMessage = t.getMessage() == null
                        ? "The routing response could not be read" : t.getMessage();
            }
        }

        @Override
        protected void postResponse() {
            if (routes != null) {
                deliverRoutes();
            } else {
                deliverFailure(errorMessage, error);
            }
        }

        @Override
        protected void handleException(Exception err) {
            failLater("The routing request failed: " + err, err);
        }

        @Override
        protected void handleErrorResponseCode(int code, String message) {
            failLater("The routing service returned HTTP " + code
                    + (message == null || message.length() == 0 ? "" : " (" + message + ")"), null);
        }

        private void deliverRoutes() {
            if (delivered) {
                return;
            }
            delivered = true;
            callback.routesFound(routes);
        }

        private void deliverFailure(String message, Throwable err) {
            if (delivered) {
                return;
            }
            delivered = true;
            callback.routeFailed(message, err);
        }

        /// Both network failure hooks run on the network thread, so the
        /// callback contract (always the EDT) is honored by hopping over.
        private void failLater(final String message, final Throwable err) {
            CN.callSerially(new Runnable() {
                @Override
                public void run() {
                    deliverFailure(message, err);
                }
            });
        }
    }
}
