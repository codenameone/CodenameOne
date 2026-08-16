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
 * The Android manifest fragments for {@code com.codename1.home}, and the
 * accessory-data classifier the iOS entitlement gate reads.
 */
public class SmartHomeManifestFragmentsTest {

    /**
     * Without the queries entry the Google Home app is invisible to package
     * visibility on API 30+, so {@code getLaunchIntentForPackage} answers null
     * even when it is installed -- and {@code openEcosystemApp()}, the
     * recovery an app offers a user with no home set up, silently does
     * nothing.
     */
    @Test
    public void queriesDeclareTheGoogleHomeApp() {
        String queries = SmartHomeManifestFragments.injectQueries("");
        assertTrue(queries.contains(
                SmartHomeManifestFragments.GOOGLE_HOME_PACKAGE),
                queries);
        assertTrue(queries.contains("<package android:name="), queries);
    }

    @Test
    public void queriesInjectionIsIdempotentAndNullSafe() {
        String once = SmartHomeManifestFragments.injectQueries(null);
        String twice = SmartHomeManifestFragments.injectQueries(once);
        assertEquals(once, twice,
                "a project that already declared the package must keep its own"
                        + " entry rather than getting a second one");
    }

    @Test
    public void aProjectsOwnDeclarationIsLeftAlone() {
        String existing = "        <package android:name=\""
                + SmartHomeManifestFragments.GOOGLE_HOME_PACKAGE
                + "\" />\n";
        assertEquals(existing,
                SmartHomeManifestFragments.injectQueries(existing));
    }

    /**
     * The Play services home AAR declares no permissions, and neither does
     * this. Commissioning runs entirely inside Play services' own activity,
     * so an app that never scans for anything must not be made to ask its
     * users for Bluetooth.
     *
     * <p>Asserted as an absence because the temptation runs the other way:
     * "commissioning uses Bluetooth, so add BLUETOOTH_SCAN" is a reasonable
     * guess and a wrong one.</p>
     */
    @Test
    public void noPermissionsAreEmitted() {
        String queries = SmartHomeManifestFragments.injectQueries("");
        assertFalse(queries.contains("uses-permission"),
                "commissioning happens in Play services' own activity, so the"
                        + " app needs no permission of its own: " + queries);
        assertFalse(queries.toUpperCase().contains("BLUETOOTH"), queries);
        assertFalse(queries.toUpperCase().contains("LOCATION"), queries);
    }

    /**
     * From the AAR's own manifest. Raising the floor here beats letting
     * Gradle's manifest merger reject the build with an error naming a
     * transitive dependency the developer never wrote down.
     */
    @Test
    public void theMinimumSdkMatchesThePlayServicesAar() {
        assertEquals(21, SmartHomeManifestFragments.MINIMUM_SDK);
    }

    /**
     * The classifier that decides whether an app gets the HomeKit
     * entitlement.
     *
     * <p>Getting this wrong in the permissive direction is not harmless: the
     * entitlement has to be granted on the App ID, so an app that merely
     * rendered "smart home is unavailable" would fail codesigning for a
     * capability it never asked for.</p>
     */
    @Test
    public void touchingAccessoriesIsRecognized() {
        assertTrue(SmartHomeManifestFragments.isAccessoryDataCall("read"));
        assertTrue(SmartHomeManifestFragments.isAccessoryDataCall("write"));
        assertTrue(SmartHomeManifestFragments.isAccessoryDataCall("subscribe"));
        assertTrue(SmartHomeManifestFragments.isAccessoryDataCall("refresh"));
        assertTrue(SmartHomeManifestFragments.isAccessoryDataCall(
                "getStructures"));
        assertTrue(SmartHomeManifestFragments.isAccessoryDataCall(
                "executeScene"));
        assertTrue(SmartHomeManifestFragments.isAccessoryDataCall("identify"));
        assertTrue(SmartHomeManifestFragments.isAccessoryDataCall(
                "drainChanges"));
    }

    /**
     * The other half, and the one that makes the gate worth having. If every
     * method counted, the availability-only case would not exist.
     */
    @Test
    public void askingWhetherSmartHomeExistsIsNotTouchingIt() {
        String[] safe = SmartHomeManifestFragments.availabilityOnlyCalls();
        assertTrue(safe.length > 0);
        for (String method : safe) {
            assertFalse(SmartHomeManifestFragments.isAccessoryDataCall(method),
                    method + " must not earn the HomeKit entitlement: an app"
                            + " that only asks whether smart home is"
                            + " available would fail codesigning against an"
                            + " App ID that never enabled the capability");
        }
    }

    @Test
    public void theClassifierIsNullSafe() {
        assertFalse(SmartHomeManifestFragments.isAccessoryDataCall(null));
        assertFalse(SmartHomeManifestFragments.isAccessoryDataCall(""));
    }

    /**
     * Logged on every build so a BuildDaemon running a stale copy of this
     * class is apparent from the log rather than from a bug report.
     */
    @Test
    public void theFragmentVersionIsStated() {
        assertTrue(SmartHomeManifestFragments.FRAGMENT_VERSION.length() > 0);
    }
}
