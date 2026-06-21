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
package com.codename1.maps;

import com.codename1.ui.Component;
import com.codename1.ui.geom.Point;

/// The provider-agnostic map API shared by the pure-vector [MapView] and the
/// native-peer [NativeMap].
///
/// Application code should program against `MapSurface` so it does not need
/// to know whether the map is drawn by the built-in vector engine or by a
/// native provider (Apple MapKit, Google Maps, ...). The two concrete
/// components differ only in how they render; their behavior through this
/// interface is identical, and a [NativeMap] with no native provider wired
/// in transparently delegates to an embedded [MapView].
///
/// Named `MapSurface` rather than `Map` to avoid clashing with
/// `java.util.Map`.
public interface MapSurface {

    // ---- Camera -----------------------------------------------------------

    /// The current camera position (target, zoom, bearing, tilt).
    CameraPosition getCameraPosition();

    /// Moves the camera to `position`, animating where the backend supports it.
    void setCameraPosition(CameraPosition position);

    /// Convenience to recenter at `target` and set `zoom` in one call.
    void moveCamera(LatLng target, double zoom);

    /// The current zoom level.
    double getZoom();

    /// Sets the zoom level, keeping the current center.
    void setZoom(double zoom);

    /// The smallest zoom level the backend permits.
    double getMinZoom();

    /// The largest zoom level the backend permits.
    double getMaxZoom();

    /// The geographic coordinate at the center of the viewport.
    LatLng getCenter();

    /// Recenters the viewport at `center`, keeping the current zoom.
    void setCenter(LatLng center);

    // ---- Bounds -----------------------------------------------------------

    /// The geographic bounds currently visible, or `null` before layout.
    /// (Named `getVisibleRegion` to avoid clashing with
    /// `Component.getVisibleBounds()`, which returns a pixel rectangle.)
    MapBounds getVisibleRegion();

    /// Moves and zooms the camera so `bounds` fits within the viewport,
    /// inset by `paddingPixels` on every edge.
    void fitBounds(MapBounds bounds, int paddingPixels);

    // ---- Map objects ------------------------------------------------------

    /// Adds a marker described by `options` and returns its live handle.
    Marker addMarker(MarkerOptions options);

    /// Removes a previously added marker.
    void removeMarker(Marker marker);

    /// Adds a polyline and returns it for chaining.
    Polyline addPolyline(Polyline polyline);

    /// Removes a previously added polyline.
    void removePolyline(Polyline polyline);

    /// Adds a polygon and returns it for chaining.
    Polygon addPolygon(Polygon polygon);

    /// Removes a previously added polygon.
    void removePolygon(Polygon polygon);

    /// Adds a circle and returns it for chaining.
    Circle addCircle(Circle circle);

    /// Removes a previously added circle.
    void removeCircle(Circle circle);

    /// Removes every marker, polyline, polygon and circle.
    void clearMapObjects();

    // ---- Coordinate conversion -------------------------------------------

    /// Converts a geographic coordinate to a pixel relative to this component.
    Point latLngToScreen(LatLng coord);

    /// Converts a pixel relative to this component to a geographic coordinate.
    LatLng screenToLatLng(int x, int y);

    // ---- Listeners --------------------------------------------------------

    /// Registers a tap listener.
    void addTapListener(MapTapListener l);

    /// Unregisters a tap listener.
    void removeTapListener(MapTapListener l);

    /// Registers a long-press listener.
    void addLongPressListener(MapTapListener l);

    /// Unregisters a long-press listener.
    void removeLongPressListener(MapTapListener l);

    /// Registers a camera-change listener.
    void addCameraChangeListener(CameraChangeListener l);

    /// Unregisters a camera-change listener.
    void removeCameraChangeListener(CameraChangeListener l);

    // ---- Backend introspection -------------------------------------------

    /// True when a native provider currently backs this surface; false for a
    /// pure-vector map or a [NativeMap] that fell back to the vector engine.
    boolean isNativeMap();

    /// Whether the vector engine still has tiles in flight for the current
    /// view. Useful to defer a screenshot (or hide a spinner) until the basemap
    /// has finished loading. Always false for a native-provider-backed surface,
    /// which loads its own tiles outside the engine's knowledge.
    boolean isLoadingTiles();

    /// This surface as a Codename One [Component] for layout purposes.
    Component asComponent();
}
