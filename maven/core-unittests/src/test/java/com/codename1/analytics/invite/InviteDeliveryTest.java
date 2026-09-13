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
import com.codename1.analytics.ConsentMode;
import com.codename1.analytics.AnalyticsConsent;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InviteDeliveryTest extends UITestBase {

    @AfterEach
    void cleanUp() {
        InviteTestSupport.tearDown();
    }

    private static final class Capture implements InviteListener {
        final List<InviteAttribution> received = new ArrayList<InviteAttribution>();
        final List<String> unavailable = new ArrayList<String>();

        public void inviteReceived(InviteAttribution attribution) {
            received.add(attribution);
        }

        public void attributionUnavailable(String reason) {
            unavailable.add(reason);
        }
    }

    @FormTest
    void anAttributionIsDeliveredExactlyOnce() {
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Capture capture = new Capture();
        Invites.setInviteListener(capture);

        Invites.handleResolution(
                InviteTestSupport.resolvedJson("ABC123xxxxxxxxxxxxxxxx", "spring", "sms"),
                Invites.MATCH_REFERRER, true);

        assertEquals(1, capture.received.size());
        assertEquals("ABC123xxxxxxxxxxxxxxxx", capture.received.get(0).getCode());
        assertTrue(capture.received.get(0).isDeferred());

        // Re-entering the facade, as a later start() would, must not deliver
        // the same attribution a second time.
        Invites.checkForInvite();
        Invites.setInviteListener(capture);
        assertEquals(1, capture.received.size(), "the attribution was delivered twice");
    }

    @FormTest
    void anAnswerThatArrivesBeforeTheListenerIsHeldAndDeliveredOnRegistration() {
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);

        // A cold launch from a link resolves before the application has run
        // start(), so the answer has to wait rather than be dropped.
        Invites.handleResolution(
                InviteTestSupport.resolvedJson("ABC123xxxxxxxxxxxxxxxx", "spring", "sms"),
                Invites.MATCH_REFERRER, true);

        Capture capture = new Capture();
        Invites.setInviteListener(capture);

        assertEquals(1, capture.received.size(), "the held attribution was never delivered");
        assertEquals("ABC123xxxxxxxxxxxxxxxx", capture.received.get(0).getCode());
    }

    @FormTest
    void aZeroWindowSwitchesDeferredAttributionOff() {
        InviteTestSupport.freshInstall();
        implementation.clearQueuedRequests();
        implementation.setAutoProcessConnections(false);
        Capture capture = new Capture();
        Invites.setInviteListener(capture);
        Invites.setAttributionWindow(0);

        Invites.checkForInvite();

        assertEquals(0, implementation.getQueuedRequests().size(),
                "the documented kill switch still sent something");
        assertEquals(1, capture.unavailable.size());
        assertEquals(Invites.REASON_UNSUPPORTED, capture.unavailable.get(0));
    }

    @FormTest
    void aStoreReferrerResolvesDeterministically() {
        InviteTestSupport.freshInstall();
        implementation.clearQueuedRequests();
        implementation.setAutoProcessConnections(false);
        Invites.registerInstallReferrerSource(new InstallReferrerSource() {
            public boolean isSupported() {
                return true;
            }

            public boolean discardReferrer() {
                return true;
            }

            public void requestReferrer(InstallReferrerCallback callback) {
                callback.onReferrer(
                        "utm_source=cn1_invite&utm_medium=referral&cn1_invite=ABC123xxxxxxxxxxxxxxxx",
                        1700000000L, 1700000060L);
            }
        });

        Invites.checkForInvite();

        // The deterministic path posts a claim carrying the code itself, and
        // never the statistical match.
        boolean sawClaim = false;
        for (int i = 0; i < implementation.getQueuedRequests().size(); i++) {
            String url = implementation.getQueuedRequests().get(i).getUrl();
            assertTrue(!url.endsWith("/invites/match"),
                    "a device with a store referrer must not be fingerprinted");
            if (url.endsWith("/invites/claim")) {
                sawClaim = true;
                assertTrue(implementation.getQueuedRequests().get(i)
                        .getRequestBody().contains("ABC123xxxxxxxxxxxxxxxx"));
            }
        }
        assertTrue(sawClaim, "expected a deterministic claim");
    }

    @FormTest
    void noStoreReferrerFallsBackToTheAppClipHandoff() {
        // The Android store answered "no referral", so the deferred lookup asks
        // the other exact source: the code an iOS App Clip left in the
        // container it shares with this application.
        //
        // It used to fall back to a statistical match -- a coarse device
        // profile posted to the server, matched against a hashed click within
        // an hour. Nothing is posted now and nothing about the device is read,
        // which is why this asserts on the SOURCE rather than on a request
        // body: there is no request.
        InviteTestSupport.freshInstall();
        implementation.clearQueuedRequests();
        implementation.setAutoProcessConnections(false);
        Invites.registerInstallReferrerSource(new InstallReferrerSource() {
            public boolean isSupported() {
                return true;
            }

            public boolean discardReferrer() {
                return true;
            }

            public void requestReferrer(InstallReferrerCallback callback) {
                callback.onUnavailable(Invites.REASON_NO_MATCH);
            }
        });

        Invites.checkForInvite();

        assertTrue(InviteTestSupport.pendingHandoff.wasAsked(),
                "the referrer came back empty and nothing asked the App Clip");
        for (int i = 0; i < implementation.getQueuedRequests().size(); i++) {
            assertTrue(!implementation.getQueuedRequests().get(i).getUrl().endsWith("/match"),
                    "a statistical match was still posted to the server");
        }

        // And the code the clip hands over is claimed exactly, like a referrer.
        InviteTestSupport.pendingHandoff.answer("CLIP123");
        boolean sawClaim = false;
        for (int i = 0; i < implementation.getQueuedRequests().size(); i++) {
            if (implementation.getQueuedRequests().get(i).getUrl().endsWith("/invites/claim")) {
                String body = implementation.getQueuedRequests().get(i).getRequestBody();
                if (body != null && body.contains("CLIP123")) {
                    sawClaim = true;
                    assertTrue(body.contains("app_clip"), body);
                    // Nothing about the device goes with it.
                    assertTrue(!body.contains("osVersion"), body);
                    assertTrue(!body.contains("deviceModel"), body);
                }
            }
        }
        assertTrue(sawClaim, "the App Clip's code was never claimed");
    }

    @FormTest
    void anInstallWithNoClipHandoffIsNotInvited() {
        // The overwhelmingly common case: somebody installed the application
        // without ever tapping an invite. The clip answers that it has nothing,
        // and that is a real and permanent answer about this install -- not
        // "unsupported", which is the reopenable marker the kill switch writes
        // and would have every launch ask again for something that can never be
        // there.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        final String[] told = new String[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
            }

            public void attributionUnavailable(String reason) {
                told[0] = reason;
            }
        });

        Invites.checkForInvite();
        InviteTestSupport.pendingHandoff.answerNothing(Invites.REASON_NO_MATCH);

        assertEquals(Invites.REASON_NO_MATCH, told[0]);
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState());
    }

    @FormTest
    void aplatformWithNoAppClipSettlesRatherThanWaiting() {
        // No clip source at all -- the desktop, the simulator, an iOS build
        // without a clip, or Android once the referrer has already answered.
        // Settling immediately is what keeps the listener's contract: exactly
        // one answer per install, and this install's answer is "no invite".
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.registerAppClipHandoffSource(null);
        final int[] told = new int[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
            }

            public void attributionUnavailable(String reason) {
                told[0]++;
            }
        });

        Invites.checkForInvite();

        assertEquals(1, told[0], "a platform with no App Clip left the listener waiting");
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState());
    }

    @FormTest
    void aclipCodeIsWrittenDownBeforeItIsSent() {
        // The claim is one fail-silent request, and a fresh install is exactly
        // when the device is most likely to be offline. The clip has already
        // cleared its own copy by the time it answers, so a code that lived
        // only in the callback was gone for good the moment that request
        // failed.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);

        Invites.checkForInvite();
        InviteTestSupport.pendingHandoff.answer("CLIPSAVE");

        Map<String, String> record = InviteStore.read(InviteStore.PENDING);
        assertNotNull(record);
        assertEquals("CLIPSAVE", InviteStore.get(record, "code", null),
                "the clip's code was never written down, so a failed claim loses it");
        assertEquals(Invites.MATCH_APP_CLIP, InviteStore.get(record, "codeMatch", null),
                "the saved code lost its provenance");
    }

    @FormTest
    void theclipsTapTimeSurvivesIntoTheClaim() {
        // The clip is the only witness to the tap: iOS resolves a clip
        // invocation from the association file, so it never reaches the
        // redirect, and the native side clears the handoff as it reads it.
        // Dropped here the value is gone, and every App Clip attribution
        // reports a click time of zero.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        implementation.clearQueuedRequests();

        long tappedSeconds = System.currentTimeMillis() / 1000L - 600L;
        Invites.checkForInvite();
        InviteTestSupport.pendingHandoff.answer("CLIPTIME", tappedSeconds);

        // Written down, because the claim can fail and be resent from the
        // record rather than from the callback.
        Map<String, String> record = InviteStore.read(InviteStore.PENDING);
        assertNotNull(record);
        assertEquals(tappedSeconds * 1000L,
                InviteStore.getLong(record, "codeClicked", 0),
                "the tap time was not persisted, so a resent claim loses it");

        // And it is on the wire, in milliseconds.
        List sent = implementation.getQueuedRequests();
        assertFalse(sent.isEmpty(), "no claim was sent at all");
        String body = ((com.codename1.io.ConnectionRequest)
                sent.get(sent.size() - 1)).getRequestBody();
        // A bare number, not a quoted one. Asserted because the server binds
        // it to a long: a string would still coerce today and would stop
        // doing so the moment anything there gets stricter.
        assertTrue(body.contains("\"clickedMillis\": " + (tappedSeconds * 1000L)),
                "the claim did not carry the tap time as a number: " + body);
    }

    @FormTest
    void theclipsTapTimeSurvivesAConsentRefusal() {
        // A refusal is reopenable, so the code survives it -- and the tap time
        // has to travel with the code. An App Clip invocation never reaches
        // the redirect, so the clip is the only witness, and it cleared its
        // own copy as it was read. Dropped from the marker, a
        // withdraw-then-grant cycle resends the claim with a zero time that
        // nothing can recover.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(AnalyticsConsent.builder().analytics(true).build());

        // The clip answers while consent stands, so the code and its tap time
        // are persisted and the claim goes out.
        long tappedSeconds = System.currentTimeMillis() / 1000L - 900L;
        Invites.checkForInvite();
        InviteTestSupport.pendingHandoff.answer("CLIPDENY", tappedSeconds);
        assertEquals(tappedSeconds * 1000L,
                InviteStore.getLong(InviteStore.read(InviteStore.PENDING), "codeClicked", 0),
                "the fixture never persisted a tap time");

        // Consent is withdrawn before the claim resolves, which writes the
        // reopenable DECLINED marker.
        Analytics.setConsent(AnalyticsConsent.builder().analytics(false).build());

        Map<String, String> marker = InviteStore.read(InviteStore.PENDING);
        assertNotNull(marker, "the refusal left no marker");
        assertEquals("CLIPDENY", InviteStore.get(marker, "code", null),
                "the fixture did not reach the reopenable marker");
        assertEquals(tappedSeconds * 1000L,
                InviteStore.getLong(marker, "codeClicked", 0),
                "the tap time did not survive the consent refusal");
    }

    @FormTest
    void theclipHandoffReadIsNotChargedAsANetworkAttempt() {
        // The claim bumps the counter itself. Charging the local handoff read
        // too started the first network claim at 2, so the install settled
        // terminal after four requests instead of the five MAX_ATTEMPTS
        // promises -- and the referrer path, which never bumped here, got its
        // full budget.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.checkForInvite();
        InviteTestSupport.pendingHandoff.answer("CLIPBUDGET");

        Map<String, String> record = InviteStore.read(InviteStore.PENDING);
        assertNotNull(record);
        assertEquals(1, InviteStore.getInt(record, "attempts", 0),
                "the handoff read and its claim were both charged");
    }

    @FormTest
    void aclipAnswerThatOutlivedItsLookupIsIgnored() {
        // The read is asynchronous and everything that supersedes a lookup
        // bumps the epoch. A direct link arriving while the clip read is
        // outstanding is the case that shows it: the link is an exact answer
        // about THIS install, and a clip code read before it must not overwrite
        // the record it just wrote.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);

        Invites.checkForInvite();
        assertTrue(InviteTestSupport.pendingHandoff.wasAsked());

        // A link is tapped while the clip read is still outstanding.
        assertTrue(Invites.handleUrl("https://cloud.codenameone.com/i/acme/DIRECTWINSxxxxxxxxxxxx"));
        assertEquals("DIRECTWINSxxxxxxxxxxxx",
                InviteStore.get(InviteStore.read(InviteStore.PENDING), "code", null));

        // The clip finally answers, with something else.
        InviteTestSupport.pendingHandoff.answer("STALECLIP");

        assertEquals("DIRECTWINSxxxxxxxxxxxx",
                InviteStore.get(InviteStore.read(InviteStore.PENDING), "code", null),
                "a clip answer from before the link overwrote the newer exact claim");
    }

    @FormTest
    void aSecondLinkDoesNotRewriteTheFirstTouchCohort() {
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.handleResolution(
                InviteTestSupport.resolvedJson("FIRST", "spring", "sms"),
                Invites.MATCH_REFERRER, true);

        Invites.handleUrl("https://cloud.codenameone.com/i/SECONDxxxxxxxxxxxxxxxx");

        InviteAttribution a = Invites.getAttribution();
        assertNotNull(a);
        assertEquals("FIRST", a.getCode(),
                "rewriting the cohort mid-stream makes lifetime value unjoinable");
    }
}
