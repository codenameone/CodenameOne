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

import com.codename1.analytics.Analytics;
import com.codename1.analytics.AnalyticsConsent;
import com.codename1.analytics.ConsentMode;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.Storage;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InviteConsentAndErasureTest extends UITestBase {

    @AfterEach
    void cleanUp() {
        InviteTestSupport.tearDown();
    }

    @FormTest
    void resolutionWritesTheReferralDimensions() {
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);

        Invites.handleResolution(
                InviteTestSupport.resolvedJson("ABC123", "spring", "sms"),
                Invites.MATCH_REFERRER, true);

        Map<String, String> dims = Analytics.getDimensions();
        assertEquals("ABC123", dims.get(Invites.DIMENSION_CODE));
        assertEquals("spring", dims.get(Invites.DIMENSION_CAMPAIGN));
        assertEquals("sms", dims.get(Invites.DIMENSION_CHANNEL));
        assertEquals(Invites.MATCH_REFERRER, dims.get(Invites.DIMENSION_MATCH));
    }

    @FormTest
    void theDimensionsRideEveryLaterBatch() {
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.handleResolution(
                InviteTestSupport.resolvedJson("ABC123", "spring", "sms"),
                Invites.MATCH_REFERRER, true);

        Analytics.clearProviders();
        Analytics.setConsentMode(ConsentMode.OPT_OUT);
        Analytics.addProvider(new com.codename1.analytics.CodenameOneAnalyticsProvider());
        implementation.clearQueuedRequests();

        // This is the claim the whole feature rests on: revenue per campaign
        // needs no new aggregation, because the purchase event the framework
        // already emits arrives carrying the attribution.
        Analytics.event(com.codename1.analytics.AnalyticsEvent.create("purchase")
                .param("value", 9.99).build());
        Analytics.flush();

        List<ConnectionRequest> requests = implementation.getQueuedRequests();
        assertEquals(1, requests.size());
        String body = requests.get(0).getRequestBody();
        assertTrue(body.contains(Invites.DIMENSION_CAMPAIGN), body);
        assertTrue(body.contains("spring"), body);
        assertTrue(body.contains("purchase"), body);
    }

    @FormTest
    void resetClientIdErasesTheReferralDimensionsAndKeepsTheApplicationsOwn() {
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Analytics.setDimension("plan", "pro");
        Invites.handleResolution(
                InviteTestSupport.resolvedJson("ABC123", "spring", "sms"),
                Invites.MATCH_REFERRER, true);
        assertNotNull(Invites.getAttribution());

        Analytics.resetClientId();

        // Leaving the referral dimensions behind would re-link the freshly
        // issued pseudonymous id to the same inviter, which is exactly what
        // the erasure was asked to undo.
        Map<String, String> dims = Analytics.getDimensions();
        assertNull(dims.get(Invites.DIMENSION_CODE));
        assertNull(dims.get(Invites.DIMENSION_CAMPAIGN));
        assertNull(dims.get(Invites.DIMENSION_CHANNEL));
        assertNull(dims.get(Invites.DIMENSION_MATCH));
        // ... and taking the application's own dimensions with it would be
        // destroying data it never asked to lose.
        assertEquals("pro", dims.get("plan"));
        assertNull(Invites.getAttribution());
        assertEquals(Invites.STATE_NONE, Invites.getState());
    }

    @FormTest
    void registeringTheProviderIsNotMistakenForAnErasure() {
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.handleResolution(
                InviteTestSupport.resolvedJson("ABC123", "spring", "sms"),
                Invites.MATCH_REFERRER, true);

        // addProvider calls init() with the current client id, exactly as
        // resetClientId does. Only a CHANGE means erase.
        Analytics.addProvider(new RecordingProvider());
        Analytics.addProvider(new InviteAttributionProvider());

        assertNotNull(Invites.getAttribution(), "a plain registration erased the attribution");
        assertEquals("spring", Analytics.getDimensions().get(Invites.DIMENSION_CAMPAIGN));
    }

    @FormTest
    void nothingIsTransmittedBeforeConsentAndTheProfileIsDeletedIfRefused() {
        InviteTestSupport.freshInstall();
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(AnalyticsConsent.none());
        implementation.clearQueuedRequests();
        implementation.setAutoProcessConnections(false);

        Invites.checkForInvite();

        assertEquals(0, implementation.getQueuedRequests().size(),
                "nothing may leave the device before consent");
        // The profile is held locally so a deferred match is still possible if
        // consent arrives inside the window.
        assertTrue(Storage.getInstance().exists(InviteStore.PENDING));

        Analytics.setConsent(AnalyticsConsent.builder().analytics(false).build());

        assertFalse(Storage.getInstance().exists(InviteStore.PENDING),
                "a refused profile must be deleted, not held");
        assertEquals(Invites.STATE_DECLINED, Invites.getState());
    }

    @FormTest
    void optOutModeAloneDoesNotAuthoriseTheStatisticalMatch() {
        InviteTestSupport.freshInstall();
        // The deprecated AnalyticsService forces OPT_OUT, under which the
        // ordinary gate reports permission with no user choice on record.
        // Sending a device profile on that basis is not defensible, so the
        // match requires an explicit grant.
        Analytics.setConsentMode(ConsentMode.OPT_OUT);
        Analytics.setConsent(null);
        implementation.clearQueuedRequests();
        implementation.setAutoProcessConnections(false);

        Invites.checkForInvite();

        for (ConnectionRequest r : implementation.getQueuedRequests()) {
            assertFalse(r.getUrl().endsWith("/invites/match"),
                    "the statistical match went out under an implicit allow");
        }
    }

    @FormTest
    void revokingConsentClearsTheDimensionsButKeepsTheAttribution() {
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.handleResolution(
                InviteTestSupport.resolvedJson("ABC123", "spring", "sms"),
                Invites.MATCH_REFERRER, true);

        Analytics.setConsent(AnalyticsConsent.builder().analytics(false).build());
        assertNull(Analytics.getDimensions().get(Invites.DIMENSION_CAMPAIGN));
        assertNotNull(Invites.getAttribution(), "the local record is not personal to anyone else");

        Analytics.setConsent(AnalyticsConsent.granted());
        assertEquals("spring", Analytics.getDimensions().get(Invites.DIMENSION_CAMPAIGN),
                "re-granting must restore the dimensions from the stored record");
    }
}
