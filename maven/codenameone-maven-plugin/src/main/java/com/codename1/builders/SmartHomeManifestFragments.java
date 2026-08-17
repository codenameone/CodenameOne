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
 * Builds the AndroidManifest fragments injected when an app uses
 * {@code com.codename1.home} on Android.
 *
 * <p>Extracted into a pure static helper so the logic is unit-testable
 * without running a build, and so the BuildDaemon copy stays trivially
 * diffable -- <b>keep this file in sync with
 * {@code com.codename1.build.daemon.SmartHomeManifestFragments}</b>.
 * {@link #FRAGMENT_VERSION} is logged during the build so a stale daemon is
 * visible in the log rather than silently producing an APK that cannot see
 * the Google Home app.</p>
 *
 * <h2>There are deliberately no permissions here</h2>
 *
 * <p>The obvious expectation is Bluetooth and local-network permissions,
 * because commissioning a Matter accessory involves both. It does not need
 * them: Play services runs the entire add-device interaction in <b>its own
 * activity</b>, holding its own permissions, and hands back a result. The
 * {@code play-services-home} AAR's manifest declares no permissions at all,
 * which is the authority on the question.</p>
 *
 * <p>Adding them "to be safe" would put a Bluetooth permission prompt in
 * front of the users of an app that never scans for anything -- and on
 * Android 12+ that is three separate runtime permissions with a
 * location-adjacent reputation.</p>
 *
 * <h2>What is needed is package visibility</h2>
 *
 * <p>{@code SmartHome.openEcosystemApp()} resolves a launch intent for the
 * Google Home app, and from API 30 onwards package-visibility filtering makes
 * an undeclared package invisible -- {@code getLaunchIntentForPackage}
 * answers null even when the app is installed. So the one fragment this class
 * emits is the {@code <queries>} entry, and without it the recovery path an
 * app offers a user with no Google Home setup silently does nothing.</p>
 */
final class SmartHomeManifestFragments {

    /**
     * Bumped whenever the emitted fragments change. Logged by the Android
     * builder so a BuildDaemon running an older copy of this class is
     * apparent from the build log rather than from a bug report.
     */
    static final String FRAGMENT_VERSION = "smart-home-1";

    /** The Google Home app, which is also the Matter commissioning UI host. */
    static final String GOOGLE_HOME_PACKAGE =
            "com.google.android.apps.chromecast.app";

    /**
     * The lowest API level {@code play-services-home} supports, from its own
     * AAR manifest. The builder raises the app's floor to this rather than
     * letting Gradle's manifest merger reject the build with an error that
     * names a transitive dependency the developer never wrote down.
     */
    static final int MINIMUM_SDK = 21;

    private SmartHomeManifestFragments() {
    }

    /**
     * Returns {@code xQueries} with the Google Home app declared.
     *
     * <p>Idempotent: a project that already declared the package keeps its
     * own entry rather than getting a second one, which the manifest merger
     * tolerates but which makes a generated manifest harder to read.</p>
     *
     * @param xQueries the accumulated {@code <queries>} body, possibly null
     * @return the body with the Google Home package declared
     */
    static String injectQueries(String xQueries) {
        // The complete attribute, not the bare name: a project already
        // declaring com.google.android.apps.chromecast.app.preview contains
        // this package's name as a prefix, and skipping on that basis leaves
        // the exact entry out. Package visibility is matched exactly on API
        // 30+, so getLaunchIntentForPackage then answers null with Google
        // Home installed -- and openEcosystemApp() silently does nothing.
        if (xQueries != null
                && xQueries.contains("\"" + GOOGLE_HOME_PACKAGE + "\"")) {
            return xQueries;
        }
        return (xQueries == null ? "" : xQueries)
                + "        <package android:name=\"" + GOOGLE_HOME_PACKAGE
                + "\" />\n";
    }

    /**
     * Whether a method observed on {@code com.codename1.home.SmartHome} means
     * the app touches accessory <b>data</b>, as opposed to merely asking
     * whether smart home exists.
     *
     * <p>Only used on the iOS side, where the distinction decides whether the
     * app gets the HomeKit entitlement -- and getting it wrong in the
     * permissive direction is not harmless: the entitlement has to be granted
     * on the App ID, so an app that merely called {@code getAvailability()}
     * would fail codesigning for a capability it never wanted.</p>
     *
     * <p>Lives here rather than in the iOS builder so both builders read one
     * list, and so the list is testable.</p>
     *
     * @param method the method name the scanner reported
     * @return true when the call implies reading or writing accessory state
     */
    static boolean isAccessoryDataCall(String method) {
        if (method == null || method.length() == 0) {
            // No method observed at all, which is not the same as one this
            // list has not heard of: there is nothing to classify, and
            // demanding an entitlement for it would be a build failure caused
            // by a blank string.
            return false;
        }
        // Everything EXCEPT the availability-only list, rather than a list of
        // the calls that count. The two readings fail in opposite directions
        // and only one of them is recoverable: a method this list has not
        // heard of is most likely a new one that touches the graph, and
        // guessing "not data" ships an app that codesigns cleanly and then
        // fails every accessory call on the device. Guessing "data" costs a
        // build error naming the entitlement, which the developer can act on.
        //
        // It also means adding a method to SmartHome cannot silently widen
        // what builds without the entitlement: it has to be put on the
        // availability-only list deliberately.
        for (String safe : availabilityOnlyCalls()) {
            if (safe.equals(method)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The methods that are explicitly <b>not</b> accessory-data calls, for the
     * test that pins the pair together.
     *
     * <p>Written out rather than inferred, because the interesting property is
     * that this list is non-empty: if every method counted as data access then
     * the availability-only case would not exist and the entitlement gate
     * would be pointless.</p>
     *
     * @return method names that must never imply the HomeKit entitlement
     */
    static String[] availabilityOnlyCalls() {
        return new String[] {
            // getInstance first, because every call goes through it: an app
            // that only probes availability calls getInstance() and
            // getAvailability(), and the scanner reports both -- so leaving
            // it out made the entitlement-free probe demand the entitlement.
            "getInstance",
            "getAvailability", "isSupported", "getBackend",
            "getConfigurationProblems", "areIdsPersistent",
            "isAutomationSupported", "getAuthorizationStatus",
            "openHomeSettings", "openEcosystemApp", "openProviderSetup",
            "getCommissioner"
        };
    }
}
