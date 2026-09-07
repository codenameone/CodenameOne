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

package com.codenameone.developerguide.snippets.generated;

import com.codename1.gpu.*;
import com.codename1.ui.*;
import com.codename1.ui.animations.*;
import com.codename1.ui.events.*;
import com.codename1.ui.geom.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.list.*;
import com.codename1.ui.plaf.*;
import com.codename1.ui.util.*;
import com.codename1.components.*;
import com.codename1.charts.models.*;
import com.codename1.charts.renderers.*;
import com.codename1.charts.views.*;
import com.codename1.capture.*;
import com.codename1.io.*;
import com.codename1.l10n.*;
import com.codename1.location.*;
import com.codename1.maps.*;
import com.codename1.media.*;
import com.codename1.messaging.*;
import com.codename1.payment.*;
import com.codename1.processing.*;
import com.codename1.properties.*;
import com.codename1.push.*;
import com.codename1.security.*;
import com.codename1.social.*;
import com.codename1.ui.spinner.*;
import java.io.*;
import com.codename1.ui.geom.Point;
import java.util.*;


class MapsJava008Snippet {


    Object context;
    Object url;
    Object value;
    Object body;
    Object event;
    String apiKey = "test-key";
    String myHttpsURL = "https://example.com";
    java.util.List<String> validKeysList = new java.util.ArrayList<>();
    Image myImage;
    Graphics graphics;
    Graphics g;
    GraphicsDevice device;
    Form form;
    Form hi;
    Container cnt;
    Container myForm;
    Component component;
    Button button;
    MultiButton myMultiButton;
    Label label;
    BrowserComponent browserComponent;
    Resources theme;
    
    void snippet() throws Exception {
        // tag::maps-java-008[]
        // Camera
        map.setCameraPosition(new CameraPosition(new LatLng(48.8566, 2.3522), 11));
        map.moveCamera(new LatLng(48.8566, 2.3522), 11);
        map.setZoom(13);
        map.fitBounds(new MapBounds(new LatLng(48.8, 2.2), new LatLng(48.9, 2.4)), 24);

        // Markers
        Marker m = map.addMarker(new MarkerOptions(new LatLng(48.8584, 2.2945))
                .icon(pinImage)
                .title("Eiffel Tower")
                .anchor(0.5f, 1.0f)
                .onClick(e -> showDetails()));
        map.removeMarker(m);

        // Shapes
        map.addPolyline(new Polyline(routePoints).setStrokeColor(0xff5722).setStrokeWidth(6));
        map.addPolygon(new Polygon(areaPoints).setFillColor(0x803f51b5).setStrokeColor(0x3f51b5));
        map.addCircle(new Circle(new LatLng(48.85, 2.35), 500).setFillColor(0x804caf50));
        map.clearMapObjects();

        // Coordinate conversion and bounds
        Point pixel = map.latLngToScreen(new LatLng(48.85, 2.35));
        LatLng coord = map.screenToLatLng(120, 240);
        MapBounds visible = map.getVisibleRegion();

        // Events
        map.addTapListener((surface, location, x, y) -> placeMarker(location));
        map.addLongPressListener((surface, location, x, y) -> contextMenu(location));
        map.addCameraChangeListener((surface, camera) -> persist(camera));
        // end::maps-java-008[]
    }

    MapSurface map;
    EncodedImage pinImage;
    LatLng[] routePoints = new LatLng[0];
    LatLng[] areaPoints = new LatLng[0];
    void showDetails() { }
    void placeMarker(LatLng location) { }
    void contextMenu(LatLng location) { }
    void persist(CameraPosition camera) { }

}
