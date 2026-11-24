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
package com.codenameone.developerguide.snippets.generated;

import com.codename1.components.ToastBar;
import com.codename1.maps.MapView;
import com.codename1.maps.geocoding.GeocodeCallback;
import com.codename1.maps.geocoding.NominatimGeocoder;
import com.codename1.maps.geocoding.Place;
import java.util.List;

class MapsGeocodeSnippet {

    void run(final MapView map) {
        // tag::maps-geocode[]
        NominatimGeocoder geocoder = new NominatimGeocoder();
        geocoder.search("10 Downing Street, London", 5, new GeocodeCallback() {
            public void placesFound(List places) {
                if (!places.isEmpty()) {
                    map.moveCamera(((Place) places.get(0)).getLocation(), 17);
                }
            }
            public void geocodeFailed(String message, Throwable error) {
                ToastBar.showErrorMessage(message);
            }
        });
        // end::maps-geocode[]
    }
}
