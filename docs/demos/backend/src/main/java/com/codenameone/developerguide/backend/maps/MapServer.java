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
package com.codenameone.developerguide.backend.maps;

import com.codename1.backend.Backend;
import com.codename1.backend.MapTiles;
import com.codename1.backend.osm.OsmExtract;
import com.codename1.backend.osm.OsmGeocoding;
import com.codename1.backend.osm.OsmRouting;

/// Serves tiles, routes and addresses from two files on the server's disk.
public final class MapServer {
    private MapServer() {
    }

    /// Loads the extract and starts the server.
    public static void main(String[] args) throws Exception {
        // tag::backend-maps-server[]
        OsmExtract town = OsmExtract.load("/data/monaco.osm.pbf");
        Backend.builder()
                .handler(new MapTiles("/data/monaco.pmtiles", "/tiles"))
                .handler(new OsmRouting(town, "/osrm"))
                .handler(new OsmGeocoding(town, "/geo"))
                .start();
        // end::backend-maps-server[]
    }
}
