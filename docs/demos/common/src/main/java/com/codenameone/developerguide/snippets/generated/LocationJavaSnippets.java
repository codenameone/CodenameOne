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
import java.util.*;
import com.codename1.util.AsyncResource;
import com.codename1.location.*;

class LocationJavaSnippets {

    Label label;
    Form form;

    void locationButton() {
        // tag::location-java-001[]
        LocationButton share = new LocationButton(LocationButton.TEXT_SHARE_PRECISE_LOCATION);
        share.addLocationSharedListener(loc -> {
            if (loc == null) {
                label.setText("Location not shared");
            } else {
                label.setText(loc.getLatitude() + ", " + loc.getLongitude());
            }
            label.getParent().revalidate();
        });
        form.add(share);
        // end::location-java-001[]
    }

    void beforeMigration() {
        // tag::location-java-002[]
        Button findNearby = new Button("Find shops near me");
        findNearby.addActionListener(e -> {
            Location loc = LocationManager.getLocationManager().getCurrentLocationSync();
            label.setText(loc == null ? "Location unavailable" : loc.getLatitude() + ", " + loc.getLongitude());
        });
        // end::location-java-002[]
    }

    void afterMigration() {
        // tag::location-java-003[]
        LocationButton findNearby = new LocationButton(LocationButton.TEXT_NEAR_MY_PRECISE_LOCATION);
        findNearby.addLocationSharedListener(loc ->
                label.setText(loc == null ? "Location unavailable" : loc.getLatitude() + ", " + loc.getLongitude()));
        // end::location-java-003[]
    }

    void persistentListener() {
        // tag::location-java-004[]
        LocationManager.getLocationManager().setLocationListener(new LocationListener() {
            public void locationUpdated(Location location) {
                // update the UI
            }

            public void providerStateChanged(int newState) {
                // handle status changes and errors
            }
        });
        // end::location-java-004[]
    }
}
