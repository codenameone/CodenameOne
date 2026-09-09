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
    void nothingIsTransmittedBeforeAChoiceAndTheProfileIsDeletedIfRefused() {
        // "No choice yet" is null, NOT AnalyticsConsent.none() -- none() is an
        // explicit refusal, and this test used to conflate the two. The
        // distinction is the whole point: an unanswered prompt still captures
        // the profile, because the match window closes long before a user gets
        // round to answering.
        InviteTestSupport.freshInstall();
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(null);
        implementation.clearQueuedRequests();
        implementation.setAutoProcessConnections(false);

        Invites.checkForInvite();

        assertEquals(0, implementation.getQueuedRequests().size(),
                "nothing may leave the device before a choice is made");
        assertTrue(Storage.getInstance().exists(InviteStore.PENDING),
                "an unanswered prompt must still capture the profile");

        Analytics.setConsent(AnalyticsConsent.builder().analytics(false).build());

        assertNoProfileHeld("a refused profile must be deleted, not held");
        assertEquals(Invites.STATE_DECLINED, Invites.getState());
    }

    @FormTest
    void anAlreadyRefusedUserNeverGetsAProfileWrittenAtAll() {
        // The earlier hole: the record was created and persisted BEFORE the
        // consent check, and onConsentChanged only deletes a record that exists
        // when it runs. So a user who had already refused got a profile written
        // on the next launch and it stayed there indefinitely -- contradicting
        // the documented promise that a refused profile is deleted.
        InviteTestSupport.freshInstall();
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(AnalyticsConsent.builder().analytics(false).build());
        implementation.clearQueuedRequests();
        implementation.setAutoProcessConnections(false);

        Invites.checkForInvite();

        assertNoProfileHeld("a refused user must never have a profile written");
        assertEquals(0, implementation.getQueuedRequests().size());
        assertEquals(Invites.STATE_DECLINED, Invites.getState());
    }

    @FormTest
    void registeringTheProviderBeforeAnyChoiceMustNotLookLikeARefusal() {
        // The regression this pins: Analytics.addProvider synthesizes
        // AnalyticsConsent.denied() for the null state, and the invite provider
        // is registered on every facade entry. A second launch before the user
        // has answered the prompt therefore arrived looking exactly like an
        // explicit refusal, deleted the profile captured on the first launch,
        // and moved to DECLINED -- so a later grant could never resume, for a
        // user who had refused nothing.
        InviteTestSupport.freshInstall();
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(null);
        implementation.setAutoProcessConnections(false);

        // First launch captures the deferred profile.
        Invites.checkForInvite();
        assertTrue(Storage.getInstance().exists(InviteStore.PENDING));
        assertEquals(Invites.STATE_PENDING, Invites.getState());

        // Second launch, still no choice on record: re-registering the provider
        // must leave the profile alone.
        Analytics.clearProviders();
        Analytics.addProvider(new InviteAttributionProvider());

        assertTrue(Storage.getInstance().exists(InviteStore.PENDING),
                "an unanswered prompt was treated as a refusal");
        assertEquals(Invites.STATE_PENDING, Invites.getState());

        // And a later grant still resolves rather than being stuck at DECLINED.
        Analytics.setConsent(AnalyticsConsent.granted());
        assertEquals(Invites.STATE_PENDING, Invites.getState());
    }

    @FormTest
    void customParametersSurviveARestart() {
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.handleResolution("{\"resolved\":true,\"code\":\"ABC123\","
                + "\"campaign\":\"spring\",\"parameters\":{\"room\":\"42\"}}",
                Invites.MATCH_REFERRER, true);
        assertEquals("42", Invites.getAttribution().getParameters().get("room"));

        // Simulate the next process: drop the in-memory copy and re-read the
        // durable record. An answer that arrives before the listener registers
        // is delivered on the NEXT launch, so losing the parameters here means
        // delivering an attribution stripped of the data the app acts on.
        Invites.forgetCachedAttributionForTest();

        assertEquals("42", Invites.getAttribution().getParameters().get("room"),
                "custom parameters did not survive the restart");
        assertEquals("spring", Invites.getAttribution().getCampaign());
    }

    @FormTest
    void aResponseInFlightDuringAnErasureIsDiscarded() {
        // An erasure deletes the pending record and clears the dimensions, but
        // the request it raced was already on the wire. Resolving it anyway
        // wrote the attribution and the referral dimensions straight back --
        // under the freshly issued identity -- undoing exactly what the user
        // asked for.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.checkForInvite();

        int issuedUnder = Invites.currentLookupEpochForTest();
        Analytics.resetClientId();

        Invites.handleResolution(
                InviteTestSupport.resolvedJson("ABC123", "spring", "sms"),
                Invites.MATCH_REFERRER, true, issuedUnder);

        assertNull(Invites.getAttribution(), "an erased identity was re-attributed");
        assertNull(Analytics.getDimensions().get(Invites.DIMENSION_CAMPAIGN));
    }

    @FormTest
    void aResponseInFlightWhenConsentIsWithdrawnIsDiscarded() {
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.checkForInvite();

        int issuedUnder = Invites.currentLookupEpochForTest();
        Analytics.setConsent(AnalyticsConsent.builder().analytics(false).build());

        Invites.handleResolution(
                InviteTestSupport.resolvedJson("ABC123", "spring", "sms"),
                Invites.MATCH_REFERRER, true, issuedUnder);

        assertNull(Invites.getAttribution(), "a refusal was overridden by a late response");
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

    /**
     * Asserts that no device profile is held, which is not the same as
     * asserting the record is absent.
     *
     * <p>A refusal has to be durable or the listener is told again on every
     * launch, so what remains is a marker carrying the state and the reason and
     * nothing else. The promise is about the profile -- the platform, the OS
     * version, the model, the screen size, the locale -- and that is what this
     * checks. Asserting absence instead made the promise untestable the moment
     * it had to survive a relaunch.</p>
     */
    private void assertNoProfileHeld(String message) {
        Map<String, String> record = InviteStore.read(InviteStore.PENDING);
        if (record == null) {
            return;
        }
        // firstLaunch and expiresAt are NOT in this list. They are two clock
        // readings, they describe no device, and they never leave it -- the
        // marker is local. Keeping them is what stops a consent grant arriving
        // a week later from restarting the attribution window and matching an
        // unrelated click, so dropping them would cost privacy rather than
        // protect it.
        for (String key : new String[] {"platform", "osVersion", "deviceModel",
                "screenWidth", "screenHeight", "locale"}) {
            assertFalse(record.containsKey(key), message + " (held " + key + ")");
        }
    }

    @FormTest
    void anErasureClearsTheReferralDimensionsEvenWithNoProviderRegistered() {
        // The invite provider is the ordinary route and does more -- it drops
        // the durable records too -- but a provider can be absent:
        // Analytics.clearProviders() is public and the deprecated
        // AnalyticsService.init() calls it. In that window an erasure left the
        // reserved dimensions on the new id, and the next provider the app
        // registered transmitted them. An erasure cannot depend on who happens
        // to be registered when it runs.
        InviteTestSupport.freshInstall();
        Invites.handleResolution(InviteTestSupport.resolvedJson("CODE1", "spring", "sms"),
                Invites.MATCH_DIRECT, false);
        assertNotNull(Analytics.getDimensions().get("cn1_campaign"));
        Analytics.setDimension("plan", "pro");

        Analytics.clearProviders();
        Analytics.resetClientId();

        assertNull(Analytics.getDimensions().get("cn1_campaign"),
                "an erasure left the referral dimensions on the new client id");
        assertNull(Analytics.getDimensions().get("cn1_invite_code"));
        assertEquals("pro", Analytics.getDimensions().get("plan"),
                "the application's own dimension must survive an erasure");
    }
}
