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

import java.util.ArrayList;
import java.util.List;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.maps.CameraChangeListener;
import com.codename1.maps.MapSurface;
import com.codename1.maps.MapTapListener;
import com.codename1.maps.MapView;
import com.codename1.maps.MarkerOptions;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import dart.runtime.Funcs;

/**
 * Hosts a {@link MapView} for {@link GoogleMap}. Headless (tests), no view is created:
 * the camera is tracked here, so a controller still answers it.
 */
public class GoogleMapRenderElement extends RenderElement {
    private static final double DEFAULT_SIZE_LP = 300;

    private MapView view;
    private CameraPosition camera;
    private final List<com.codename1.maps.Marker> placed = new ArrayList<com.codename1.maps.Marker>();
    private boolean created;

    public GoogleMapRenderElement(GoogleMap widget) {
        super(widget);
        camera = widget.initialCameraPosition;
    }

    private GoogleMap map() {
        return (GoogleMap) widget();
    }

    MapView view() {
        return view;
    }

    CameraPosition camera() {
        if (view != null) {
            camera = CameraPosition.of(view.getCameraPosition());
        }
        return camera;
    }

    void camera(CameraPosition p) {
        camera = p;
        if (view != null) {
            view.setCameraPosition(p.toCn1());
        }
    }

    @Override
    protected Component createComponent() {
        if (Display.isInitialized()) {
            view = new MapView();
            view.setCameraPosition(camera.toCn1());
            view.addTapListener(new MapTapListener() {
                @Override
                public void mapTapped(MapSurface m, com.codename1.maps.LatLng at, int x, int y) {
                    Funcs.VoidFunc1<LatLng> h = map().onTap;
                    if (h != null) {
                        h.call(LatLng.of(at));
                    }
                }
            });
            view.addLongPressListener(new MapTapListener() {
                @Override
                public void mapTapped(MapSurface m, com.codename1.maps.LatLng at, int x, int y) {
                    Funcs.VoidFunc1<LatLng> h = map().onLongPress;
                    if (h != null) {
                        h.call(LatLng.of(at));
                    }
                }
            });
            view.addCameraChangeListener(new CameraChangeListener() {
                @Override
                public void cameraChanged(MapSurface m, com.codename1.maps.CameraPosition p) {
                    camera = CameraPosition.of(p);
                    if (map().onCameraMove != null) {
                        map().onCameraMove.call(camera);
                    }
                    if (map().onCameraIdle != null) {
                        map().onCameraIdle.call();
                    }
                }
            });
            placeMarkers();
        }
        notifyCreated();
        return view == null ? null : view.asComponent();
    }

    @Override
    protected void updateComponent(Component c) {
        placeMarkers();
    }

    private void notifyCreated() {
        if (created) {
            return;
        }
        created = true;
        final Funcs.VoidFunc1<GoogleMapController> h = map().onMapCreated;
        if (h == null) {
            return;
        }
        final GoogleMapController c = new GoogleMapController(this);
        if (!Display.isInitialized()) {
            h.call(c);
            return;
        }
        // after this frame, as Flutter does: the callback commonly calls setState
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                h.call(c);
            }
        });
    }

    private void placeMarkers() {
        if (view == null) {
            return;
        }
        for (com.codename1.maps.Marker m : placed) {
            view.removeMarker(m);
        }
        placed.clear();
        if (map().markers == null) {
            return;
        }
        for (Object o : map().markers) {
            if (!(o instanceof Marker)) {
                continue;
            }
            final Marker mk = (Marker) o;
            if (!mk.visible()) {
                continue;
            }
            MarkerOptions opts = new MarkerOptions(mk.position().toCn1()).draggable(mk.draggable());
            if (mk.infoWindow() != null) {
                opts.title(mk.infoWindow().title()).snippet(mk.infoWindow().snippet());
            }
            if (mk.tapHandler() != null) {
                opts.onClick(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        mk.tapHandler().call();
                    }
                });
            }
            placed.add(view.addMarker(opts));
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double w = constraints.hasBoundedWidth() ? constraints.maxWidth() : Dp.px(DEFAULT_SIZE_LP);
        double h = constraints.hasBoundedHeight() ? constraints.maxHeight() : Dp.px(DEFAULT_SIZE_LP);
        return constraints.constrain(new Size(w, h));
    }
}
