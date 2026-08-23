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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the manifest fragments injected for the
 * {@code com.codename1.nearby} packages.
 *
 * <p>Three properties matter here. Each package pays only for itself, because
 * the package prefix is the whole opt-in. The version-conditional permissions
 * appear on the right side of their boundary, because that is what the flat
 * catalog table could not express and the reason this class exists. And
 * nothing is declared twice when an app also uses
 * {@code com.codename1.bluetooth}, whose injector runs over the same
 * string.</p>
 */
class NearbyManifestFragmentsTest {

    private static int count(String haystack, String needle) {
        int count = 0;
        int idx = haystack.indexOf(needle);
        while (idx >= 0) {
            count++;
            idx = haystack.indexOf(needle, idx + needle.length());
        }
        return count;
    }

    @Test
    void rangingPaysForRangingOnly() {
        String out = NearbyManifestFragments.inject("", true, false, false,
                false, false, 34);
        assertTrue(out.contains("android.permission.UWB_RANGING"));
        assertTrue(out.contains("android:name=\"android.hardware.uwb\""
                + " android:required=\"false\""));
        // Nothing from the other two packages.
        assertFalse(out.contains("BLUETOOTH"));
        assertFalse(out.contains("NEARBY_WIFI_DEVICES"));
        assertFalse(out.contains("companion_device_setup"));
        assertFalse(out.contains("REQUEST_COMPANION"));
    }

    @Test
    void uwbRangingIsNotDeclaredBelowTheApiThatHasIt() {
        // The permission arrives in API 31. Declaring it on an older target
        // is harmless and noisy, and a store review asks about it.
        String out = NearbyManifestFragments.inject("", true, false, false,
                false, false, 30);
        assertFalse(out.contains("android.permission.UWB_RANGING"));
        // The feature is still declared, because that is what keeps the app
        // installable on a device without the radio.
        assertTrue(out.contains("android.hardware.uwb"));
    }

    @Test
    void transportCarriesTheAndroid12SplitWithTheLegacyPairCapped() {
        String out = NearbyManifestFragments.inject("", false, true, false,
                false, false, 34);
        assertTrue(out.contains("android:name=\"android.permission.BLUETOOTH\""
                + " android:maxSdkVersion=\"30\""));
        assertTrue(out.contains(
                "android:name=\"android.permission.BLUETOOTH_ADMIN\""
                + " android:maxSdkVersion=\"30\""));
        assertTrue(out.contains(
                "android:name=\"android.permission.BLUETOOTH_SCAN\""
                + " android:usesPermissionFlags=\"neverForLocation\""));
        assertTrue(out.contains("android.permission.BLUETOOTH_ADVERTISE"));
        assertTrue(out.contains("android.permission.BLUETOOTH_CONNECT"));
        assertTrue(out.contains("android.permission.ACCESS_WIFI_STATE"));
        assertTrue(out.contains("android.permission.CHANGE_WIFI_STATE"));
    }

    @Test
    void transportStopsAskingForLocationOnceNearbyWifiExists() {
        String modern = NearbyManifestFragments.inject("", false, true, false,
                false, false, 34);
        assertTrue(modern.contains(
                "android:name=\"android.permission.NEARBY_WIFI_DEVICES\""
                + " android:usesPermissionFlags=\"neverForLocation\""));
        assertTrue(modern.contains(
                "android:name=\"android.permission.ACCESS_FINE_LOCATION\""
                + " android:maxSdkVersion=\"32\""));

        // Below 33 there is no NEARBY_WIFI_DEVICES, and Nearby Connections
        // genuinely refuses to start without a location grant -- so it must
        // NOT be capped there.
        String older = NearbyManifestFragments.inject("", false, true, false,
                false, false, 31);
        assertFalse(older.contains("NEARBY_WIFI_DEVICES"));
        assertTrue(older.contains(
                "android:name=\"android.permission.ACCESS_FINE_LOCATION\" />"));
    }

    @Test
    void transportOnALegacyTargetKeepsTheLegacyPairUncapped() {
        String out = NearbyManifestFragments.inject("", false, true, false,
                false, false, 30);
        assertTrue(out.contains(
                "android:name=\"android.permission.BLUETOOTH\" />"));
        assertFalse(out.contains("BLUETOOTH_SCAN"));
        assertFalse(out.contains("BLUETOOTH_ADVERTISE"));
    }

    @Test
    void associatingWithoutWatchingCostsNoBackgroundPermission() {
        String out = NearbyManifestFragments.inject("", false, false, true,
                false, false, 34);
        assertTrue(out.contains("android.software.companion_device_setup"));
        // This is the point of tracking presence separately: background
        // privileges an app never uses are privileges a user is asked about
        // for nothing.
        assertFalse(out.contains("REQUEST_COMPANION_RUN_IN_BACKGROUND"));
        assertFalse(out.contains("REQUEST_COMPANION_USE_DATA_IN_BACKGROUND"));
        assertFalse(out.contains("REQUEST_COMPANION_PROFILE_WATCH"));
    }

    @Test
    void watchingEarnsTheBackgroundPermissions() {
        String out = NearbyManifestFragments.inject("", false, false, true,
                true, false, 34);
        assertTrue(out.contains(
                "android.permission.REQUEST_COMPANION_RUN_IN_BACKGROUND"));
        assertTrue(out.contains(
                "android.permission.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND"));
        assertTrue(out.contains("android.permission.REQUEST_COMPANION"
                + "_START_FOREGROUND_SERVICES_FROM_BACKGROUND"));
    }

    @Test
    void theWatchProfilePermissionIsOptInAndModernOnly() {
        assertFalse(NearbyManifestFragments.inject("", false, false, true,
                false, false, 34)
                .contains("REQUEST_COMPANION_PROFILE_WATCH"));
        assertTrue(NearbyManifestFragments.inject("", false, false, true,
                false, true, 34)
                .contains("android.permission.REQUEST_COMPANION_PROFILE_WATCH"));
        // The permission arrives with the profiles, in API 31.
        assertFalse(NearbyManifestFragments.inject("", false, false, true,
                false, true, 30)
                .contains("REQUEST_COMPANION_PROFILE_WATCH"));
    }

    @Test
    void nothingIsDeclaredTwiceWhenBluetoothRanFirst() {
        // The realistic collision: an app that uses com.codename1.bluetooth
        // AND com.codename1.nearby.transport runs both injectors over one
        // string, and both want the same six Bluetooth permissions.
        String afterBluetooth = BluetoothManifestFragments.inject("", true,
                true, true, false, true, false, 34);
        String out = NearbyManifestFragments.inject(afterBluetooth, false,
                true, false, false, false, 34);
        assertEquals(1, count(out,
                "android:name=\"android.permission.BLUETOOTH\""));
        assertEquals(1, count(out,
                "android:name=\"android.permission.BLUETOOTH_SCAN\""));
        assertEquals(1, count(out,
                "android:name=\"android.permission.BLUETOOTH_ADVERTISE\""));
        assertEquals(1, count(out,
                "android:name=\"android.permission.BLUETOOTH_CONNECT\""));
        assertEquals(1, count(out,
                "android:name=\"android.permission.ACCESS_FINE_LOCATION\""));
    }

    @Test
    void aQuotedTokenIsWhatSuppressesADuplicate() {
        // BLUETOOTH is a prefix of BLUETOOTH_SCAN. A substring check would
        // see the scan permission and wrongly skip the legacy one.
        String seeded = "    <uses-permission android:name="
                + "\"android.permission.BLUETOOTH_SCAN\" />\n";
        String out = NearbyManifestFragments.inject(seeded, false, true, false,
                false, false, 34);
        assertEquals(1, count(out,
                "android:name=\"android.permission.BLUETOOTH_SCAN\""));
        assertEquals(1, count(out,
                "android:name=\"android.permission.BLUETOOTH\""));
    }

    @Test
    void aUserDeclaredPermissionIsNotDuplicated() {
        String seeded = "    <uses-permission android:name="
                + "\"android.permission.UWB_RANGING\" />\n";
        String out = NearbyManifestFragments.inject(seeded, true, false, false,
                false, false, 34);
        assertEquals(1, count(out, "android.permission.UWB_RANGING"));
    }

    @Test
    void theServiceElementOnlyExistsForAnAppThatWatches() {
        assertEquals("", NearbyManifestFragments.presenceService(false));
        String service = NearbyManifestFragments.presenceService(true);
        assertTrue(service.contains("com.codename1.impl.android.nearby"
                + ".CN1CompanionDeviceService"));
        // Both are required for the platform to bind it at all.
        assertTrue(service.contains(
                "android:permission=\"android.permission"
                + ".BIND_COMPANION_DEVICE_SERVICE\""));
        assertTrue(service.contains(
                "<action android:name=\"android.companion"
                + ".CompanionDeviceService\" />"));
        assertTrue(service.contains("android:exported=\"true\""));
    }

    @Test
    void nullInputIsTreatedAsEmpty() {
        String out = NearbyManifestFragments.inject(null, true, false, false,
                false, false, 34);
        assertTrue(out.contains("android.permission.UWB_RANGING"));
    }

    @Test
    void usingNoneOfItChangesNothing() {
        String seeded = "    <uses-permission android:name=\"x\" />\n";
        assertEquals(seeded, NearbyManifestFragments.inject(seeded, false,
                false, false, false, false, 34));
    }
}
