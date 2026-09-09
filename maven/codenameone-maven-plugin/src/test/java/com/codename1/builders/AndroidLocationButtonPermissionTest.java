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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which class references earn an application {@code USE_LOCATION_BUTTON}.
 *
 * <p>The permission is what the platform requires before it will render the
 * system location button at all, and there is no runtime way to acquire it --
 * so a missed reference is a control that silently never appears. The opposite
 * error matters too: every existing location app references
 * {@code com/codename1/location/...} and none of them should start shipping a
 * permission they do not use.</p>
 */
class AndroidLocationButtonPermissionTest {

    @Test
    void theLocationButtonEarnsThePermission() {
        assertTrue(AndroidGradleBuilder.isLocationButtonClass(
                "com/codename1/location/LocationButton"));
    }

    /** An anonymous listener written inside the component is still the component. */
    /**
     * The scan sets gpsPermission for anything under com/codename1/location,
     * and the manifest block that generates ACCESS_FINE_LOCATION -- including
     * the exclusive flag and its compile-SDK validation -- sits behind that
     * flag. So the button earning the restriction depends on the button also
     * living under that prefix. It does, and this pins it: moving
     * LocationButton to another package would leave a forced
     * android.locationButton.exclusive=true generating no declaration at all,
     * which is the one way review's "the outer gate bypasses the validator"
     * reading could become true.
     */
    @Test
    void theButtonSitsUnderThePrefixThatEarnsTheLocationPermission() {
        String[] buttonClasses = {
            "com/codename1/location/LocationButton",
            "com/codename1/location/LocationButton$1",
        };
        for (int iter = 0; iter < buttonClasses.length; iter++) {
            assertTrue(AndroidGradleBuilder.isLocationButtonClass(buttonClasses[iter]),
                    buttonClasses[iter] + " should be the button");
            assertTrue(buttonClasses[iter].indexOf("com/codename1/location") == 0,
                    buttonClasses[iter] + " must keep setting gpsPermission");
        }
    }

    @Test
    void aNestedClassOfTheButtonEarnsIt() {
        assertTrue(AndroidGradleBuilder.isLocationButtonClass(
                "com/codename1/location/LocationButton$1"));
    }

    /**
     * The rest of the location package is the ordinary location API, which
     * needs ACCESS_FINE_LOCATION and not this.
     */
    @Test
    void ordinaryLocationApisDoNotEarnIt() {
        assertFalse(AndroidGradleBuilder.isLocationButtonClass(
                "com/codename1/location/LocationManager"));
        assertFalse(AndroidGradleBuilder.isLocationButtonClass(
                "com/codename1/location/Location"));
        assertFalse(AndroidGradleBuilder.isLocationButtonClass(
                "com/codename1/location/GeofenceManager"));
        assertFalse(AndroidGradleBuilder.isLocationButtonClass(null));
    }

    /**
     * A prefix match would have caught this one. LocationButtonThing is not
     * ours and does not name the component.
     */
    @Test
    void aLongerNameStartingTheSameWayDoesNotEarnIt() {
        assertFalse(AndroidGradleBuilder.isLocationButtonClass(
                "com/codename1/location/LocationButtonThing"));
    }

    /** The fragment names the permission the platform documents, and nothing else. */
    @Test
    void theFragmentIsTheDocumentedPermission() {
        assertTrue(AndroidGradleBuilder.LOCATION_BUTTON_PERMISSION.contains(
                "android.permission.USE_LOCATION_BUTTON"));
        assertFalse(AndroidGradleBuilder.LOCATION_BUTTON_PERMISSION.contains(
                "onlyForLocationButton"),
                "the exclusive flag rides on ACCESS_FINE_LOCATION, not on this one");
    }

    /**
     * The button's own path does not count as ordinary precise-location use.
     * The listener and the value it carries are part of using the button, so an
     * application that only does that still qualifies for the restriction.
     */
    @Test
    void theButtonsOwnPathIsNotOrdinaryUse() {
        assertFalse(AndroidGradleBuilder.needsOrdinaryPreciseLocation(
                "com/codename1/location/LocationButton"));
        assertFalse(AndroidGradleBuilder.needsOrdinaryPreciseLocation(
                "com/codename1/location/LocationButton$1"));
        assertFalse(AndroidGradleBuilder.needsOrdinaryPreciseLocation(
                "com/codename1/location/LocationSharedListener"));
        assertFalse(AndroidGradleBuilder.needsOrdinaryPreciseLocation(
                "com/codename1/location/Location"));
        assertFalse(AndroidGradleBuilder.needsOrdinaryPreciseLocation(null));
        assertFalse(AndroidGradleBuilder.needsOrdinaryPreciseLocation(
                "com/codename1/ui/Form"));
    }

    /** Everything else that locates the user rules the restriction out. */
    @Test
    void trackingNavigatingAndGeofencingRuleItOut() {
        assertTrue(AndroidGradleBuilder.needsOrdinaryPreciseLocation(
                "com/codename1/location/LocationManager"));
        assertTrue(AndroidGradleBuilder.needsOrdinaryPreciseLocation(
                "com/codename1/location/LocationListener"));
        assertTrue(AndroidGradleBuilder.needsOrdinaryPreciseLocation(
                "com/codename1/location/LocationRequest"));
        assertTrue(AndroidGradleBuilder.needsOrdinaryPreciseLocation(
                "com/codename1/location/GeofenceManager"));
        assertTrue(AndroidGradleBuilder.needsOrdinaryPreciseLocation(
                "com/codename1/maps/MapComponent"));
    }

    /**
     * A class added to the location package later must read as ordinary use.
     * The other default would quietly take precise location away from an
     * application that needs it, and that failure does not announce itself.
     */
    @Test
    void anUnrecognisedLocationClassCountsAsOrdinaryUse() {
        assertTrue(AndroidGradleBuilder.needsOrdinaryPreciseLocation(
                "com/codename1/location/SomethingAddedLater"));
    }

    /** The button alone earns the restriction; anything else takes it away. */
    @Test
    void theRestrictionIsInferredFromButtonOnlyUse() {
        assertTrue(AndroidGradleBuilder.wantsExclusiveLocation("auto", true, false));
        assertFalse(AndroidGradleBuilder.wantsExclusiveLocation("auto", true, true));
        assertFalse(AndroidGradleBuilder.wantsExclusiveLocation("auto", false, false));
        assertFalse(AndroidGradleBuilder.wantsExclusiveLocation("auto", false, true));
    }

    /** The hint overrides the inference in both directions. */
    @Test
    void theHintOverridesTheInference() {
        assertFalse(AndroidGradleBuilder.wantsExclusiveLocation("false", true, false));
        assertTrue(AndroidGradleBuilder.wantsExclusiveLocation("true", false, true));
    }

    /**
     * onlyForLocationButton is an API 37 enum value and AAPT resolves manifest
     * enum values against the COMPILE SDK. Below 37 it is a resource-linking
     * failure, measured:
     *
     * <pre>AAPT: error: 'onlyForLocationButton' is incompatible with attribute
     * usesPermissionFlags (attr) flags [neverForLocation=65536]</pre>
     *
     * so the build must be able to decline to emit it.
     */
    @Test
    void theRestrictionNeedsACompileSdkThatUnderstandsIt() {
        assertFalse(AndroidGradleBuilder.compileSdkSupportsExclusiveLocation(35));
        assertFalse(AndroidGradleBuilder.compileSdkSupportsExclusiveLocation(36));
        assertTrue(AndroidGradleBuilder.compileSdkSupportsExclusiveLocation(37));
        assertTrue(AndroidGradleBuilder.compileSdkSupportsExclusiveLocation(38));
    }

    /**
     * An explicit restriction the compile SDK cannot express has to stop the
     * build. Executor.error() only writes to the logger and returns, so
     * reporting it that way would leave the build green while shipping an
     * application without the privacy restriction its developer asked for -- the
     * silent failure moved from the manifest into the build log. The message
     * has to name the level and the way out.
     */
    @Test
    void theExplicitRestrictionFailureNamesTheLevelAndTheFix() {
        String message = AndroidGradleBuilder.requireExclusiveCompileSdkMessage(36);
        assertTrue(message.contains("36"), "names the level this build has");
        assertTrue(message.contains("37"), "names the level it needs");
        assertTrue(message.contains("android.locationButton.exclusive"),
                "names the hint that caused it");
        assertTrue(message.contains("onlyForLocationButton"),
                "names the value AAPT rejects");
    }

    /** Only the restrictive declaration carries the flag. */
    @Test
    void theDeclarationsDifferOnlyInTheFlag() {
        assertFalse(AndroidGradleBuilder.FINE_LOCATION_PERMISSION.contains(
                "usesPermissionFlags"));
        assertTrue(AndroidGradleBuilder.FINE_LOCATION_PERMISSION_EXCLUSIVE.contains(
                "android:usesPermissionFlags=\"onlyForLocationButton\""));
        assertTrue(AndroidGradleBuilder.FINE_LOCATION_PERMISSION_EXCLUSIVE.contains(
                "android.permission.ACCESS_FINE_LOCATION"));
    }

    /**
     * A longer name starting the same way is not the button's listener, and a
     * prefix match would have said it was.
     */
    @Test
    void aLongerNameIsNotTheButtonsPath() {
        assertTrue(AndroidGradleBuilder.needsOrdinaryPreciseLocation(
                "com/codename1/location/LocationSharedListenerRegistry"));
        assertTrue(AndroidGradleBuilder.needsOrdinaryPreciseLocation(
                "com/codename1/location/LocationTracker"));
    }
}
