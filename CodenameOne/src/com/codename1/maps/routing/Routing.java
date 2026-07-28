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
        final OnceOnly guarded = new OnceOnly(callback);
        try {
            // Everything that touches the service belongs inside the guard,
            // not just the routing call: resolving it, asking whether it is
            // ready and reading its id are all third-party code, and any of
            // them throwing would otherwise escape a method that promises the
            // caller exactly one asynchronous answer.
            RouteService routeService = getService();
            if (!routeService.isAvailable()) {
                String reported = routeService.getId();
                // A service is supposed to have an id, but quoting a null or
                // blank one back at the user reads like a bug in the message.
                final String id = reported == null || reported.length() == 0
                        ? "unknown" : reported;
                guarded.routeFailed("The routing service '" + id
                        + "' is not ready to route; it may still need to be configured", null);
                return;
            }
            routeService.findRoutes(request, guarded);
        } catch (RuntimeException e) {
            // A service is required to answer through the callback, but this
            // facade promises the app exactly one answer and cannot rely on a
            // third-party implementation keeping its side of the bargain. The
            // wrapper latches, so this is a no-op when the service already
            // reported, and it handles getting onto the EDT.
            guarded.routeFailed("The routing service failed: "
                    + (e.getMessage() == null ? e.toString() : e.getMessage()), e);
        }
    }

    /// Enforces both halves of what [#findRoute(RouteRequest, RouteCallback)]
    /// promises the application -- exactly one outcome, on the event dispatch
    /// thread -- no matter how the [RouteService] behind it behaves.
    ///
    /// The SPI requires implementations to deliver once and on the EDT, but a
    /// facade cannot assume a third-party implementation honors either. A
    /// service that reports from a background thread, reports twice, or
    /// reports concurrently with throwing would otherwise push its bug
    /// straight through to application code that has every right to expect
    /// the documented behavior. So the latch is claimed under a lock rather
    /// than with a plain field read, and whatever wins is re-dispatched onto
    /// the EDT (passed straight through when already there, so the common
    /// case costs nothing).
    private static final class OnceOnly implements RouteCallback {

        private final RouteCallback delegate;
        private boolean delivered;

        OnceOnly(RouteCallback delegate) {
            this.delegate = delegate;
        }

        /// Claims the single delivery. Returns true for exactly one caller
        /// however many threads race here.
        private synchronized boolean claim() {
            if (delivered) {
                return false;
            }
            delivered = true;
            return true;
        }

        @Override
        public void routesFound(final List routes) {
            if (!claim()) {
                return;
            }
            onEdt(new Runnable() {
                @Override
                public void run() {
                    delegate.routesFound(routes);
                }
            });
        }

        @Override
        public void routeFailed(final String message, final Throwable error) {
            if (!claim()) {
                return;
            }
            onEdt(new Runnable() {
                @Override
                public void run() {
                    delegate.routeFailed(message, error);
                }
            });
        }

        /// Always queues rather than running inline when already on the EDT.
        /// The contract is "returns immediately, answers later", and a
        /// service that happens to answer synchronously would otherwise run
        /// the application's callback before `findRoute` had returned -- the
        /// caller would be re-entered mid-setup, and the timing would differ
        /// between services for no reason the caller can see.
        private static void onEdt(Runnable r) {
            CN.callSerially(r);
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
