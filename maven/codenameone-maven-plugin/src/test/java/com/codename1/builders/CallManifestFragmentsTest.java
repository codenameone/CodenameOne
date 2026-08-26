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
    public void onlyVoipEarnsTheForegroundServicePermission() {
        String session = CallManifestFragments.injectPermissions("", true,
                false, false, 34);
        assertFalse(session.contains("FOREGROUND_SERVICE_PHONE_CALL"),
                "an app that never rings in the background pays nothing for"
                + " the ability to");
        String voip = CallManifestFragments.injectPermissions("", true, true,
                false, 34);
        assertTrue(voip.contains(
                "\"android.permission.FOREGROUND_SERVICE_PHONE_CALL\""));
    }

    @Test
    public void everyCallingAppCanRingOnScreen() {
        // Telecom draws nothing for a self-managed account, so the port rings
        // with a full-screen-intent notification -- which needs both of these
        // whether or not the app was woken by a push. Without them a reported
        // call rang in Telecom's bookkeeping and appeared nowhere.
        String session = CallManifestFragments.injectPermissions("", true,
                false, false, 34);
        assertTrue(session.contains(
                "\"android.permission.POST_NOTIFICATIONS\""));
        assertTrue(session.contains(
                "\"android.permission.USE_FULL_SCREEN_INTENT\""));
        String directory = CallManifestFragments.injectPermissions("", false,
                false, true, 34);
        assertFalse(directory.contains("USE_FULL_SCREEN_INTENT"),
                "labelling somebody else's caller never rings anything");
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
    public void oneHandDeclaredServiceDoesNotSuppressTheOther() {
        // The bug: an app that declared either service itself in
        // android.xapplication used to suppress the whole generated block,
        // so the other went missing and either Telecom could not create the
        // app's calls or Android could not bind the screening service.
        String mine = "        <service android:name=\""
                + CallManifestFragments.CONNECTION_SERVICE + "\" />\n";
        String out = CallManifestFragments.services(true, false, true, mine);
        assertFalse(out.contains(CallManifestFragments.CONNECTION_SERVICE),
                "a service the project declared must not be generated again");
        assertTrue(out.contains(CallManifestFragments.SCREENING_SERVICE),
                "the other service is still required");
    }

    @Test
    public void aHandDeclaredScreeningServiceLeavesTheConnectionService() {
        String mine = "        <service android:name=\""
                + CallManifestFragments.SCREENING_SERVICE + "\" />\n";
        String out = CallManifestFragments.services(true, false, true, mine);
        assertTrue(out.contains(CallManifestFragments.CONNECTION_SERVICE));
        assertFalse(out.contains(CallManifestFragments.SCREENING_SERVICE));
    }

    @Test
    public void aMereMentionDoesNotCountAsADeclaration() {
        // Suppression used to be a substring test, so anything that merely
        // named the class -- an XML comment, a meta-data value, an
        // intent-filter -- deleted the real <service>. A missing
        // ConnectionService is not a build error: Telecom just refuses every
        // call at runtime, on a device, long after the build went green.
        String mine = "        <!-- we do not use "
                + CallManifestFragments.CONNECTION_SERVICE + " yet -->\n"
                + "        <meta-data android:name=\"screener\""
                + " android:value=\""
                + CallManifestFragments.SCREENING_SERVICE + "\" />\n";
        String out = CallManifestFragments.services(true, false, true, mine);
        assertTrue(out.contains("android:name=\""
                + CallManifestFragments.CONNECTION_SERVICE + "\""));
        assertTrue(out.contains("android:name=\""
                + CallManifestFragments.SCREENING_SERVICE + "\""));
    }

    @Test
    public void everyCompileSdkRaiseIsInTheHelper() {
        // The service-type decision is read thousands of lines before the
        // gradle file is written, and THREE separate raises turned out to
        // apply only at the later site -- the target raise, the ranging
        // raise, and the 33 floor every nearby cluster carries. Each time the
        // manifest got a preliminary answer, dropped the phoneCall service
        // type, and shipped an app that compiled against something newer,
        // where startForeground refuses a type the manifest never declared.
        //
        // This pins the enumeration rather than any one raise: a raise added
        // to the generation below must be added to the helper too, or these
        // stop matching.
        assertEquals(36, AndroidGradleBuilder.RANGING_MIN_COMPILE_SDK);
        assertEquals(33, AndroidGradleBuilder.NEARBY_MIN_COMPILE_SDK);
        // Ranging alone reaches 36 even when the ladder and the target are
        // far below it.
        assertEquals(36, AndroidGradleBuilder.compileSdkInt("28", "28", "28",
                true, true, false));
        // Transport or companion without ranging still reaches 33.
        assertEquals(33, AndroidGradleBuilder.compileSdkInt("28", "28", "28",
                false, true, false));
        // No nearby at all: the ladder and the target decide.
        assertEquals(28, AndroidGradleBuilder.compileSdkInt("28", "28", "28",
                false, false, false));
        assertEquals(34, AndroidGradleBuilder.compileSdkInt("28", "28", "34",
                false, false, false),
                "the compile SDK is never below the target");
    }

    @Test
    public void aVoipAppTargeting29OrLaterCanDeclareItsServiceType() {
        // The attribute is required from 34 and unwritable below a compile
        // SDK of 29, so leaving a target-34 VoIP build at 28 shipped an app
        // whose foreground service could not start at all. Raised only when
        // the TARGET asks for it: an app really targeting 28 keeps its
        // legacy compile SDK, needing neither the attribute nor the raise.
        // In THIS copy the compile SDK is raised to the target
        // unconditionally, so a target that requires the attribute has
        // already made it writable and no separate VoIP rule is needed. The
        // daemon twin raises to the target only inside its container path,
        // which is why it states the rule on its own.
        assertEquals(34, AndroidGradleBuilder.compileSdkInt("28", "28", "34",
                false, false, true),
                "a target-34 VoIP build must be able to name phoneCall");
        assertTrue(AndroidGradleBuilder.compileSdkInt("28", "28", "34",
                false, false, true)
                >= AndroidGradleBuilder.FOREGROUND_SERVICE_TYPE_COMPILE_SDK);
        assertEquals(28, AndroidGradleBuilder.compileSdkInt("28", "28", "28",
                false, false, true),
                "a target-28 app neither needs the attribute nor a raise");
    }

    @Test
    public void thePhoneCallServiceTypeNeedsApi29ToCompile() {
        // android:foregroundServiceType="phoneCall" arrived in API 29, and
        // AAPT REJECTS a manifest naming an enum value the compile SDK does
        // not know -- so emitting it unconditionally broke the still
        // supported android.useGradle8=false with buildToolsVersion=28
        // configuration before compilation even started.
        String old = CallManifestFragments.services(true, true, false, "", 28);
        assertFalse(old.contains("foregroundServiceType"),
                "compiling against 28 cannot name a 29 enum value");
        assertTrue(old.contains(CallManifestFragments.CONNECTION_SERVICE),
                "the service itself is still required");

        String modern = CallManifestFragments.services(true, true, false, "", 29);
        assertTrue(modern.contains("android:foregroundServiceType=\"phoneCall\""));
        assertTrue(CallManifestFragments.services(true, true, false, "", 34)
                .contains("foregroundServiceType"));
        // Unknown compile SDK keeps the attribute: the default path is
        // modern, and dropping it there would cost startForeground on 34+.
        assertTrue(CallManifestFragments.services(true, true, false, "", 0)
                .contains("foregroundServiceType"));
    }

    @Test
    public void aCommentedOutDeclarationIsNotADeclaration() {
        // The exact-attribute matcher still matched inside a comment, so an
        // app that had commented its own <service> OUT lost the generated one
        // too -- and a manifest with no ConnectionService is not a build
        // error, just Telecom refusing every call on the device.
        String mine = "        <!-- <service android:name=\""
                + CallManifestFragments.CONNECTION_SERVICE + "\" /> -->\n";
        assertTrue(CallManifestFragments.services(true, false, false, mine)
                .contains("android:name=\""
                        + CallManifestFragments.CONNECTION_SERVICE + "\""));
    }

    @Test
    public void anAttributeOutsideAServiceElementIsNotADeclaration() {
        String mine = "        <meta-data android:name=\""
                + CallManifestFragments.CONNECTION_SERVICE + "\""
                + " android:value=\"1\" />\n";
        assertTrue(CallManifestFragments.services(true, false, false, mine)
                .contains("android:name=\""
                        + CallManifestFragments.CONNECTION_SERVICE + "\""));
    }

    @Test
    public void aLiveDeclarationAfterACommentStillSuppresses() {
        String mine = "        <!-- old: <service android:name=\"x\" /> -->\n"
                + "        <service android:name=\""
                + CallManifestFragments.CONNECTION_SERVICE + "\" />\n";
        assertFalse(CallManifestFragments.services(true, false, false, mine)
                .contains(CallManifestFragments.CONNECTION_SERVICE));
    }

    @Test
    public void aLongerClassNameThatStartsWithOursIsNotOurs() {
        String mine = "        <service android:name=\""
                + CallManifestFragments.CONNECTION_SERVICE + "Proxy\" />\n";
        String out = CallManifestFragments.services(true, false, false, mine);
        assertTrue(out.contains("android:name=\""
                + CallManifestFragments.CONNECTION_SERVICE + "\""),
                "an app's own subclass must not displace the real service");
    }

    @Test
    public void singleQuotedDeclarationsSuppressToo() {
        // android.xapplication is hand-written XML, where either quoting is
        // valid.
        String mine = "        <service android:name='"
                + CallManifestFragments.CONNECTION_SERVICE + "' />\n";
        assertFalse(CallManifestFragments.services(true, false, false, mine)
                .contains(CallManifestFragments.CONNECTION_SERVICE));
    }

    @Test
    public void declaringBothLeavesNothingToGenerate() {
        String mine = CallManifestFragments.services(true, false, true, "");
        assertEquals("", CallManifestFragments.services(true, false, true, mine));
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

    @Test
    public void aVoipConnectionServiceDeclaresItsForegroundType() {
        // From API 34 startForeground is refused for a type the manifest
        // never declared, so granting FOREGROUND_SERVICE_PHONE_CALL and
        // stopping there cannot keep the service alive for a call that
        // arrived in the background.
        String voip = CallManifestFragments.services(true, true, false);
        assertTrue(voip.contains("android:foregroundServiceType=\"phoneCall\""));
        String session = CallManifestFragments.services(true, false, false);
        assertFalse(session.contains("foregroundServiceType"),
                "an app that never rings in the background does not run a"
                        + " foreground service for calls");
    }
}
