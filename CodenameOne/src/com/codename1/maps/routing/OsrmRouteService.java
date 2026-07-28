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
/// **On travel modes**: the request URL carries the mode as OSRM's profile
/// path component, which OSRM-compatible hosted services (Mapbox Directions
/// and friends) use to pick a profile. A stock `osrm-routed` process does
/// not: it serves the single dataset it was prepared and started with and
/// ignores that part of the path. So one `OsrmRouteService` speaks to one
/// endpoint, and that endpoint answers with whatever profile it holds --
/// the public demo server holds the car profile, which is why
/// [TravelMode#WALKING] and [TravelMode#CYCLING] are accepted there but
/// answered with driving data. To offer several modes off self-hosted OSRM,
/// give each mode its own endpoint and pick the matching service:
///
/// ```java
/// OsrmRouteService walking = new OsrmRouteService("https://osrm-foot.example.com");
/// walking.findRoutes(request.setTravelMode(TravelMode.WALKING), callback);
/// ```
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
            throw new IllegalArgumentException("callback is required: routing is asynchronous, "
                    + "so there is no other way to report the result");
        }
        if (request == null || request.getOrigin() == null || request.getDestination() == null) {
            fail(callback, "A route request needs both an origin and a destination", null);
            return;
        }
        String unusable = firstUnroutable(request);
        if (unusable != null) {
            fail(callback, unusable, null);
            return;
        }
        RouteConnection req = new RouteConnection(callback);
        req.setUrl(buildUrl(request));
        req.setPost(false);
        req.start();
    }

    /// Names the first coordinate in `request` that cannot be routed from, or
    /// `null` when they are all usable.
    ///
    /// A `NaN` or infinite coordinate has to be caught before the URL is
    /// built. [#appendFixed] rounds, and `Math.round(NaN)` is 0, so a `NaN`
    /// would be written as `0.000000` and quietly route from null island
    /// instead of failing. [LatLng] does not stop them either: its range
    /// clamps compare against `NaN`, and every such comparison is false.
    private static String firstUnroutable(RouteRequest request) {
        if (!isUsable(request.getOrigin())) {
            return "The route origin is not a usable coordinate";
        }
        for (Object waypoint : request.getWaypoints()) {
            if (!isUsable((LatLng) waypoint)) {
                return "A route waypoint is not a usable coordinate";
            }
        }
        if (!isUsable(request.getDestination())) {
            return "The route destination is not a usable coordinate";
        }
        return null;
    }

    private static boolean isUsable(LatLng coord) {
        return isReal(coord.getLatitude()) && isReal(coord.getLongitude());
    }

    private static boolean isReal(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
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
    /// **Geometry precision**: this reads geometries as `geometries=polyline`,
    /// the precision-5 encoding this service always requests. A response
    /// fetched with `geometries=polyline6` decodes ten times off through here;
    /// decode those yourself with
    /// [com.codename1.maps.PolylineCodec#decode(String, int)] at precision 6.
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
    /// - `IOException`: when the body is malformed -- unparseable, or holding
    ///   a route, leg or step that is not a JSON object -- or when OSRM
    ///   reported that it could not route the request. Malformed input never
    ///   escapes as an unchecked exception, so catching this is enough.
    public static List parseResponse(String json) throws IOException {
        if (json == null || json.length() == 0) {
            throw new IOException("Empty routing response");
        }
        Map root;
        try {
            root = JSONParser.parseJSON(json);
        } catch (RuntimeException e) {
            // The parser is lenient with most junk, but not contractually so.
            // Converting keeps the promise that IOException is all a caller
            // has to catch.
            throw new IOException("Malformed routing response: "
                    + describe(e, "it could not be parsed as JSON"), e);
        }
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
            routes.add(parseRoute(requireObject(routeObj, "route")));
        }
        return routes;
    }

    /// Returns `value` when it is a list, `null` when the field was absent,
    /// and throws when it is present as something else.
    ///
    /// Absent is legitimate -- a request with `steps=false` carries no steps.
    /// Present-but-not-a-list is malformed, and quietly reading it as absent
    /// would hand back a half-populated route instead of reporting the bad
    /// response [#parseResponse(String)] promises to report.
    private static List requireListOrNull(Object value, String what) throws IOException {
        if (value == null) {
            return null;
        }
        if (!(value instanceof List)) {
            throw new IOException("Malformed routing response: " + what + " is not a list");
        }
        return (List) value;
    }

    /// Casts a decoded JSON value that has to be an object, turning the
    /// malformed case into the [IOException] [#parseResponse(String)]
    /// documents rather than letting an unchecked cast escape a public API.
    private static Map requireObject(Object value, String what) throws IOException {
        if (!(value instanceof Map)) {
            throw new IOException("Malformed routing response: " + what + " is not an object");
        }
        return (Map) value;
    }

    private static Route parseRoute(Map json) throws IOException {
        List points = PolylineCodec.decode(string(json.get("geometry")));
        if (points.isEmpty()) {
            // The request always asks for overview=full, so a route with no
            // decodable geometry is a malformed answer rather than a short
            // one. Accepting it would draw an empty polyline and report
            // success, which is worse than saying the response was bad.
            throw new IOException("Malformed routing response: a route carried no geometry");
        }
        List legs = new ArrayList();
        String summary = "";
        Object legsObj = requireListOrNull(json.get("legs"), "route legs");
        if (legsObj != null) {
            for (Object legObj : (List) legsObj) {
                RouteLeg leg = parseLeg(requireObject(legObj, "route leg"));
                legs.add(leg);
                if (summary.length() == 0) {
                    summary = leg.getSummary();
                }
            }
        }
        return new Route(points, legs, number(json.get("distance")),
                number(json.get("duration")), summary);
    }

    private static RouteLeg parseLeg(Map json) throws IOException {
        List steps = new ArrayList();
        Object stepsObj = requireListOrNull(json.get("steps"), "route steps");
        if (stepsObj != null) {
            for (Object stepObj : (List) stepsObj) {
                steps.add(parseStep(requireObject(stepObj, "route step")));
            }
        }
        return new RouteLeg(string(json.get("summary")), number(json.get("distance")),
                number(json.get("duration")), steps);
    }

    private static RouteStep parseStep(Map json) {
        String roadName = string(json.get("name"));
        String type = "";
        String modifier = "";
        int exit = 0;
        LatLng start = null;
        Object maneuverObj = json.get("maneuver");
        if (maneuverObj instanceof Map) {
            Map maneuver = (Map) maneuverObj;
            type = string(maneuver.get("type"));
            modifier = string(maneuver.get("modifier"));
            exit = (int) number(maneuver.get("exit"));
            start = parseLocation(maneuver.get("location"));
        }
        return new RouteStep(describeManeuver(type, modifier, roadName, exit), roadName,
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
    private static String describeManeuver(String type, String modifier, String roadName,
                                           int exit) {
        String onto = roadName.length() > 0 ? " onto " + roadName : "";
        if ("depart".equals(type)) {
            return roadName.length() > 0 ? "Head out on " + roadName : "Start";
        }
        if ("arrive".equals(type)) {
            return "Arrive at your destination";
        }
        if ("turn".equals(type) || "end of road".equals(type)
                || "roundabout turn".equals(type)) {
            // "roundabout turn" is OSRM's small-roundabout case, taken as an
            // ordinary turn. Falling through to the generic wording gave
            // "Roundabout turn onto Main Street" and threw the direction away.
            return turnPhrase(modifier) + onto;
        }
        if ("exit roundabout".equals(type) || "exit rotary".equals(type)) {
            return "Exit the roundabout" + onto;
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
            // OSRM numbers the exit; without it the instruction is useless at
            // every roundabout with more than one way out.
            if (exit > 0) {
                return "At the roundabout take the " + ordinal(exit) + " exit" + onto;
            }
            return "Enter the roundabout and exit" + onto;
        }
        if (type.length() == 0) {
            return "Continue" + onto;
        }
        return capitalize(type) + onto;
    }

    /// English ordinal for a roundabout exit: 1st, 2nd, 3rd, 4th ... The
    /// teens are the exception that catches naive implementations (11th, not
    /// 11st), though a roundabout that large is hypothetical.
    private static String ordinal(int n) {
        int lastTwo = n % 100;
        if (lastTwo >= 11 && lastTwo <= 13) {
            return n + "th";
        }
        switch (n % 10) {
            case 1:
                return n + "st";
            case 2:
                return n + "nd";
            case 3:
                return n + "rd";
            default:
                return n + "th";
        }
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

    /// The readable half of an exception, for the `message` a [RouteCallback]
    /// may put in front of a user. Falls back to `fallback` when the exception
    /// carries no message of its own, so a bare `UnknownHostException` does
    /// not surface as an empty string or as a raw class name.
    private static String describe(Throwable error, String fallback) {
        String message = error == null ? null : error.getMessage();
        return message == null || message.length() == 0 ? fallback : message;
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
    /// The request is deliberately *not* marked `failSilently`:
    /// [com.codename1.io.NetworkManager] swallows transport exceptions
    /// outright for silent requests, so [#handleException] would never run and
    /// the caller would wait forever for a callback that never came. Since
    /// every failure hook here is overridden and none of them delegate to
    /// `super`, nothing reaches the framework's default retry dialog either
    /// way.
    ///
    /// That still leaves one path where nothing would be delivered: an
    /// app-wide error listener registered through
    /// [com.codename1.io.NetworkManager#addErrorListener] that *consumes* the
    /// event makes `NetworkManager` skip this request's hooks entirely. A
    /// completion listener closes that hole -- see [#actionPerformed].
    ///
    /// The success and HTTP-error paths cannot be suppressed at all:
    /// `postResponse` and `handleErrorResponseCode` are invoked on the request
    /// directly, with no listener in between. One residual case survives, and
    /// needs an app to consume *two* different global event streams: a
    /// consuming error listener hides the transport exception, and a progress
    /// listener registered before this one that also consumes
    /// `PROGRESS_TYPE_COMPLETED` starves the backstop, because
    /// [com.codename1.ui.util.EventDispatcher] stops dispatching at a consumed
    /// event. Consuming a progress event has no defined meaning in the
    /// framework, so this is documented rather than worked around -- the only
    /// unsuppressable hook left is a getter the network manager happens to
    /// call, and depending on that side effect would be far more fragile than
    /// the case it guards against.
    private static final class RouteConnection extends ConnectionRequest
            implements ActionListener<NetworkEvent> {

        private final RouteCallback callback;
        private boolean delivered;
        private List routes;
        private String errorMessage;
        private Throwable error;
        /// Attempts queued and not yet completed. Starts at one for the
        /// initial queueing and rises with every [#retry()], so a redirect
        /// hop is not mistaken for the end of the request.
        private int outstandingAttempts = 1;

        RouteConnection(RouteCallback callback) {
            this.callback = callback;
        }

        /// Queues the request, watching for its completion so the callback is
        /// delivered even when nothing else reports the outcome.
        void start() {
            NetworkManager nm = NetworkManager.getInstance();
            nm.addProgressListener(this);
            try {
                nm.addToQueue(this);
            } catch (RuntimeException e) {
                // Queueing validates synchronously -- a base URL that is not
                // HTTP fails right here. Nothing will ever run or complete, so
                // detach the listener (it would otherwise pin this request and
                // its callback to the NetworkManager for good) and report the
                // failure the same way as any other, rather than letting it
                // escape a call documented to answer through the callback.
                detach();
                failLater("The routing request could not be sent: "
                        + describe(e, "the endpoint was rejected"), e);
            }
        }

        /// Counts the extra attempt before delegating. `retry()` runs inside
        /// `performOperationComplete`, ahead of the completion event for the
        /// attempt that scheduled it, so the count is always raised before the
        /// event it has to outlive.
        @Override
        public void retry() {
            synchronized (this) {
                outstandingAttempts++;
            }
            super.retry();
        }

        /// The completion backstop.
        ///
        /// `NetworkManager` fires this from its `finally` block for every
        /// attempt -- including a redirect hop that is about to be retried --
        /// so it cannot be read as "the request is over" on its own. Counting
        /// attempts against [#retry()] can: only when the last one has
        /// completed is there nothing left to wait for. Testing `isRedirecting()`
        /// instead would be a race, because this runs on the EDT after the
        /// network thread may already have started the next attempt and reset
        /// the flag.
        ///
        /// A real result always wins: `postResponse` is queued onto the EDT
        /// before this event, and this hops once more before delivering.
        @Override
        public void actionPerformed(NetworkEvent n) {
            // Identity, not equality: the listener is registered globally, so
            // it sees every request's progress and must pick out this exact
            // instance.
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
            if (delivered) {
                // The common case: `postResponse` is queued onto the EDT ahead
                // of this event, so a real result has already landed and there
                // is nothing to back up. Checking here keeps the success path
                // free of a pointless second hop.
                return;
            }
            failLater("The routing request ended without a result", null);
        }

        @Override
        protected void readResponse(InputStream input) throws IOException {
            byte[] data = Util.readInputStream(input);
            try {
                routes = parseResponse(new String(data, "UTF-8"));
            } catch (Exception e) {
                // Deliberately not Throwable: an OutOfMemoryError or
                // StackOverflowError is a VM problem, not a routing failure,
                // and disguising it as one only makes it harder to find.
                routes = null;
                error = e;
                errorMessage = describe(e, "The routing response could not be read");
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
            failLater("The routing request failed: "
                    + describe(err, "the network could not be reached"), err);
        }

        @Override
        protected void handleErrorResponseCode(int code, String message) {
            failLater("The routing service returned HTTP " + code
                    + (message == null || message.length() == 0 ? "" : " (" + message + ")"), null);
        }

        /// Stops listening for progress. Called from every delivery path, not
        /// just the backstop: an app-level progress listener registered before
        /// this one can *consume* the completion event, and
        /// [com.codename1.ui.util.EventDispatcher] then never reaches this
        /// listener at all. Detaching only from [#actionPerformed] would leave
        /// one listener -- holding this request and the application's callback
        /// -- attached to the [NetworkManager] for every route ever requested.
        private void detach() {
            NetworkManager.getInstance().removeProgressListener(this);
        }

        private void deliverRoutes() {
            if (delivered) {
                return;
            }
            delivered = true;
            detach();
            callback.routesFound(routes);
        }

        private void deliverFailure(String message, Throwable err) {
            if (delivered) {
                return;
            }
            delivered = true;
            detach();
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
