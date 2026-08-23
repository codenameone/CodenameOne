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
                false, "", 34);
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
    void uwbRangingIsDeclaredWhateverTheTargetSdk() {
        // targetSdkVersion picks compatibility behaviours, not the device. An
        // app targeting 30 still runs on an Android 12 phone with a UWB radio,
        // and there the runtime request fails unless the manifest declares
        // this. Older devices ignore a permission they do not know.
        String legacy = NearbyManifestFragments.inject("", true, false, false,
                false, "", 30);
        assertTrue(legacy.contains("android.permission.UWB_RANGING"));
        String modern = NearbyManifestFragments.inject("", true, false, false,
                false, "", 34);
        assertTrue(modern.contains("android.permission.UWB_RANGING"));
        // The feature stays optional, because that is what keeps the app
        // installable on a device without the radio.
        assertTrue(legacy.contains("android.hardware.uwb"));
    }

    @Test
    void transportCarriesTheAndroid12SplitWithTheLegacyPairCapped() {
        String out = NearbyManifestFragments.inject("", false, true, false,
                false, "", 34);
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
                false, "", 34);
        assertTrue(modern.contains(
                "android:name=\"android.permission.NEARBY_WIFI_DEVICES\""
                + " android:usesPermissionFlags=\"neverForLocation\""));
        assertTrue(modern.contains(
                "android:name=\"android.permission.ACCESS_FINE_LOCATION\""
                + " android:maxSdkVersion=\"32\""));

        // Below a target of 33 the CAP is what changes, not the
        // declaration. Nearby Connections refuses to start without a location
        // grant there, and the app runs under its target's rules whatever
        // device it is on -- so location must not be capped. The permission
        // is still declared, because the app may run on a 13 device and the
        // runtime asks for what THAT device requires.
        String older = NearbyManifestFragments.inject("", false, true, false,
                false, "", 31);
        assertTrue(older.contains("NEARBY_WIFI_DEVICES"));
        assertTrue(older.contains(
                "android:name=\"android.permission.ACCESS_FINE_LOCATION\" />"));
    }

    @Test
    void transportOnALegacyTargetKeepsTheLegacyPairUncapped() {
        String out = NearbyManifestFragments.inject("", false, true, false,
                false, "", 30);
        assertTrue(out.contains(
                "android:name=\"android.permission.BLUETOOTH\" />"));
    }

    @Test
    void theSplitPermissionsAreDeclaredEvenForALegacyTarget() {
        // A permission is requested at RUNTIME according to the level the
        // app is actually running under, and requesting one the manifest
        // does not declare is refused instantly with no prompt -- so a
        // target-30 app on Android 12 could not ask for these at all. A
        // device below 31 ignores permissions it has never heard of.
        String out = NearbyManifestFragments.inject("", false, true, false,
                false, "", 30);
        assertTrue(out.contains("BLUETOOTH_SCAN"), out);
        assertTrue(out.contains("BLUETOOTH_ADVERTISE"), out);
        assertTrue(out.contains("BLUETOOTH_CONNECT"), out);
        assertTrue(out.contains("NEARBY_WIFI_DEVICES"), out);
        // The legacy pair stays uncapped for a legacy target: that is what
        // Android 12 actually honours for such an app.
        assertTrue(out.contains(
                "android:name=\"android.permission.BLUETOOTH\" />"), out);
    }

    @Test
    void everyProfileTheApiExposesHasItsOwnPermission() {
        // AndroidNearbyBackend forwards COMPUTER on API 33 and GLASSES on
        // 34, and without the matching permission the platform rejects the
        // association before the chooser opens -- which looks to the user
        // like nothing happened at all.
        String watch = NearbyManifestFragments.inject("", false, false, true,
                false, "watch", 34);
        assertTrue(watch.contains("REQUEST_COMPANION_PROFILE_WATCH"), watch);
        assertFalse(watch.contains("REQUEST_COMPANION_PROFILE_COMPUTER"),
                watch);

        String computer = NearbyManifestFragments.inject("", false, false,
                true, false, "computer", 34);
        assertTrue(computer.contains("REQUEST_COMPANION_PROFILE_COMPUTER"),
                computer);

        String glasses = NearbyManifestFragments.inject("", false, false, true,
                false, "glasses", 34);
        assertTrue(glasses.contains("REQUEST_COMPANION_PROFILE_GLASSES"),
                glasses);

        String both = NearbyManifestFragments.inject("", false, false, true,
                false, "watch,glasses", 34);
        assertTrue(both.contains("REQUEST_COMPANION_PROFILE_WATCH"), both);
        assertTrue(both.contains("REQUEST_COMPANION_PROFILE_GLASSES"), both);
    }

    @Test
    void aProfileNameIsMatchedWholeRatherThanAsASubstring() {
        assertFalse(NearbyManifestFragments.hasProfile("watchdog", "watch"));
        assertTrue(NearbyManifestFragments.hasProfile(" Watch , glasses",
                "watch"));
        assertFalse(NearbyManifestFragments.hasProfile(null, "watch"));
        assertFalse(NearbyManifestFragments.hasProfile("", "watch"));
    }

    @Test
    void associatingWithoutWatchingCostsNoBackgroundPermission() {
        String out = NearbyManifestFragments.inject("", false, false, true,
                false, "", 34);
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
                true, "", 34);
        assertTrue(out.contains(
                "android.permission.REQUEST_COMPANION_RUN_IN_BACKGROUND"));
        assertTrue(out.contains(
                "android.permission.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND"));
        assertTrue(out.contains("android.permission.REQUEST_COMPANION"
                + "_START_FOREGROUND_SERVICES_FROM_BACKGROUND"));
    }

    @Test
    void theForegroundServiceExemptionArrivesWithApi31NotApi33() {
        // It is a companion permission since API 31. Gating it on 33 left an
        // Android 12/12L app unable to start a foreground service when the
        // platform woke its CompanionDeviceService, which is what observing
        // presence is for.
        String twelve = NearbyManifestFragments.inject("", false, false, true,
                true, "", 31);
        assertTrue(twelve.contains("android.permission.REQUEST_COMPANION"
                + "_START_FOREGROUND_SERVICES_FROM_BACKGROUND"));
        // Still absent below the API that has it.
        String eleven = NearbyManifestFragments.inject("", false, false, true,
                true, "", 30);
        assertFalse(eleven.contains(
                "REQUEST_COMPANION_START_FOREGROUND_SERVICES"));
    }

    @Test
    void theWatchProfilePermissionIsOptInButNotTargetGated() {
        assertFalse(NearbyManifestFragments.inject("", false, false, true,
                false, "", 34)
                .contains("REQUEST_COMPANION_PROFILE_WATCH"));
        assertTrue(NearbyManifestFragments.inject("", false, false, true,
                false, "watch", 34)
                .contains("android.permission.REQUEST_COMPANION_PROFILE_WATCH"));
        // Selecting DEVICE_PROFILE_WATCH needs this on an Android 12 device
        // whatever the app targets, so a legacy target must still declare it.
        assertTrue(NearbyManifestFragments.inject("", false, false, true,
                false, "watch", 30)
                .contains("android.permission.REQUEST_COMPANION_PROFILE_WATCH"));
    }

    @Test
    void nothingIsDeclaredTwiceWhenBluetoothRanFirst() {
        // The realistic collision: an app that uses com.codename1.bluetooth
        // AND com.codename1.nearby.transport runs both injectors over one
        // string, and both want the same six Bluetooth permissions.
        String afterBluetooth = BluetoothManifestFragments.inject("", true,
                true, true, false, true, false, 34);
        String out = NearbyManifestFragments.inject(afterBluetooth, false,
                true, false, false, "", 34);
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
                false, "", 34);
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
                false, "", 34);
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
                false, "", 34);
        assertTrue(out.contains("android.permission.UWB_RANGING"));
    }

    @Test
    void transportWidensTheLocationCapBluetoothAlreadyDeclared() {
        // BluetoothManifestFragments runs first and, for a scanning app with
        // the default neverForLocation, caps this at 30. Nearby Connections
        // needs it through 32, and a plain duplicate-suppressing add left the
        // 30 in place -- so transport had no location grant at all on Android
        // 12 and 12L, where the API refuses to start without one.
        String bluetooth = BluetoothManifestFragments.inject("", true, false,
                false, false, true, false, 34);
        assertTrue(bluetooth.contains("ACCESS_FINE_LOCATION"),
                "precondition: bluetooth declares the permission");
        assertTrue(bluetooth.contains("android:maxSdkVersion=\"30\""),
                "precondition: bluetooth caps it at 30");

        String out = NearbyManifestFragments.inject(bluetooth, false, true,
                false, false, "", 34);
        int at = out.indexOf("ACCESS_FINE_LOCATION");
        int elementEnd = out.indexOf('>', at);
        String element = out.substring(out.lastIndexOf('<', at), elementEnd);
        assertTrue(element.contains("android:maxSdkVersion=\"32\""),
                "the cap should reach 32: " + element);
        // Widened, never duplicated: two declarations of one permission is
        // not a manifest Android accepts predictably.
        assertEquals(out.indexOf("ACCESS_FINE_LOCATION"),
                out.lastIndexOf("ACCESS_FINE_LOCATION"));
    }

    @Test
    void transportBelowTiramisuRemovesTheCapAltogether() {
        // With no NEARBY_WIFI_DEVICES to fall back on, the location grant has
        // to hold at every level the app runs at.
        String bluetooth = BluetoothManifestFragments.inject("", true, false,
                false, false, true, false, 32);
        String out = NearbyManifestFragments.inject(bluetooth, false, true,
                false, false, "", 32);
        int at = out.indexOf("ACCESS_FINE_LOCATION");
        String element = out.substring(out.lastIndexOf('<', at),
                out.indexOf('>', at));
        assertFalse(element.contains("maxSdkVersion"),
                "the cap should be gone: " + element);
    }

    @Test
    void coarseLocationIsDeclaredAlongsideFineWithTheSameReach() {
        // From Android 12 the two are requested together -- one dialog with a
        // precise/approximate choice -- and a request for fine alone is
        // refused when coarse is not declared, so the grant Nearby
        // Connections needs on 12 and 12L never arrived.
        String out = NearbyManifestFragments.inject("", false, true, false,
                false, "", 34);
        assertTrue(out.contains("android:name=\"android.permission"
                + ".ACCESS_COARSE_LOCATION\" android:maxSdkVersion=\"32\""),
                out);
        assertTrue(out.contains("android:name=\"android.permission"
                + ".ACCESS_FINE_LOCATION\" android:maxSdkVersion=\"32\""),
                out);
    }

    @Test
    void coarseLocationIsUncappedBelowATiramisuTarget() {
        String out = NearbyManifestFragments.inject("", false, true, false,
                false, "", 31);
        assertTrue(out.contains("android:name=\"android.permission"
                + ".ACCESS_COARSE_LOCATION\" />"), out);
    }

    @Test
    void aCapThatAlreadyReachesFarEnoughIsLeftAlone() {
        String seeded = "    <uses-permission android:name=\"android.permission"
                + ".ACCESS_FINE_LOCATION\" android:maxSdkVersion=\"33\" />\n";
        String out = NearbyManifestFragments.inject(seeded, false, true, false,
                false, "", 34);
        assertTrue(out.contains("android:maxSdkVersion=\"33\""),
                "a wider cap is not narrowed: " + out);
    }

    @Test
    void usingNoneOfItChangesNothing() {
        String seeded = "    <uses-permission android:name=\"x\" />\n";
        assertEquals(seeded, NearbyManifestFragments.inject(seeded, false,
                false, false, false, "", 34));
    }
}
