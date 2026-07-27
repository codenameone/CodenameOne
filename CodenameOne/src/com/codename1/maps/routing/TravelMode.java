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

/// How the traveller moves, which decides the road network and speeds a
/// [RouteService] routes over.
///
/// Not every backend implements every mode. A service that cannot honor the
/// requested mode answers with the closest profile it does have rather than
/// failing the request, so check the [RouteService] you installed before
/// promising a user a walking route -- see [OsrmRouteService] for how the
/// built-in default behaves.
public enum TravelMode {

    /// Route over roads open to cars, respecting one-way streets and turn
    /// restrictions.
    DRIVING("driving"),

    /// Route over footpaths and pedestrian crossings, ignoring one-way
    /// restrictions that do not apply on foot.
    WALKING("walking"),

    /// Route over cycleways and bike-legal roads.
    CYCLING("cycling");

    private final String id;

    TravelMode(String id) {
        this.id = id;
    }

    /// The lowercase wire identifier (`driving`, `walking`, `cycling`) used in
    /// routing service URLs.
    public String getId() {
        return id;
    }
}
