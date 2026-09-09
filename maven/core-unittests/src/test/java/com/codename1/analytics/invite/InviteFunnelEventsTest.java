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
package com.codename1.analytics.invite;

import com.codename1.analytics.AnalyticsEvent;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.share.ShareResult;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InviteFunnelEventsTest extends UITestBase {

    @AfterEach
    void cleanUp() {
        InviteTestSupport.tearDown();
    }

    @FormTest
    void sharedToReportsInviteSharedWithTheRealTarget() {
        RecordingProvider recorder = InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invite invite = Invites.create(InviteRequest.create().campaign("spring").build());
        recorder.clear();

        Invites.reportShareResult(invite, ShareResult.sharedTo("com.whatsapp"));

        AnalyticsEvent e = recorder.first("invite_shared");
        assertNotNull(e, "expected invite_shared, saw " + recorder.names());
        assertEquals(Invites.CATEGORY, e.getCategory());
        assertEquals(invite.getCode(), e.getParameters().get("invite_code"));
        assertEquals("com.whatsapp", e.getParameters().get("target"));
    }

    @FormTest
    void aDismissedSheetNeverReportsAShare() {
        RecordingProvider recorder = InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invite invite = Invites.create(InviteRequest.create().build());
        recorder.clear();

        Invites.reportShareResult(invite, ShareResult.dismissed());

        // This is the difference between a measured funnel and an assumed one:
        // "created but abandoned" has to be distinguishable from "sent".
        assertEquals(0, recorder.count("invite_shared"));
        assertNotNull(recorder.first("invite_share_dismissed"));
    }

    @FormTest
    void anUnknownTargetOmitsTheParameterRatherThanInventingOne() {
        RecordingProvider recorder = InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invite invite = Invites.create(InviteRequest.create().build());
        recorder.clear();

        // Older Android and the web share api cannot say where it went.
        Invites.reportShareResult(invite, ShareResult.sharedTo(null));

        AnalyticsEvent e = recorder.first("invite_shared");
        assertNotNull(e);
        assertFalse(e.getParameters().containsKey("target"),
                "an unknown target must be absent, not a placeholder");
    }

    @FormTest
    void conversionIsANoOpUntilSomethingIsAttributed() {
        RecordingProvider recorder = InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        recorder.clear();

        Invites.conversion("signup", 9.99, "USD");

        assertEquals(0, recorder.count("invite_converted"));
    }

    @FormTest
    void conversionCarriesValueAndCurrencyOnceAttributed() {
        RecordingProvider recorder = InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.handleResolution(
                InviteTestSupport.resolvedJson("ABC123", "spring", "sms"),
                Invites.MATCH_REFERRER, true);
        recorder.clear();

        Invites.conversion("signup", 9.99, "USD");

        AnalyticsEvent e = recorder.first("invite_converted");
        assertNotNull(e, "expected invite_converted, saw " + recorder.names());
        assertEquals(Invites.CATEGORY, e.getCategory());
        assertEquals("ABC123", e.getParameters().get("invite_code"));
        assertEquals("spring", e.getParameters().get("campaign"));
        assertEquals("signup", e.getParameters().get("action"));
        assertEquals("USD", e.getParameters().get("currency"));
        assertNotNull(e.getParameters().get("value"));
    }

    @FormTest
    void aDeferredResolutionReportsAnInstallRatherThanAnOpen() {
        RecordingProvider recorder = InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        recorder.clear();

        Invites.handleResolution(
                InviteTestSupport.resolvedJson("ABC123", "spring", "sms"),
                Invites.MATCH_REFERRER, true);

        assertNotNull(recorder.first("invite_install"));
        assertEquals(0, recorder.count("invite_opened"));
        assertEquals(Invites.MATCH_REFERRER,
                recorder.first("invite_install").getParameters().get("match"));
    }

    @FormTest
    void aDirectOpenReportsAnOpenRatherThanAnInstall() {
        RecordingProvider recorder = InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        recorder.clear();

        Invites.handleResolution(
                InviteTestSupport.resolvedJson("ABC123", "spring", "sms"),
                Invites.MATCH_DIRECT, false);

        assertNotNull(recorder.first("invite_opened"));
        assertEquals(0, recorder.count("invite_install"));
    }

    @FormTest
    void everyFunnelEventUsesTheReferralCategory() {
        RecordingProvider recorder = InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invite invite = Invites.create(InviteRequest.create().build());
        Invites.reportShareResult(invite, ShareResult.sharedTo("com.whatsapp"));
        Invites.handleResolution(
                InviteTestSupport.resolvedJson("ABC123", "spring", "sms"),
                Invites.MATCH_REFERRER, true);
        Invites.conversion("signup");

        assertTrue(recorder.events().size() >= 4, recorder.names().toString());
        for (AnalyticsEvent e : recorder.events()) {
            assertEquals(Invites.CATEGORY, e.getCategory(),
                    e.getName() + " is not under the referral category");
        }
    }
}
