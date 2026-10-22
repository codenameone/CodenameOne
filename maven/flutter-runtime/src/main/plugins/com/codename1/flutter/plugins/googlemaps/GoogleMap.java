/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.googlemaps;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * google_maps_flutter's GoogleMap, hosting Codename One's {@code MapView} -- native
 * maps where the platform has them, vector tiles elsewhere. Markers, the camera,
 * taps and camera moves are supported; polylines, polygons, circles and map styles
 * are accepted and not drawn.
 */
public class GoogleMap extends Widget {
    CameraPosition initialCameraPosition = new CameraPosition();
    Iterable<?> markers;
    MapType mapType = MapType.normal;
    Funcs.VoidFunc1<GoogleMapController> onMapCreated;
    Funcs.VoidFunc1<LatLng> onTap;
    Funcs.VoidFunc1<LatLng> onLongPress;
    Funcs.VoidFunc1<CameraPosition> onCameraMove;
    Funcs.VoidFunc0 onCameraIdle;

    public GoogleMap() {
    }

    public void initialCameraPosition(CameraPosition v) {
        initialCameraPosition = v;
    }

    public void markers(Iterable<?> v) {
        markers = v;
    }

    public void mapType(MapType v) {
        mapType = v;
    }

    public void onMapCreated(Funcs.VoidFunc1<GoogleMapController> v) {
        onMapCreated = v;
    }

    public void onTap(Funcs.VoidFunc1<LatLng> v) {
        onTap = v;
    }

    public void onLongPress(Funcs.VoidFunc1<LatLng> v) {
        onLongPress = v;
    }

    public void onCameraMove(Funcs.VoidFunc1<CameraPosition> v) {
        onCameraMove = v;
    }

    public void onCameraIdle(Funcs.VoidFunc0 v) {
        onCameraIdle = v;
    }

    /** Accepted and ignored. */
    public void polylines(Object v) {
    }

    public void polygons(Object v) {
    }

    public void circles(Object v) {
    }

    public void myLocationEnabled(boolean v) {
    }

    public void myLocationButtonEnabled(boolean v) {
    }

    public void zoomControlsEnabled(boolean v) {
    }

    public void zoomGesturesEnabled(boolean v) {
    }

    public void scrollGesturesEnabled(boolean v) {
    }

    public void rotateGesturesEnabled(boolean v) {
    }

    public void tiltGesturesEnabled(boolean v) {
    }

    public void compassEnabled(boolean v) {
    }

    public void mapToolbarEnabled(boolean v) {
    }

    public void trafficEnabled(boolean v) {
    }

    public void buildingsEnabled(boolean v) {
    }

    public void liteModeEnabled(boolean v) {
    }

    public void padding(Object v) {
    }

    public void minMaxZoomPreference(Object v) {
    }

    public void gestureRecognizers(Object v) {
    }

    public void style(String v) {
    }

    @Override
    public Element createElement() {
        return new GoogleMapRenderElement(this);
    }
}
