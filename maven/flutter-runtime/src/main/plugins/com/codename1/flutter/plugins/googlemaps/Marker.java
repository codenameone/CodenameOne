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

import dart.runtime.Funcs;

/** google_maps_flutter's Marker: an id, a position, and an info window. */
public class Marker {
    private MarkerId markerId;
    private LatLng position = new LatLng(0, 0);
    private InfoWindow infoWindow;
    private boolean draggable;
    private boolean visible = true;
    private Funcs.VoidFunc0 onTap;

    public Marker() {
    }

    public void markerId(MarkerId v) {
        markerId = v;
    }

    public void position(LatLng v) {
        position = v;
    }

    public void infoWindow(InfoWindow v) {
        infoWindow = v;
    }

    public void draggable(boolean v) {
        draggable = v;
    }

    public void visible(boolean v) {
        visible = v;
    }

    public void onTap(Funcs.VoidFunc0 v) {
        onTap = v;
    }

    /** Accepted and ignored: markers use the platform's default pin. */
    public void icon(Object v) {
    }

    public void alpha(double v) {
    }

    public void rotation(double v) {
    }

    public void zIndex(double v) {
    }

    public void onDragEnd(Funcs.VoidFunc1<LatLng> v) {
    }

    public MarkerId markerId() {
        return markerId;
    }

    public LatLng position() {
        return position;
    }

    public InfoWindow infoWindow() {
        return infoWindow;
    }

    public boolean draggable() {
        return draggable;
    }

    public boolean visible() {
        return visible;
    }

    Funcs.VoidFunc0 tapHandler() {
        return onTap;
    }
}
