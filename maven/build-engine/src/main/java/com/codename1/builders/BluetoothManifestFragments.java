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

/**
 * Builds the AndroidManifest permission/feature fragments injected when the
 * bytecode scanner detects usage of the {@code com.codename1.bluetooth} API.
 * Extracted into a pure static helper so the Android 12 permission-split
 * nuances (maxSdkVersion caps, {@code neverForLocation}) are unit-testable
 * and so the BuildDaemon copy of this class stays trivially diffable --
 * <b>keep this file in sync with
 * {@code com.codename1.build.daemon.BluetoothManifestFragments}</b>.
 *
 * <p>Duplicate suppression uses quote-delimited tokens
 * ({@code "android.permission.BLUETOOTH\""}) rather than plain substring
 * checks: {@code BLUETOOTH_SCAN} contains {@code BLUETOOTH}, so a loose
 * check would wrongly skip the legacy permission when the new one is
 * present (and vice versa a user-declared legacy permission must not
 * suppress the Android 12 ones).</p>
 */
final class BluetoothManifestFragments {

    private BluetoothManifestFragments() {
    }

    /**
     * Returns {@code xPermissions} with the Bluetooth fragments prepended.
     *
     * @param xPermissions      the current accumulated manifest fragment
     * @param scan              scanning/discovery API usage detected
     * @param connect           connection/GATT/RFCOMM/L2CAP usage detected
     * @param peripheral        peripheral-mode (advertise + GATT server)
     *                          usage detected
     * @param classic           classic (BR/EDR) API usage detected
     * @param neverForLocation  {@code android.bluetooth.neverForLocation}
     *                          hint (default true): declare that scan
     *                          results are never used to derive location,
     *                          capping the legacy location permission at
     *                          API 30. Beacon apps set the hint to false.
     * @param bleFeatureRequired {@code android.bluetooth.required} hint:
     *                          marks the BLE hardware feature required so
     *                          stores hide the app on devices without it
     * @param targetSdkVersion  the build's target SDK level; below 31 the
     *                          Android 12 permissions are not injected and
     *                          the legacy ones are left uncapped
     */
    static String inject(String xPermissions, boolean scan, boolean connect,
            boolean peripheral, boolean classic, boolean neverForLocation,
            boolean bleFeatureRequired, int targetSdkVersion) {
        String out = xPermissions == null ? "" : xPermissions;
        boolean modern = targetSdkVersion >= 31;
        String legacyCap = modern ? " android:maxSdkVersion=\"30\"" : "";

        // base: any com.codename1.bluetooth usage
        out = addPermission(out, "android.permission.BLUETOOTH", legacyCap);
        out = addFeature(out, "android.hardware.bluetooth_le",
                bleFeatureRequired);
        if (classic) {
            out = addFeature(out, "android.hardware.bluetooth", false);
        }
        if (scan) {
            out = addPermission(out, "android.permission.BLUETOOTH_ADMIN",
                    legacyCap);
            if (modern) {
                out = addPermission(out, "android.permission.BLUETOOTH_SCAN",
                        neverForLocation
                                ? " android:usesPermissionFlags=\"neverForLocation\""
                                : "");
            }
            // BLE scan results require a location grant up to API 30; when
            // the app derives location from beacons (neverForLocation=false)
            // the permission must stay uncapped so 12+ keeps granting it.
            out = addPermission(out, "android.permission.ACCESS_FINE_LOCATION",
                    modern && neverForLocation
                            ? " android:maxSdkVersion=\"30\"" : "");
        }
        if (connect || peripheral) {
            if (modern) {
                out = addPermission(out,
                        "android.permission.BLUETOOTH_CONNECT", "");
            }
        }
        if (peripheral) {
            out = addPermission(out, "android.permission.BLUETOOTH_ADMIN",
                    legacyCap);
            if (modern) {
                out = addPermission(out,
                        "android.permission.BLUETOOTH_ADVERTISE", "");
            }
        }
        return out;
    }

    private static String addPermission(String xPermissions, String name,
            String extraAttributes) {
        if (xPermissions.contains("\"" + name + "\"")) {
            return xPermissions;
        }
        return "    <uses-permission android:name=\"" + name + "\""
                + extraAttributes + " />\n" + xPermissions;
    }

    private static String addFeature(String xPermissions, String name,
            boolean required) {
        if (xPermissions.contains("\"" + name + "\"")) {
            return xPermissions;
        }
        return "    <uses-feature android:name=\"" + name
                + "\" android:required=\"" + required + "\" />\n"
                + xPermissions;
    }
}
