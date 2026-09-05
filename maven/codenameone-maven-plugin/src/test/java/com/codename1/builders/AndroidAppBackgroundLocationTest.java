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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code appBackgroundLocation} refuses a build, so what sets it has to be
 * exact.
 *
 * <p>The legacy {@code backgroundLocationPermission} beside it is set by a
 * prefix test on the class and a substring test on the method. That looseness
 * has always been there and always been harmless, because the worst it bought
 * was a permission the app did not need. The moment the same predicate fed the
 * exclusivity check, a historical over-grant became a build the developer
 * cannot make -- and cannot diagnose, because the class that tripped it is
 * their own.</p>
 */
class AndroidAppBackgroundLocationTest {

    /** An application class, i.e. not one the filter recognises as ours. */
    private static final String APP = "com/example/MyForm";

    @Test
    void theApplicationsOwnGeofencingCounts() {
        assertTrue(AndroidGradleBuilder.appOwnsBackgroundLocation(
                "com/codename1/location/LocationManager", "addGeoFencing",
                APP));
        assertTrue(AndroidGradleBuilder.appOwnsBackgroundLocation(
                "com/codename1/location/LocationManager",
                "setBackgroundLocationListener", APP));
        assertTrue(AndroidGradleBuilder.appOwnsGeofencing(
                "com/codename1/location/GeofenceManager", APP));
        assertTrue(AndroidGradleBuilder.appOwnsGeofencing(
                "com/codename1/location/Geofence", APP));
    }

    @Test
    void aNameThatMerelyStartsOrContainsTheRightThingDoesNot() {
        // The shape the legacy tests accept: a class whose name STARTS WITH
        // ours and a method whose name CONTAINS one of the markers.
        assertFalse(AndroidGradleBuilder.appOwnsBackgroundLocation(
                "com/codename1/location/LocationManagerHelper",
                "addGeoFencingLater", APP),
                "a helper of the application's own must not refuse the build");
        assertFalse(AndroidGradleBuilder.appOwnsBackgroundLocation(
                "com/codename1/location/LocationManager", "addGeoFencingLater",
                APP),
                "the right class and a longer method is still not the call");
        assertFalse(AndroidGradleBuilder.appOwnsGeofencing(
                "com/example/GeofenceHelper", APP));
        assertFalse(AndroidGradleBuilder.appOwnsGeofencing(
                "com/codename1/location/GeofenceManagerFactory", APP),
                "a longer name that starts the same way is a different class");
    }

    @Test
    void theFrameworksOwnCallsAreNotTheApplications() {
        // dummyClassesDir is the application MERGED WITH THE FRAMEWORK, so
        // GeofenceManager's own call to addGeoFencing is in every build ever
        // made. Reading it as the app's refused the hint for the button-only
        // app it exists for.
        assertFalse(AndroidGradleBuilder.appOwnsBackgroundLocation(
                "com/codename1/location/LocationManager", "addGeoFencing",
                "com/codename1/location/GeofenceManager"));
        assertFalse(AndroidGradleBuilder.appOwnsGeofencing(
                "com/codename1/location/GeofenceManager",
                "com/codename1/location/LocationButton$3"),
                "an inner class of the framework is the framework");
    }

    @Test
    void theFrameworksOwnNestedGeofenceClassesStillCount() {
        // Nested classes of the geofencing API are the API; only the class
        // DOING the referencing decides whose reference it is.
        assertTrue(AndroidGradleBuilder.appOwnsGeofencing(
                "com/codename1/location/Geofence$Builder", APP));
        assertTrue(AndroidGradleBuilder.appOwnsGeofencing(
                "com/codename1/location/GeofenceManager$Listener", APP));
    }
}
