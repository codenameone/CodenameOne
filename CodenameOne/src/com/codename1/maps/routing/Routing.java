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

import com.codename1.maps.LatLng;
import com.codename1.maps.MapSurface;
import com.codename1.ui.CN;

import java.util.List;

/// The entry point for road-following routes.
///
/// A [com.codename1.maps.Polyline] joins the points you give it with straight
/// lines. To draw the road a driver would actually take you need a routing
/// service to work out the road geometry first, which is what this class does:
///
/// ```java
/// MapView map = new MapView();
/// Routing.showRoute(map, new LatLng(38.8977, -77.0365), new LatLng(38.8894, -77.0352));
/// ```
///
/// Routing is asynchronous -- the call returns immediately and the line
/// appears once the service answers. For control over the result use
/// [#findRoute(RouteRequest, RouteCallback)]:
///
/// ```java
/// Routing.findRoute(new RouteRequest(origin, destination), new RouteCallback() {
///     public void routesFound(List routes) {
///         Route best = (Route)routes.get(0);
///         map.addPolyline(best.toPolyline().setStrokeColor(0xff5722));
///         map.fitBounds(best.getBounds(), 40);
///         distanceLabel.setText((int)(best.getDistanceMeters() / 1000) + " km");
///     }
///
///     public void routeFailed(String message, Throwable error) {
///         ToastBar.showErrorMessage(message);
///     }
/// });
/// ```
///
/// The work is done by a [RouteService]. Unless you install another, that is
/// the keyless [OsrmRouteService] -- read its documentation before shipping,
/// since its default endpoint is a public demo server.
public final class Routing {

    private static RouteService service;

    private Routing() {
    }

    /// The service backing every routing call, creating the default keyless
    /// [OsrmRouteService] on first use.
    public static synchronized RouteService getService() {
        if (service == null) {
            service = new OsrmRouteService();
        }
        return service;
    }

    /// Installs the service backing every routing call. Pass `null` to restore
    /// the default [OsrmRouteService].
    public static synchronized void setService(RouteService service) {
        Routing.service = service;
    }

    /// Routes from `origin` to `destination` by car and reports the outcome to
    /// `callback`.
    public static void findRoute(LatLng origin, LatLng destination, RouteCallback callback) {
        findRoute(new RouteRequest(origin, destination), callback);
    }

    /// Routes `request` and reports the outcome to `callback`.
    ///
    /// Returns immediately; `callback` is invoked later on the event dispatch
    /// thread, exactly once. A service that reports itself unavailable -- one
    /// still waiting for an API key, say -- fails the request here with a
    /// readable message instead of letting it turn into a network error, so
    /// callers never have to check [RouteService#isAvailable()] themselves.
    ///
    /// Throws `IllegalArgumentException` when `callback` is `null`; an
    /// asynchronous call has no other way to reach you.
    public static void findRoute(RouteRequest request, final RouteCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback is required: routing is asynchronous, "
                    + "so there is no other way to report the result");
        }
        RouteService routeService = getService();
        final OnceOnly guarded = new OnceOnly(callback);
        if (!routeService.isAvailable()) {
            final String id = routeService.getId();
            CN.callSerially(new Runnable() {
                @Override
                public void run() {
                    guarded.routeFailed("The routing service '" + id
                            + "' is not ready to route; it may still need to be configured", null);
                }
            });
            return;
        }
        try {
            routeService.findRoutes(request, guarded);
        } catch (final RuntimeException e) {
            // A service is required to answer through the callback, but this
            // facade promises the app exactly one answer and cannot rely on a
            // third-party implementation keeping its side of the bargain. The
            // latch below makes this harmless if the service already reported.
            CN.callSerially(new Runnable() {
                @Override
                public void run() {
                    guarded.routeFailed("The routing service failed: "
                            + (e.getMessage() == null ? e.toString() : e.getMessage()), e);
                }
            });
        }
    }

    /// Passes the first outcome through and swallows any that follow, so a
    /// misbehaving [RouteService] cannot make [#findRoute(RouteRequest, RouteCallback)]
    /// break its own exactly-once promise. Touched only on the event dispatch
    /// thread, where every delivery is dispatched.
    private static final class OnceOnly implements RouteCallback {

        private final RouteCallback delegate;
        private boolean delivered;

        OnceOnly(RouteCallback delegate) {
            this.delegate = delegate;
        }

        @Override
        public void routesFound(List routes) {
            if (delivered) {
                return;
            }
            delivered = true;
            delegate.routesFound(routes);
        }

        @Override
        public void routeFailed(String message, Throwable error) {
            if (delivered) {
                return;
            }
            delivered = true;
            delegate.routeFailed(message, error);
        }
    }

    /// Draws the best route between `origin` and `destination` on `map` and
    /// frames it, with no further code required.
    ///
    /// This is the least code that gets you from two coordinates to a line
    /// following the roads. Failures are silent -- when you need to report
    /// them, or to style the line, use
    /// [#showRoute(MapSurface, RouteRequest, RouteCallback)].
    ///
    /// #### Parameters
    ///
    /// - `map`: the map to draw on
    ///
    /// - `origin`: where the journey starts
    ///
    /// - `destination`: where the journey ends
    public static void showRoute(MapSurface map, LatLng origin, LatLng destination) {
        showRoute(map, new RouteRequest(origin, destination), null);
    }

    /// Draws the best route for `request` on `map`, frames it, and forwards
    /// the outcome to `callback`.
    ///
    /// The polyline is added and the camera moved *before* `callback` runs, so
    /// the callback can read the route's distance and duration to update the
    /// UI. It cannot restyle the line that was drawn -- that polyline is not
    /// exposed, and [Route#toPolyline()] hands back a fresh one every call. To
    /// control how the route looks, skip this method: call
    /// [#findRoute(RouteRequest, RouteCallback)] and add the styled polyline
    /// yourself.
    ///
    /// #### Parameters
    ///
    /// - `map`: the map to draw on
    ///
    /// - `request`: the journey to route
    ///
    /// - `callback`: notified of the outcome, or `null` to just draw the route
    public static void showRoute(final MapSurface map, RouteRequest request,
                                 final RouteCallback callback) {
        findRoute(request, new RouteCallback() {
            @Override
            public void routesFound(List routes) {
                if (routes == null || routes.isEmpty()) {
                    // A service that breaks the contract shouldn't surface as
                    // an IndexOutOfBoundsException in the middle of the EDT.
                    routeFailed("The routing service returned no route", null);
                    return;
                }
                Route best = (Route) routes.get(0);
                map.addPolyline(best.toPolyline());
                if (best.getBounds() != null) {
                    map.fitBounds(best.getBounds(), CN.convertToPixels(4));
                }
                if (callback != null) {
                    callback.routesFound(routes);
                }
            }

            @Override
            public void routeFailed(String message, Throwable error) {
                if (callback != null) {
                    callback.routeFailed(message, error);
                }
            }
        });
    }
}
