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
            String profiles, int targetSdkVersion) {
        String out = xPermissions == null ? "" : xPermissions;
        boolean modern = targetSdkVersion >= 31;
        boolean tiramisu = targetSdkVersion >= 33;

        if (ranging) {
            // Declared whatever the target SDK is. targetSdkVersion says
            // which compatibility behaviours the app opts into, NOT which
            // device it runs on -- and an app targeting 30 still runs on an
            // Android 12 phone with a UWB radio, where the runtime request
            // fails outright unless the manifest declares the permission.
            // Older devices ignore a permission they have never heard of, so
            // declaring it always costs nothing and gating it cost the
            // feature on every build that had not yet raised its target.
            out = addPermission(out, "android.permission.UWB_RANGING", "");
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
            // Declared whatever the app targets, for the same reason
            // UWB_RANGING above is. A permission is asked for at RUNTIME
            // according to the level the app is actually running under, and
            // requesting one the manifest does not declare is refused
            // instantly with no prompt -- so a target-30 app on Android 12
            // could not ask for BLUETOOTH_SCAN at all. A device below 31
            // ignores permissions it has never heard of, so declaring them
            // costs an older device nothing.
            out = addPermission(out, "android.permission.BLUETOOTH_SCAN",
                    " android:usesPermissionFlags=\"neverForLocation\"");
            out = addPermission(out,
                    "android.permission.BLUETOOTH_ADVERTISE", "");
            out = addPermission(out, "android.permission.BLUETOOTH_CONNECT",
                    "");
            out = addPermission(out, "android.permission.ACCESS_WIFI_STATE",
                    "");
            out = addPermission(out, "android.permission.CHANGE_WIFI_STATE",
                    "");
            out = addPermission(out,
                    "android.permission.NEARBY_WIFI_DEVICES",
                    " android:usesPermissionFlags=\"neverForLocation\"");
            // Nearby Connections genuinely needs a location grant up to API
            // 32 -- it is not a scan-results technicality there, the API
            // refuses to start without it. Capped so 33 and later use
            // NEARBY_WIFI_DEVICES instead and the app stops asking for
            // location it does not use.
            //
            // Widened rather than added, because addPermission suppresses a
            // duplicate by NAME alone. BluetoothManifestFragments runs first
            // and, for a scanning app with the default neverForLocation, has
            // already declared this permission with maxSdkVersion="30" -- so
            // the plain add left that cap in place and transport had no
            // location grant at all on Android 12 and 12L, where it cannot
            // start without one.
            out = widenPermission(out, "android.permission.ACCESS_FINE_LOCATION",
                    tiramisu ? 32 : 0);
            // COARSE alongside FINE, with the same reach. From Android 12 the
            // two are requested TOGETHER -- the system shows one dialog with a
            // precise/approximate choice and refuses a request for fine alone
            // when coarse is not declared -- so a transport app on 12 or 12L
            // could not obtain the location grant Nearby Connections needs
            // there, and discovery never started.
            out = widenPermission(out,
                    "android.permission.ACCESS_COARSE_LOCATION",
                    tiramisu ? 32 : 0);
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
                // API 31, not 33. Gating this on the Tiramisu boundary
                // left an Android 12/12L app unable to use the
                // companion-device exemption when the platform woke its
                // CN1CompanionDeviceService -- which is the whole point of
                // observing presence. Verified against the SDK's own
                // api-versions.xml, not inferred from the neighbours.
                if (modern) {
                    out = addPermission(out, "android.permission"
                            + ".REQUEST_COMPANION_START_FOREGROUND_SERVICES"
                            + "_FROM_BACKGROUND", "");
                }
            }
            // One permission per profile the app says it selects, and all
            // three the portable API exposes -- not only WATCH.
            // AndroidNearbyBackend forwards COMPUTER on API 33 and GLASSES
            // on 34, and without the matching permission the platform
            // rejects the association before the chooser opens, which looks
            // to the user like nothing happened at all.
            //
            // Declared whatever the target SDK is, for the reason
            // UWB_RANGING above is: selecting a profile needs its permission
            // on a device that has the profile no matter what the app
            // targets, and an app targeting 30 had the association rejected
            // there. Older devices ignore a permission they never heard of.
            if (hasProfile(profiles, "watch")) {
                out = addPermission(out,
                        "android.permission.REQUEST_COMPANION_PROFILE_WATCH",
                        "");
            }
            if (hasProfile(profiles, "computer")) {
                out = addPermission(out,
                        "android.permission"
                        + ".REQUEST_COMPANION_PROFILE_COMPUTER", "");
            }
            if (hasProfile(profiles, "glasses")) {
                out = addPermission(out,
                        "android.permission"
                        + ".REQUEST_COMPANION_PROFILE_GLASSES", "");
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

    /// Makes sure a permission is declared and that its `maxSdkVersion` cap,
    /// if any, reaches at least as far as this feature needs.
    ///
    /// A permission another feature already declared is not re-added -- the
    /// manifest would then carry it twice -- so the only way to widen its
    /// reach is to edit the declaration that is there. An existing
    /// declaration with no cap already covers every level and is left alone.
    ///
    /// @param xPermissions the manifest fragment so far
    /// @param name the permission
    /// @param requiredThrough the highest API level at which the permission
    ///        must still be granted, or 0 when it must not be capped at all
    /// @return the fragment, with the declaration added or widened
    static String widenPermission(String xPermissions, String name,
            int requiredThrough) {
        int at = xPermissions.indexOf("\"" + name + "\"");
        if (at < 0) {
            return addPermission(xPermissions, name, requiredThrough > 0
                    ? " android:maxSdkVersion=\"" + requiredThrough + "\"" : "");
        }
        int start = xPermissions.lastIndexOf('<', at);
        int end = xPermissions.indexOf('>', at);
        if (start < 0 || end < 0) {
            return xPermissions;
        }
        String element = xPermissions.substring(start, end + 1);
        String marker = "android:maxSdkVersion=\"";
        int capAt = element.indexOf(marker);
        if (capAt < 0) {
            // Uncapped, so it already reaches further than anything asked for.
            return xPermissions;
        }
        int capEnd = element.indexOf('"', capAt + marker.length());
        if (capEnd < 0) {
            return xPermissions;
        }
        int cap;
        try {
            cap = Integer.parseInt(element.substring(
                    capAt + marker.length(), capEnd).trim());
        } catch (NumberFormatException notANumber) {
            return xPermissions;
        }
        if (requiredThrough > 0 && cap >= requiredThrough) {
            return xPermissions;
        }
        String widened;
        if (requiredThrough > 0) {
            widened = element.substring(0, capAt + marker.length())
                    + requiredThrough + element.substring(capEnd);
        } else {
            widened = element.substring(0, capAt)
                    + element.substring(capEnd + 1);
            // The attribute left a double space behind it.
            widened = widened.replace("  ", " ");
        }
        return xPermissions.substring(0, start) + widened
                + xPermissions.substring(end + 1);
    }

    /// True when a comma-separated profile list names this profile.
    ///
    /// Compared on whole entries so "watch" does not match a longer name
    /// that merely contains it.
    ///
    /// @param profiles the comma-separated list, may be null
    /// @param profile the profile to look for, lowercase
    /// @return whether the list names it
    static boolean hasProfile(String profiles, String profile) {
        if (profiles == null) {
            return false;
        }
        String[] parts = profiles.split(",");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].trim().toLowerCase(java.util.Locale.ROOT)
                    .equals(profile)) {
                return true;
            }
        }
        return false;
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

    // ------------------------------------------------------------------
    // Library bytecode
    // ------------------------------------------------------------------

    /// What a tree of bytecode was found to use.
    public static final class NearbyUsage {

        private boolean ranging;
        private boolean transport;
        private boolean companion;
        private boolean presence;

        public boolean usesRanging() {
            return ranging;
        }

        public boolean usesTransport() {
            return transport;
        }

        public boolean usesCompanion() {
            return companion;
        }

        public boolean usesPresence() {
            return presence;
        }

        /// True when nothing at all was found, which is the ordinary case.
        public boolean isEmpty() {
            return !ranging && !transport && !companion && !presence;
        }
    }

    /// The package a reference to it is stored under, in every constant pool
    /// that names one of its classes.
    private static final String RANGING_MARKER =
            "com/codename1/nearby/ranging/";
    private static final String TRANSPORT_MARKER =
            "com/codename1/nearby/transport/";
    private static final String COMPANION_MARKER =
            "com/codename1/nearby/companion/";
    /// The method name, because presence is a call rather than a class.
    private static final String PRESENCE_MARKER = "startObservingPresence";

    /// Classes whose own mention of these packages says nothing about the
    /// application: the API, the simulator bridge and the ports implement
    /// them, so a framework jar staged beside the libraries would otherwise
    /// report every application as using all of it.
    private static final String[] FRAMEWORK_PREFIXES = {
        "com/codename1/nearby/",
        "com/codename1/impl/nearby/",
        "com/codename1/impl/android/nearby/",
        "com/codename1/impl/ios/",
    };

    /// What the bytecode under `root` uses of the nearby packages.
    ///
    /// Loose class files, jars and Android archives alike, because a library
    /// can be the only thing that touches these APIs -- the application calls
    /// the library and never names a nearby class itself. Reading only the
    /// loose tree reported no use at all, and the Android build then DELETED
    /// the implementation package out from under the library that calls it
    /// while iOS left the natives and frameworks out, so the feature was
    /// missing from a build that looked clean. The database scan is extended
    /// over the same trees for the same reason.
    ///
    /// The test is a search of the whole class file for the package name,
    /// which is how every reference to a class in it is stored. A class that
    /// mentions the string for some other reason counts too, which errs
    /// towards keeping the implementation -- the safe direction, since the
    /// cost of a false positive is bytes and the cost of a false negative is
    /// an app that crashes on a class the build removed.
    ///
    /// #### Parameters
    ///
    /// - `root`: a directory of staged classes and libraries, or null
    ///
    /// #### Returns
    ///
    /// what it uses, never null and empty when `root` is not a directory
    public static NearbyUsage scanForNearbyUsage(java.io.File root) {
        NearbyUsage found = new NearbyUsage();
        if (root != null && root.isDirectory()) {
            scanTree(root, "", found);
        }
        return found;
    }

    private static void scanTree(java.io.File dir, String relativePath,
            NearbyUsage found) {
        java.io.File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (int iter = 0; iter < children.length; iter++) {
            java.io.File child = children[iter];
            String childPath = relativePath.length() == 0
                    ? child.getName() : relativePath + "/" + child.getName();
            String name = child.getName().toLowerCase(java.util.Locale.ROOT);
            if (child.isDirectory()) {
                scanTree(child, childPath, found);
            } else if (name.endsWith(".jar") || name.endsWith(".aar")
                    || name.endsWith(".zip")) {
                scanArchive(child, found);
            } else if (name.endsWith(".class")
                    && !isFrameworkClass(childPath)) {
                inspect(readAll(child), found);
            }
        }
    }

    private static void scanArchive(java.io.File archive, NearbyUsage found) {
        java.util.zip.ZipFile zip = null;
        try {
            zip = new java.util.zip.ZipFile(archive);
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries =
                    zip.entries();
            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entry.isDirectory()) {
                    continue;
                }
                String lower = entryName.toLowerCase(java.util.Locale.ROOT);
                if (lower.endsWith(".jar")) {
                    // An Android archive keeps its bytecode in a nested
                    // classes.jar, so the entries that matter are one level
                    // further in. Caught per entry: one unreadable entry says
                    // nothing about the entries after it.
                    try {
                        inspectNested(readAll(zip.getInputStream(entry)),
                                found);
                    } catch (Throwable unreadable) {
                        continue;
                    }
                } else if (lower.endsWith(".class")
                        && !isFrameworkClass(entryName)) {
                    try {
                        inspect(readAll(zip.getInputStream(entry)), found);
                    } catch (Throwable unreadable) {
                        continue;
                    }
                }
            }
        } catch (Throwable unreadable) {
            // Not an archive, or a broken one. Nothing can be read out of it,
            // and guessing that it uses everything would charge the whole
            // apparatus to every application that ships a stray file.
            return;
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (java.io.IOException ignored) {
                    // Nothing useful to do with a failure to close.
                }
            }
        }
    }

    private static void inspectNested(byte[] archiveBytes, NearbyUsage found) {
        java.util.zip.ZipInputStream in = new java.util.zip.ZipInputStream(
                new java.io.ByteArrayInputStream(archiveBytes));
        try {
            java.util.zip.ZipEntry entry = in.getNextEntry();
            while (entry != null) {
                String entryName = entry.getName();
                if (!entry.isDirectory()
                        && entryName.toLowerCase(java.util.Locale.ROOT)
                                .endsWith(".class")
                        && !isFrameworkClass(entryName)) {
                    inspect(readAll(in), found);
                }
                entry = in.getNextEntry();
            }
        } catch (Throwable unreadable) {
            return;
        } finally {
            try {
                in.close();
            } catch (java.io.IOException ignored) {
                // Nothing useful to do with a failure to close.
            }
        }
    }

    private static boolean isFrameworkClass(String path) {
        String normalized = path.replace('\\', '/');
        for (int iter = 0; iter < FRAMEWORK_PREFIXES.length; iter++) {
            if (normalized.indexOf(FRAMEWORK_PREFIXES[iter]) >= 0) {
                return true;
            }
        }
        return false;
    }

    private static void inspect(byte[] bytes, NearbyUsage found) {
        if (bytes == null || bytes.length == 0) {
            return;
        }
        String text;
        try {
            text = new String(bytes, "ISO-8859-1");
        } catch (java.io.UnsupportedEncodingException never) {
            return;
        }
        found.ranging |= text.indexOf(RANGING_MARKER) >= 0;
        found.transport |= text.indexOf(TRANSPORT_MARKER) >= 0;
        found.companion |= text.indexOf(COMPANION_MARKER) >= 0;
        found.presence |= text.indexOf(PRESENCE_MARKER) >= 0;
    }

    private static byte[] readAll(java.io.File file) {
        try {
            java.io.InputStream in = new java.io.FileInputStream(file);
            try {
                return readAll(in);
            } finally {
                in.close();
            }
        } catch (Throwable unreadable) {
            return null;
        }
    }

    private static byte[] readAll(java.io.InputStream in) {
        java.io.ByteArrayOutputStream out =
                new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        try {
            int read = in.read(buffer);
            while (read > 0) {
                out.write(buffer, 0, read);
                read = in.read(buffer);
            }
        } catch (java.io.IOException unreadable) {
            return out.toByteArray();
        }
        return out.toByteArray();
    }
}
