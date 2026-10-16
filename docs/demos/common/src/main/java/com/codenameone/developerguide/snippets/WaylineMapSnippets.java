/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.developerguide.snippets;

import com.codename1.maps.LatLng;
import com.codename1.maps.MapSurface;
import com.codename1.maps.MapView;
import com.codename1.maps.Marker;
import com.codename1.maps.MarkerOptions;
import com.codename1.maps.NativeMap;
import com.codename1.maps.routing.OsrmRouteService;
import com.codename1.maps.routing.Routing;
import com.codename1.maps.vector.MapStyle;
import com.codename1.maps.vector.MvtTileSource;
import com.codename1.ui.EncodedImage;

/// The changes the Wayline chapter describes to the sample's map classes.
public final class WaylineMapSnippets {
    private static boolean dark;

    private WaylineMapSnippets() {
    }

    // tag::wayline-map-own-services[]
    /// Tiles from a server of your own, in place of the keyless default.
    public static MapView create(LatLng center, int zoom) {
        MapView map = new MapView(
                new MvtTileSource("https://tiles.example.com/planet/{z}/{x}/{y}.pbf", 0, 14),
                dark ? MapStyle.dark() : MapStyle.light());
        map.setName("map");
        map.moveCamera(center, zoom);
        return map;
    }

    /// Routes from your own OSRM instance. Call it once, as the app starts.
    public static void useOwnRouting() {
        Routing.setService(new OsrmRouteService("https://osrm.example.com"));
    }
    // end::wayline-map-own-services[]

    // tag::wayline-map-native[]
    /// The platform's own map where the build selected a provider, and the
    /// vector map everywhere else.
    public static NativeMap createNative(LatLng center, int zoom) {
        NativeMap map = new NativeMap(center, zoom, MvtTileSource.openFreeMap(),
                dark ? MapStyle.dark() : MapStyle.light());
        map.setName("map");
        return map;
    }
    // end::wayline-map-native[]

    // tag::wayline-map-native-marker[]
    /// Moves a marker on a map that may be native. A native provider is told
    /// about a marker when it is added and not again, so a marker that moved
    /// is taken off and put back.
    static Marker move(MapSurface map, Marker marker, EncodedImage icon, LatLng position) {
        if (!(map instanceof NativeMap) || !((NativeMap) map).isNativeMap()) {
            marker.setPosition(position);
            return marker;
        }
        map.removeMarker(marker);
        return map.addMarker(new MarkerOptions(position).icon(icon).anchor(0.5f, 0.5f));
    }
    // end::wayline-map-native-marker[]
}
