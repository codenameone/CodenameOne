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
package com.codename1.maps.spi;

import com.codename1.maps.NativeMap;
import com.codename1.ui.Component;

/// The service-provider interface a native map backend implements so the
/// core [NativeMap] component can drive it without knowing which provider
/// (Apple MapKit, Google Maps, Bing, Huawei, ...) is in use.
///
/// This is a plain interface, deliberately **not** a
/// `com.codename1.system.NativeInterface`: core neither ships nor references
/// any concrete implementation. When the developer selects a provider with
/// the `maps.provider` build hint, the build pushes a provider implementation
/// (carrying the platform-native methods) into the `com.codename1.maps`
/// package of the app and weaves in a call to
/// [MapProviderRegistry#register(MapProvider)]. Absent that injection the
/// registry stays empty and `NativeMap` falls back to the pure-vector
/// `MapView`.
///
/// Every method is keyed by a `mapId` so a single provider implementation can
/// serve multiple `NativeMap` instances. Coordinate conversion uses a stateful
/// "calculate then read" idiom ([#calcScreenPosition] then [#getScreenX] /
/// [#getScreenY]) to avoid returning multiple values across the native
/// boundary. Native code reports user interaction back through the static
/// callbacks on [NativeMap] (`fireTap`, `fireLongPress`, `fireMarkerClick`,
/// `fireCameraChange`, `fireMapReady`).
public interface MapProvider {

    /// Map type: standard street map.
    int MAP_TYPE_STANDARD = 0;
    /// Map type: satellite imagery.
    int MAP_TYPE_SATELLITE = 1;
    /// Map type: hybrid imagery with labels.
    int MAP_TYPE_HYBRID = 2;
    /// Map type: terrain relief.
    int MAP_TYPE_TERRAIN = 3;

    /// A stable identifier for this provider, e.g. `"apple"` or `"google"`.
    String getId();

    /// Whether this provider can render on the current device right now
    /// (e.g. Google checks that Play Services is present). When this returns
    /// false `NativeMap` falls back to the vector engine.
    boolean isAvailable();

    /// Creates the view for the map identified by `mapId`. Native providers
    /// return a [com.codename1.ui.PeerComponent] wrapping the native map view;
    /// a web-SDK provider returns a [com.codename1.ui.BrowserComponent]. Either
    /// way it is a [Component] the [NativeMap] adds to its layout. Returning
    /// `null` triggers the vector fallback.
    Component createPeer(NativeMap host, int mapId);

    /// Releases native resources for `mapId` when the map is no longer used.
    void deinitialize(int mapId);

    // ---- Camera -----------------------------------------------------------

    /// Moves the camera. `bearing` and `tilt` are in degrees; providers that
    /// do not support them ignore those arguments.
    void setCamera(int mapId, double lat, double lon, float zoom, float bearing, float tilt);

    double getLatitude(int mapId);

    double getLongitude(int mapId);

    float getZoom(int mapId);

    float getMaxZoom(int mapId);

    float getMinZoom(int mapId);

    // ---- Markers and shapes ----------------------------------------------

    /// Adds a marker. `icon` is PNG bytes (or `null` for the default pin);
    /// returns an opaque element key. `anchorU`/`anchorV` are normalized.
    long addMarker(int mapId, byte[] icon, double lat, double lon,
                   String title, String snippet, float anchorU, float anchorV);

    /// Starts accumulating a path; feed it with [#addToPath].
    long beginPath(int mapId);

    void addToPath(int mapId, long pathId, double lat, double lon);

    /// Finishes the path as a stroked polyline and returns its element key.
    long finishPolyline(int mapId, long pathId, int strokeColor, int strokeWidth);

    /// Finishes the path as a filled polygon and returns its element key.
    long finishPolygon(int mapId, long pathId, int fillColor, int strokeColor, int strokeWidth);

    /// Adds a geodesic circle and returns its element key.
    long addCircle(int mapId, double lat, double lon, double radiusMeters,
                   int fillColor, int strokeColor, int strokeWidth);

    void removeElement(int mapId, long elementId);

    void removeAllElements(int mapId);

    // ---- Coordinate conversion (stateful) --------------------------------

    void calcScreenPosition(int mapId, double lat, double lon);

    int getScreenX(int mapId);

    int getScreenY(int mapId);

    void calcLatLongPosition(int mapId, int x, int y);

    double getScreenLat(int mapId);

    double getScreenLon(int mapId);

    // ---- Gestures and features -------------------------------------------

    void setShowMyLocation(int mapId, boolean show);

    void setRotateGestureEnabled(int mapId, boolean enabled);

    void setMapType(int mapId, int type);
}
