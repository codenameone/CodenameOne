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

/// A backend that turns a [RouteRequest] into road-following [Route]s.
///
/// Codename One ships [OsrmRouteService], which needs no API key, and
/// [Routing] uses it unless you install another. Implement this interface to
/// route through a different provider (Google Directions, Mapbox Directions,
/// GraphHopper, your own server) and install it with
/// [Routing#setService(RouteService)] -- application code that calls
/// [Routing] keeps working unchanged.
public interface RouteService {

    /// A short identifier for this backend, for example `"osrm"`. Used in log
    /// messages and to tell services apart.
    String getId();

    /// Whether this service can route right now. A service that needs an API
    /// key returns false until one is configured, letting callers fail fast
    /// with a clear message instead of a network error.
    boolean isAvailable();

    /// Routes `request` and reports the outcome to `callback`.
    ///
    /// The call returns immediately; the work happens off the event dispatch
    /// thread and the callback is invoked back on it. Implementations must
    /// invoke exactly one callback method for every request, including when
    /// the request is rejected outright.
    void findRoutes(RouteRequest request, RouteCallback callback);
}
