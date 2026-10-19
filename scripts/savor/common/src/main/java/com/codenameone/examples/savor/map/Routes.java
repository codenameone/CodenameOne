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
package com.codenameone.examples.savor.map;

import com.codename1.maps.LatLng;
import com.codename1.maps.routing.Route;
import com.codename1.maps.routing.RouteCallback;
import com.codename1.maps.routing.RouteRequest;
import com.codename1.maps.routing.Routing;
import com.codename1.maps.routing.TravelMode;

import java.util.ArrayList;
import java.util.List;

/// The way from one point to another by road.
///
/// The roads come from `Routing`, whose default service is the public OSRM
/// demo server: free, keyless, and with no promise of being there. So a route
/// that cannot be found is not an error here. The answer is then the straight
/// line, with the time it would take at a city's pace -- the map still shows
/// where the car is going, and the fare is still quoted, from a length that is
/// a little short. For an app you ship, give `Routing.setService` a routing
/// server of your own.
public final class Routes {
    /// A route, reduced to what the screens use.
    public static final class Way {
        public final List<LatLng> points;
        public final double meters;
        public final double seconds;

        Way(List<LatLng> points, double meters, double seconds) {
            this.points = points;
            this.meters = meters;
            this.seconds = seconds;
        }
    }

    public interface Found {
        void found(Way way);
    }

    /// Meters a second in town: 30 km/h.
    private static final double CITY_SPEED = 8.3;

    private static boolean straight;

    private Routes() {
    }

    /// Answers every request with the straight line and asks no server. The
    /// tests turn this on, so that what they see does not depend on one.
    public static void useStraightLines(boolean on) {
        straight = on;
    }

    public static void find(final LatLng from, final LatLng to, final Found found) {
        if (straight) {
            found.found(line(from, to));
            return;
        }
        Routing.findRoute(new RouteRequest(from, to).setTravelMode(TravelMode.DRIVING),
                new RouteCallback() {
                    @Override
                    public void routesFound(List routes) {
                        Object first = routes == null || routes.isEmpty() ? null : routes.get(0);
                        if (!(first instanceof Route)) {
                            found.found(line(from, to));
                            return;
                        }
                        Route route = (Route) first;
                        List<LatLng> points = new ArrayList<LatLng>();
                        for (Object point : route.getPoints()) {
                            if (point instanceof LatLng) {
                                points.add((LatLng) point);
                            }
                        }
                        if (points.size() < 2) {
                            found.found(line(from, to));
                            return;
                        }
                        found.found(new Way(points, route.getDistanceMeters(),
                                route.getDurationSeconds()));
                    }

                    @Override
                    public void routeFailed(String message, Throwable err) {
                        found.found(line(from, to));
                    }
                });
    }

    private static Way line(LatLng from, LatLng to) {
        List<LatLng> points = new ArrayList<LatLng>();
        points.add(from);
        points.add(to);
        double meters = Maps.meters(from, to);
        return new Way(points, meters, meters / CITY_SPEED);
    }

    /// The point `meters` along `points` from its start; the end when the line
    /// is shorter than that.
    public static LatLng along(List<LatLng> points, double meters) {
        double left = meters;
        for (int iter = 1; iter < points.size(); iter++) {
            LatLng a = points.get(iter - 1);
            LatLng b = points.get(iter);
            double leg = Maps.meters(a, b);
            if (left <= leg && leg > 0) {
                double part = left / leg;
                return new LatLng(a.getLatitude() + (b.getLatitude() - a.getLatitude()) * part,
                        a.getLongitude() + (b.getLongitude() - a.getLongitude()) * part);
            }
            left -= leg;
        }
        return points.get(points.size() - 1);
    }
}
