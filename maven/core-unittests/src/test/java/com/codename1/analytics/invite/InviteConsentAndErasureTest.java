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
        // Terminal, not STATE_NONE. STATE_NONE is indistinguishable from a
        // fresh install, and that is precisely what let the erasure be undone:
        // the next ordinary checkForInvite() built a new profile and started
        // deferred matching again, and inside the original click window the
        // server can match the same device to the same click and restore the
        // same inviter under the new client id. The tombstone carries a state
        // and a reason and nothing else.
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState());
    }

    @FormTest
    void anerasedInstallDoesNotStartLookingAgainByItself() {
        // The erasure has to survive the next launch, not just the moment it
        // happens. Nothing personal is kept to achieve it -- the marker is a
        // state and a reason -- but the automatic lookup must not restart, or
        // the server can hand the same inviter back under the new identity.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.handleResolution(
                InviteTestSupport.resolvedJson("ABC123", "spring", "sms"),
                Invites.MATCH_REFERRER, true);
        assertNotNull(Invites.getAttribution());

        Analytics.resetClientId();
        implementation.clearQueuedRequests();

        // The next ordinary launch.
        Invites.forgetLoadedState();
        Invites.checkForInvite();

        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState(),
                "an erased install started deferred matching again by itself");
        assertEquals(0, implementation.getQueuedRequests().size(),
                "an erased install sent a fresh device profile to the server");

        // And a NEW invite still reopens it: erasing an identity is not a
        // decision about an invite the person taps afterwards.
        assertTrue(Invites.handleUrl("https://cloud.codenameone.com/i/acme/AFTER1"));
        assertEquals(Invites.STATE_PENDING, Invites.getState(),
                "a direct invite could not reopen attribution after an erasure");
    }

    @FormTest
    void anerasureIsNotReportedDoneWhileTheAttributionSurvives() {
        // Storage.deleteStorageFile reports nothing useful: Android's
        // Context.deleteFile() and JavaSE's File.delete() both return a boolean
        // and neither throws, so a delete that failed looked exactly like one
        // that worked. The caches were cleared regardless, the tombstone was
        // written, and the provider recorded the new client id as fully
        // erased -- while the attribution record was still on the disk, ready
        // to come back on the next launch and report the old referral identity
        // under the new id.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.handleResolution(
                InviteTestSupport.resolvedJson("ABC123", "spring", "sms"),
                Invites.MATCH_REFERRER, true);
        assertNotNull(Invites.getAttribution());

        InviteStore.failNextDeleteForTest(InviteStore.ATTRIBUTION);
        assertFalse(Invites.eraseInternal(),
                "an erasure reported success while the attribution record survived");
    }

    @FormTest
    void anerasureIsNotReportedDoneWhileTheOutboxSurvives() {
        // The outbox holds the queued registration JSON, and that carries the
        // OLD client id along with the campaign, payload and preview. Ignoring
        // its delete result was a hole the size of the whole erasure: the
        // erasure reported success, the provider advanced its baseline, and the
        // next drainOutbox() transmitted a pre-erasure registration under the
        // new identity as soon as storage recovered.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.create(InviteRequest.create().campaign("launch").build());
        assertFalse(InviteStore.readOutbox().isEmpty(), "the fixture queued nothing");

        InviteStore.failNextDeleteForTest(InviteStore.OUTBOX);
        assertFalse(Invites.eraseInternal(),
                "an erasure reported success while the queued registration survived");
    }

    @FormTest
    void asurvivingOutboxIsNotDrainedUntilTheErasureFinishes() {
        // Reporting the failure was not enough on its own. The entries carry
        // the OLD client id, so the next flush would transmit exactly what the
        // erasure was asked to prevent as soon as storage recovered -- an
        // erasure that ends by sending the erased identity to the server.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.create(InviteRequest.create().campaign("launch").build());
        assertFalse(InviteStore.readOutbox().isEmpty(), "the fixture queued nothing");

        InviteStore.failNextDeleteForTest(InviteStore.OUTBOX);
        assertFalse(Invites.eraseInternal(), "the fixture's erasure did not fail");

        implementation.clearQueuedRequests();
        Invites.flush();

        assertEquals(0, implementation.getQueuedRequests().size(),
                "a pre-erasure registration was transmitted after the erasure failed");
        // And the retry inside flush() finished the job, so the queue is gone.
        assertTrue(InviteStore.readOutbox().isEmpty(),
                "the erasure was never retried");
    }

    @FormTest
    void asurvivingCodeIsNotClaimedUnderTheNewIdentity() {
        // The retry gate lived only in drainOutbox(), and the lookup path had
        // none. A PENDING record that outlived its erasure still carried the
        // code a direct link left on the device, and the next checkForInvite()
        // reloaded it and claimed it under the NEW client id -- which is the
        // transmission the erasure existed to prevent, made by the erasure's
        // own aftermath.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.handleUrl("https://cloud.codenameone.com/i/ABC123");
        implementation.clearQueuedRequests();

        InviteStore.failNextDeleteForTest(InviteStore.PENDING);
        assertFalse(Invites.eraseInternal(), "the fixture's erasure did not fail");
        // The record really did survive, or this test proves nothing about the
        // gate: a deleted record cannot be claimed either way.
        assertFalse(InviteStore.read(InviteStore.PENDING) == null
                        || InviteStore.read(InviteStore.PENDING).isEmpty(),
                "the fixture did not leave a surviving record to claim");

        // Storage is still refusing, so the retry inside the gate fails too and
        // nothing may proceed.
        InviteStore.failNextDeleteForTest(InviteStore.PENDING);
        Invites.checkForInvite();

        assertEquals(0, implementation.getQueuedRequests().size(),
                "a code that survived an erasure was claimed under the new identity");
    }

    @FormTest
    void afreshInviteIsNotAppendedToAQueueTheErasureWillDelete() {
        // create() appended to whatever outbox was on the disk. An outbox that
        // survived an erasure is deleted WHOLE by the retry inside the next
        // drain -- which create() itself triggers through flush() -- so the
        // invite just minted went with it. Having reported success, nothing
        // held its code, and isRegistered() answered true about a registration
        // the server was guaranteed never to have seen.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.create(InviteRequest.create().campaign("old").build());
        assertFalse(InviteStore.readOutbox().isEmpty(), "the fixture queued nothing");

        InviteStore.failNextDeleteForTest(InviteStore.OUTBOX);
        assertFalse(Invites.eraseInternal(), "the fixture's erasure did not fail");

        // Storage recovers, which is the case the finding is about: the retry
        // inside create() now succeeds, so the stale queue goes and the new
        // invite is appended to a clean one rather than to a doomed one.
        Invite fresh = Invites.create(InviteRequest.create().campaign("new").build());
        Invites.flush();

        assertFalse(Invites.isRegistered(fresh),
                "an invite that was never acknowledged reported itself registered");
    }

    @FormTest
    void emptyingTheQueueIsVerifiedLikeEveryOtherDelete() {
        // The last acknowledged registration empties the outbox, and that path
        // called deleteStorageFile() and returned success without looking.
        // On a port where the delete silently fails the entry stays durable,
        // so every later flush resends an already acknowledged registration
        // while isRegistered() goes on answering false about it.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.create(InviteRequest.create().campaign("launch").build());
        assertFalse(InviteStore.readOutbox().isEmpty(), "the fixture queued nothing");

        InviteStore.failNextDeleteForTest(InviteStore.OUTBOX);
        assertFalse(InviteStore.writeOutbox(new java.util.ArrayList<String>()),
                "emptying the queue reported success without verifying the delete");

        // And the ordinary case still empties it and says so.
        assertTrue(InviteStore.writeOutbox(new java.util.ArrayList<String>()),
                "emptying the queue failed when the store was willing");
        assertTrue(InviteStore.readOutbox().isEmpty(), "the queue survived");
    }

    @FormTest
    void referralDimensionsWithNoRecordBehindThemAreDropped() {
        // reset() clears the dimensions in memory and asks Preferences to
        // persist that -- and Preferences cannot say whether it did: set()
        // updates a static table and swallows the store's answer, so
        // resetVerified() reported success on the three InviteStore records it
        // CAN verify while the old values stayed on the disk. A plain reset
        // keeps the same client id, so the owner stamp still matched and the
        // next launch loaded the referral straight back and transmitted it.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.handleResolution(
                InviteTestSupport.resolvedJson("GHOST1", "spring", "sms"),
                Invites.MATCH_REFERRER, true);
        assertEquals("spring", Analytics.getDimensions().get(Invites.DIMENSION_CAMPAIGN));

        // The durable record goes; the dimensions are left behind, which is
        // what an unpersisted clear looks like on the next launch.
        assertTrue(InviteStore.delete(InviteStore.ATTRIBUTION));
        Invites.forgetCachedAttributionForTest();
        Invites.forgetDimensionReconciliationForTest();

        Invites.checkForInvite();

        assertNull(Analytics.getDimensions().get(Invites.DIMENSION_CAMPAIGN),
                "a referral with no record behind it was kept and would be transmitted");
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

    @FormTest
    void anErasureDropsTheDurableRecordsEvenWithNoProviderRegistered() {
        // resetClientId clears the reserved dimensions itself, which needs no
        // provider -- but the durable records are ours and only the provider's
        // init hook drops them. Analytics.clearProviders() is public and the
        // deprecated AnalyticsService.init() calls it, so an erasure really can
        // run with the provider absent; every entry point that reads or
        // transmits stored data re-registers first, which re-runs that hook.
        InviteTestSupport.freshInstall();
        Invites.handleResolution(InviteTestSupport.resolvedJson("ERASE1", "spring", "sms"),
                Invites.MATCH_DIRECT, false);
        assertNotNull(Invites.getAttribution());

        Analytics.clearProviders();
        Analytics.resetClientId();

        assertNull(Invites.getAttribution(),
                "the old referral identity survived an erasure and can be read under the new id");
        assertNull(InviteStore.read(InviteStore.ATTRIBUTION),
                "the durable attribution record was left on the device");
    }

    @FormTest
    void grantingConsentRestartsALookupThatSuspensionKilled() {
        // Switching from OPT_OUT to OPT_IN with nothing on record withdraws the
        // mode's implicit allow, so queued requests are killed: they passed the
        // permission gate a moment ago and would transmit after transmission
        // stopped being permitted. Nothing is refused -- the prompt is simply
        // unanswered -- so the lookup stays pending.
        //
        // The kill used to leave lookupIssuedAt stamped, so for the rest of the
        // retry interval the lookup was dead and the state said it was in
        // flight. Granting consent inside that interval then did nothing,
        // because onConsentChanged() will not restart a lookup it believes is
        // already outstanding, and the invite stayed unresolved until an
        // explicit check after the delay or the next launch -- by which time
        // the attribution window may have closed.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Analytics.setConsentMode(ConsentMode.OPT_OUT);
        Analytics.setConsent(null);
        Invites.registerInstallReferrerSource(new InstallReferrerSource() {
            public boolean isSupported() {
                return true;
            }

            public boolean discardReferrer() {
                return true;
            }

            public void requestReferrer(InstallReferrerCallback callback) {
                callback.onReferrer("utm_source=cn1_invite&cn1_invite=SUSPEND1", 0L, 0L);
            }
        });
        Invites.checkForInvite();
        assertEquals(Invites.STATE_PENDING, Invites.getState(),
                "the fixture never got a lookup under way");

        // The withdrawal. The retry interval has NOT elapsed, which is the
        // whole point: this is the window the stale stamp covered.
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        implementation.clearQueuedRequests();

        Analytics.setConsent(AnalyticsConsent.builder().analytics(true).build());

        assertFalse(implementation.getQueuedRequests().isEmpty(),
                "consent was granted while the killed lookup still looked outstanding, "
                        + "so nothing restarted it and the invite stays unresolved until "
                        + "the retry interval elapses or the app is launched again");
    }
}
