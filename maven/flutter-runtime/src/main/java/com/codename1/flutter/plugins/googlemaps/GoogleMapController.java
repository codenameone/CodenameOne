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

import com.codename1.maps.MapView;

import dart.async.Future;

/** google_maps_flutter's GoogleMapController, driving the element's MapView. */
public class GoogleMapController {
    private final GoogleMapRenderElement element;

    GoogleMapController(GoogleMapRenderElement element) {
        this.element = element;
    }

    public Future<Object> animateCamera(CameraUpdate update, Object duration) {
        return moveCamera(update);
    }

    public Future<Object> moveCamera(CameraUpdate update) {
        element.camera(update.applyTo(element.camera()));
        return Future.value(null);
    }

    public Future<Double> getZoomLevel() {
        return Future.value(Double.valueOf(element.camera().zoom()));
    }

    public Future<Object> showMarkerInfoWindow(MarkerId id) {
        return Future.value(null);
    }

    public Future<Object> hideMarkerInfoWindow(MarkerId id) {
        return Future.value(null);
    }

    public Future<Object> setMapStyle(String style) {
        return Future.value(null);
    }

    public void dispose() {
    }

    MapView view() {
        return element.view();
    }
}
