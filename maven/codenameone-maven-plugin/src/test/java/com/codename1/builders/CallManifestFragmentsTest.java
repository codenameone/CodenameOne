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

/** The manifest a call-using app gets. */
public class CallManifestFragmentsTest {

    @Test
    public void owningCallsEarnsManageOwnCalls() {
        String out = CallManifestFragments.injectPermissions("", true, false,
                false, 34);
        assertTrue(out.contains("\"android.permission.MANAGE_OWN_CALLS\""));
        assertTrue(out.contains("\"android.permission.RECORD_AUDIO\""));
    }

    @Test
    public void theDirectoryAloneDoesNotEarnManageOwnCalls() {
        // The load-bearing assertion of the whole package split: an app that
        // only labels somebody else's caller never owns a call, and Play
        // Console flags gratuitous telephony permissions.
        String out = CallManifestFragments.injectPermissions("", false, false,
                true, 34);
        assertFalse(out.contains("MANAGE_OWN_CALLS"),
                "labelling a number must not buy the right to own calls");
        assertFalse(out.contains("RECORD_AUDIO"),
                "labelling a number does not carry audio");
        assertFalse(out.contains("FOREGROUND_SERVICE_PHONE_CALL"));
    }

    @Test
    public void onlyVoipEarnsTheForegroundServiceAndNotificationPermissions() {
        String session = CallManifestFragments.injectPermissions("", true,
                false, false, 34);
        assertFalse(session.contains("FOREGROUND_SERVICE_PHONE_CALL"),
                "an app that never rings in the background pays nothing for"
                + " the ability to");
        String voip = CallManifestFragments.injectPermissions("", true, true,
                false, 34);
        assertTrue(voip.contains(
                "\"android.permission.FOREGROUND_SERVICE_PHONE_CALL\""));
        assertTrue(voip.contains("\"android.permission.POST_NOTIFICATIONS\""));
    }

    @Test
    public void permissionsAreDeclaredWhateverTheTargetSdk() {
        // A permission is requested at runtime according to the level the
        // DEVICE is running, and requesting one the manifest does not declare
        // is refused instantly with no prompt. A device below the level
        // ignores a permission it has never heard of, so declaring costs
        // nothing and gating costs the feature.
        String low = CallManifestFragments.injectPermissions("", true, true,
                false, 26);
        assertTrue(low.contains("FOREGROUND_SERVICE_PHONE_CALL"));
        assertTrue(low.contains("POST_NOTIFICATIONS"));
    }

    @Test
    public void anExistingPermissionIsNotDeclaredTwice() {
        String existing =
                "    <uses-permission android:name=\"android.permission.RECORD_AUDIO\" />\n";
        String out = CallManifestFragments.injectPermissions(existing, true,
                false, false, 34);
        assertEquals(1, count(out, "\"android.permission.RECORD_AUDIO\""),
                "a permission another feature already declared must not be"
                + " declared again");
    }

    @Test
    public void suppressionIsQuoteDelimitedSoOneNameCannotMaskAnother() {
        // FOREGROUND_SERVICE is a prefix of FOREGROUND_SERVICE_PHONE_CALL, so
        // a plain substring check would skip the longer one.
        String out = CallManifestFragments.injectPermissions("", true, true,
                false, 34);
        assertTrue(out.contains("\"android.permission.FOREGROUND_SERVICE\""));
        assertTrue(out.contains(
                "\"android.permission.FOREGROUND_SERVICE_PHONE_CALL\""));
    }

    @Test
    public void theConnectionServiceIsExportedAndPermissionGuarded() {
        String out = CallManifestFragments.services(true, false, false);
        assertTrue(out.contains(CallManifestFragments.CONNECTION_SERVICE));
        // Telecom is a different process and cannot bind an unexported
        // service, so exported=true is required rather than careless -- and
        // the permission attribute is what keeps anything else out, since
        // only the system holds BIND_TELECOM_CONNECTION_SERVICE.
        assertTrue(out.contains("android:exported=\"true\""));
        assertTrue(out.contains(
                "\"android.permission.BIND_TELECOM_CONNECTION_SERVICE\""));
        assertTrue(out.contains("\"android.telecom.ConnectionService\""));
    }

    @Test
    public void theScreeningServiceAppearsOnlyForTheDirectory() {
        assertFalse(CallManifestFragments.services(true, true, false)
                .contains(CallManifestFragments.SCREENING_SERVICE));
        String dir = CallManifestFragments.services(false, false, true);
        assertTrue(dir.contains(CallManifestFragments.SCREENING_SERVICE));
        assertTrue(dir.contains(
                "\"android.permission.BIND_SCREENING_SERVICE\""));
        assertFalse(dir.contains(CallManifestFragments.CONNECTION_SERVICE),
                "screening does not need a ConnectionService");
    }

    @Test
    public void noServicesWithoutDetection() {
        assertEquals("", CallManifestFragments.services(false, false, false));
        assertEquals("", CallManifestFragments.injectPermissions("", false,
                false, false, 34));
    }

    @Test
    public void owningCallsCarriesTheApiTwentySixFloor() {
        // A self-managed ConnectionService arrives exactly at API 26 and has
        // nothing to degrade to below it.
        assertEquals(26, CallManifestFragments.minimumSdk(true, false, false));
        assertEquals(26, CallManifestFragments.minimumSdk(false, true, false));
        assertEquals(24, CallManifestFragments.minimumSdk(false, false, true));
        assertEquals(0, CallManifestFragments.minimumSdk(false, false, false));
    }

    @Test
    public void injectingIntoNullIsSafe() {
        assertTrue(CallManifestFragments.injectPermissions(null, true, false,
                false, 34).contains("MANAGE_OWN_CALLS"));
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        int at = haystack.indexOf(needle);
        while (at >= 0) {
            n++;
            at = haystack.indexOf(needle, at + 1);
        }
        return n;
    }
}
