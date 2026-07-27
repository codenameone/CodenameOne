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
/// Road-following routes for the modern maps API.
///
/// A [com.codename1.maps.Polyline] connects the coordinates you hand it with
/// straight segments, which is rarely what a navigation screen wants. This
/// package works out the geometry of the roads between two points so the line
/// on the map traces the drive a user would actually make.
///
/// [com.codename1.maps.routing.Routing] is the entry point and works with no
/// configuration -- it routes over OpenStreetMap data through the keyless
/// [com.codename1.maps.routing.OsrmRouteService], mirroring how
/// [com.codename1.maps.MapView] draws the keyless OpenFreeMap basemap:
///
/// ```java
/// MapView map = new MapView();
/// Routing.showRoute(map, origin, destination);
/// ```
///
/// A [com.codename1.maps.routing.RouteRequest] adds waypoints and a
/// [com.codename1.maps.routing.TravelMode]; the resulting
/// [com.codename1.maps.routing.Route] carries the drawable geometry, the total
/// distance and duration, and the
/// [com.codename1.maps.routing.RouteLeg]/[com.codename1.maps.routing.RouteStep]
/// breakdown behind a turn-by-turn list.
///
/// The default service is backed by OSRM's public *demo* server, which is fine
/// for development but has no SLA. Production apps point it at their own
/// instance, or install a different backend entirely by implementing
/// [com.codename1.maps.routing.RouteService] and passing it to
/// [com.codename1.maps.routing.Routing#setService] -- app code that calls
/// `Routing` is unaffected either way.
package com.codename1.maps.routing;
