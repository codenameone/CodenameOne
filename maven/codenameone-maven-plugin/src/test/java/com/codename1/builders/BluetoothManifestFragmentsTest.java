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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the Android 12 permission-split fragments injected for the
 * {@code com.codename1.bluetooth} API, including the quote-delimited
 * duplicate suppression that protects projects migrating from the old BLE
 * cn1lib (whose merged {@code android.xpermissions} already declares the
 * legacy permissions).
 */
class BluetoothManifestFragmentsTest {

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
    void scanOnlyOnModernTarget() {
        String out = BluetoothManifestFragments.inject("", true, false, false,
                false, true, false, 34);
        assertTrue(out.contains(
                "android:name=\"android.permission.BLUETOOTH_SCAN\" "
                        + "android:usesPermissionFlags=\"neverForLocation\""));
        assertTrue(out.contains(
                "android:name=\"android.permission.BLUETOOTH\" "
                        + "android:maxSdkVersion=\"30\""));
        assertTrue(out.contains(
                "android:name=\"android.permission.BLUETOOTH_ADMIN\" "
                        + "android:maxSdkVersion=\"30\""));
        assertTrue(out.contains(
                "android:name=\"android.permission.ACCESS_FINE_LOCATION\" "
                        + "android:maxSdkVersion=\"30\""));
        assertFalse(out.contains("BLUETOOTH_CONNECT"));
        assertFalse(out.contains("BLUETOOTH_ADVERTISE"));
        assertTrue(out.contains(
                "android:name=\"android.hardware.bluetooth_le\" "
                        + "android:required=\"false\""));
        assertFalse(out.contains("\"android.hardware.bluetooth\""));
    }

    @Test
    void beaconAppsKeepLocationUncapped() {
        String out = BluetoothManifestFragments.inject("", true, false, false,
                false, false, false, 34);
        // no neverForLocation flag on SCAN
        assertTrue(out.contains(
                "android:name=\"android.permission.BLUETOOTH_SCAN\" />"));
        // location permission stays valid on Android 12+
        assertTrue(out.contains(
                "android:name=\"android.permission.ACCESS_FINE_LOCATION\" />"));
        assertFalse(out.contains(
                "ACCESS_FINE_LOCATION\" android:maxSdkVersion"));
    }

    @Test
    void connectOnlyGetsConnectAndBase() {
        String out = BluetoothManifestFragments.inject("", false, true, false,
                false, true, false, 34);
        assertTrue(out.contains("android.permission.BLUETOOTH_CONNECT"));
        assertTrue(out.contains("\"android.permission.BLUETOOTH\""));
        assertFalse(out.contains("BLUETOOTH_SCAN"));
        assertFalse(out.contains("BLUETOOTH_ADVERTISE"));
        assertFalse(out.contains("ACCESS_FINE_LOCATION"));
    }

    @Test
    void peripheralGetsAdvertiseAndConnect() {
        String out = BluetoothManifestFragments.inject("", false, false, true,
                false, true, false, 34);
        assertTrue(out.contains("android.permission.BLUETOOTH_ADVERTISE"));
        assertTrue(out.contains("android.permission.BLUETOOTH_CONNECT"));
        assertTrue(out.contains(
                "android:name=\"android.permission.BLUETOOTH_ADMIN\" "
                        + "android:maxSdkVersion=\"30\""));
    }

    @Test
    void classicAddsClassicFeature() {
        String out = BluetoothManifestFragments.inject("", true, true, false,
                true, true, false, 34);
        assertTrue(out.contains(
                "android:name=\"android.hardware.bluetooth\" "
                        + "android:required=\"false\""));
        assertTrue(out.contains(
                "android:name=\"android.hardware.bluetooth_le\""));
    }

    @Test
    void requiredHintFlipsBleFeature() {
        String out = BluetoothManifestFragments.inject("", true, true, false,
                false, true, true, 34);
        assertTrue(out.contains(
                "android:name=\"android.hardware.bluetooth_le\" "
                        + "android:required=\"true\""));
    }

    @Test
    void legacyTargetSkipsModernPermissionsAndCaps() {
        String out = BluetoothManifestFragments.inject("", true, true, true,
                false, true, false, 30);
        assertFalse(out.contains("BLUETOOTH_SCAN"));
        assertFalse(out.contains("BLUETOOTH_CONNECT"));
        assertFalse(out.contains("BLUETOOTH_ADVERTISE"));
        assertFalse(out.contains("maxSdkVersion"));
        assertTrue(out.contains(
                "android:name=\"android.permission.BLUETOOTH\" />"));
        assertTrue(out.contains(
                "android:name=\"android.permission.ACCESS_FINE_LOCATION\" />"));
    }

    @Test
    void legacyPermissionFromOldCn1libDoesNotSuppressModernOnes() {
        // The old BLE cn1lib merges these via android.xpermissions. The
        // substring trap: "android.permission.BLUETOOTH" is a prefix of
        // "android.permission.BLUETOOTH_SCAN" -- the quote-delimited check
        // must still inject SCAN/CONNECT while skipping the duplicates.
        String existing =
                "    <uses-permission android:name=\"android.permission.BLUETOOTH\" />\n"
              + "    <uses-permission android:name=\"android.permission.BLUETOOTH_ADMIN\" />\n"
              + "    <uses-permission android:name=\"android.permission.ACCESS_COARSE_LOCATION\" />\n";
        String out = BluetoothManifestFragments.inject(existing, true, true,
                false, false, true, false, 34);
        assertEquals(1, count(out, "\"android.permission.BLUETOOTH\""));
        assertEquals(1, count(out, "\"android.permission.BLUETOOTH_ADMIN\""));
        assertTrue(out.contains("android.permission.BLUETOOTH_SCAN"));
        assertTrue(out.contains("android.permission.BLUETOOTH_CONNECT"));
        assertTrue(out.contains("android.permission.ACCESS_FINE_LOCATION"));
    }

    @Test
    void modernPermissionsDoNotSuppressLegacyBase() {
        // Reverse direction of the substring trap: a user-declared
        // BLUETOOTH_SCAN must not suppress the legacy BLUETOOTH entry.
        String existing =
                "    <uses-permission android:name=\"android.permission.BLUETOOTH_SCAN\" />\n";
        String out = BluetoothManifestFragments.inject(existing, true, false,
                false, false, true, false, 34);
        assertEquals(1, count(out, "\"android.permission.BLUETOOTH_SCAN\""));
        assertTrue(out.contains(
                "android:name=\"android.permission.BLUETOOTH\" "
                        + "android:maxSdkVersion=\"30\""));
    }

    @Test
    void injectionIsIdempotent() {
        String once = BluetoothManifestFragments.inject("", true, true, true,
                true, true, false, 34);
        String twice = BluetoothManifestFragments.inject(once, true, true,
                true, true, true, false, 34);
        assertEquals(once, twice);
    }
}
