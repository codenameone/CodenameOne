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

/**
 * Builds the AndroidManifest permission, feature and service fragments
 * injected when the bytecode scanner detects usage of the
 * {@code com.codename1.nearby} packages.
 *
 * <p>Extracted into a pure static helper for the reasons
 * {@link BluetoothManifestFragments} gives: the version-conditional nuances
 * are unit-testable here, and the BuildDaemon copy of this class stays
 * trivially diffable -- <b>keep this file in sync with
 * {@code com.codename1.build.daemon.NearbyManifestFragments}</b>.</p>
 *
 * <p>Why any of this is here rather than in {@code PlatformFeatureCatalog}:
 * the catalog can name a permission but not qualify it. {@code UWB_RANGING}
 * exists only from API 31, the transport needs the Android 12 Bluetooth split
 * with {@code maxSdkVersion} caps, and {@code NEARBY_WIFI_DEVICES} needs
 * {@code usesPermissionFlags="neverForLocation"} from API 33. None of those
 * fit a flat list.</p>
 *
 * <p>Duplicate suppression uses quote-delimited tokens
 * ({@code "android.permission.BLUETOOTH\""}) rather than plain substring
 * checks, for the reason the Bluetooth version documents: {@code
 * BLUETOOTH_SCAN} contains {@code BLUETOOTH}, so a loose check would wrongly
 * skip the legacy permission when the new one is present. This matters more
 * here than there, because an app that uses both {@code
 * com.codename1.bluetooth} and {@code com.codename1.nearby.transport} runs
 * both injectors over the same string.</p>
 */
final class NearbyManifestFragments {

    /**
     * Bumped when the fragments change, so a build log names which version
     * produced a manifest.
     */
    static final int FRAGMENT_VERSION = 1;

    private NearbyManifestFragments() {
    }

    /**
     * Returns {@code xPermissions} with the nearby fragments prepended.
     *
     * @param xPermissions     the current accumulated manifest fragment
     * @param ranging          {@code com.codename1.nearby.ranging} usage
     *                         detected
     * @param transport        {@code com.codename1.nearby.transport} usage
     *                         detected
     * @param companion        {@code com.codename1.nearby.companion} usage
     *                         detected
     * @param presence         presence observation detected, which is what
     *                         earns the background and foreground-service
     *                         companion permissions
     * @param watchProfile     the app asks to associate a watch, which is the
     *                         one device profile with a permission of its own
     * @param targetSdkVersion the build's target SDK level
     * @return the fragment with the nearby entries prepended
     */
    static String inject(String xPermissions, boolean ranging,
            boolean transport, boolean companion, boolean presence,
            boolean watchProfile, int targetSdkVersion) {
        String out = xPermissions == null ? "" : xPermissions;
        boolean modern = targetSdkVersion >= 31;
        boolean tiramisu = targetSdkVersion >= 33;

        if (ranging) {
            // API 31 and later only. Declaring it below that is harmless but
            // noisy, and an unknown permission in a manifest is the kind of
            // thing a store review flags and a developer then has to explain.
            if (modern) {
                out = addPermission(out, "android.permission.UWB_RANGING", "");
            }
            out = addFeature(out, "android.hardware.uwb", false);
        }

        if (transport) {
            // Nearby Connections drives Bluetooth, BLE and Wi-Fi and needs
            // all of them. The legacy pair is capped at 30 because the
            // Android 12 split replaces them; the new trio only exists from
            // 31, so both halves are present and each is bounded.
            String legacyCap = modern ? " android:maxSdkVersion=\"30\"" : "";
            out = addPermission(out, "android.permission.BLUETOOTH",
                    legacyCap);
            out = addPermission(out, "android.permission.BLUETOOTH_ADMIN",
                    legacyCap);
            if (modern) {
                out = addPermission(out, "android.permission.BLUETOOTH_SCAN",
                        " android:usesPermissionFlags=\"neverForLocation\"");
                out = addPermission(out,
                        "android.permission.BLUETOOTH_ADVERTISE", "");
                out = addPermission(out, "android.permission.BLUETOOTH_CONNECT",
                        "");
            }
            out = addPermission(out, "android.permission.ACCESS_WIFI_STATE",
                    "");
            out = addPermission(out, "android.permission.CHANGE_WIFI_STATE",
                    "");
            if (tiramisu) {
                out = addPermission(out,
                        "android.permission.NEARBY_WIFI_DEVICES",
                        " android:usesPermissionFlags=\"neverForLocation\"");
            }
            // Nearby Connections genuinely needs a location grant up to API
            // 32 -- it is not a scan-results technicality there, the API
            // refuses to start without it. Capped so 33 and later use
            // NEARBY_WIFI_DEVICES instead and the app stops asking for
            // location it does not use.
            out = addPermission(out, "android.permission.ACCESS_FINE_LOCATION",
                    tiramisu ? " android:maxSdkVersion=\"32\"" : "");
        }

        if (companion) {
            out = addFeature(out, "android.software.companion_device_setup",
                    false);
            if (presence) {
                // Only for an app that observes presence. These are what let
                // the platform wake the app for a device it saw, and asking
                // for them without that is asking for background privileges
                // with no reason to show a user.
                out = addPermission(out,
                        "android.permission.REQUEST_COMPANION_RUN_IN_BACKGROUND",
                        "");
                out = addPermission(out,
                        "android.permission.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND",
                        "");
                if (tiramisu) {
                    out = addPermission(out, "android.permission"
                            + ".REQUEST_COMPANION_START_FOREGROUND_SERVICES"
                            + "_FROM_BACKGROUND", "");
                }
            }
            if (modern && watchProfile) {
                out = addPermission(out,
                        "android.permission.REQUEST_COMPANION_PROFILE_WATCH",
                        "");
            }
        }
        return out;
    }

    /**
     * The {@code <service>} element that binds
     * {@code CN1CompanionDeviceService}, or the empty string when the app
     * never observes presence.
     *
     * <p>Goes into {@code android.xapplication} rather than
     * {@code android.xpermissions}: it is an application child, not a
     * manifest one.</p>
     *
     * @param presence presence observation detected
     * @return the element, or {@code ""}
     */
    static String presenceService(boolean presence) {
        if (!presence) {
            return "";
        }
        return "        <service\n"
                + "            android:name=\"com.codename1.impl.android"
                + ".nearby.CN1CompanionDeviceService\"\n"
                + "            android:exported=\"true\"\n"
                + "            android:permission=\"android.permission"
                + ".BIND_COMPANION_DEVICE_SERVICE\">\n"
                + "            <intent-filter>\n"
                + "                <action android:name=\"android.companion"
                + ".CompanionDeviceService\" />\n"
                + "            </intent-filter>\n"
                + "        </service>\n";
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
