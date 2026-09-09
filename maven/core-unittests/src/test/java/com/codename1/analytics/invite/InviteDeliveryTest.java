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

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
                InviteTestSupport.resolvedJson("ABC123", "spring", "sms"),
                Invites.MATCH_REFERRER, true);

        assertEquals(1, capture.received.size());
        assertEquals("ABC123", capture.received.get(0).getCode());
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
                InviteTestSupport.resolvedJson("ABC123", "spring", "sms"),
                Invites.MATCH_REFERRER, true);

        Capture capture = new Capture();
        Invites.setInviteListener(capture);

        assertEquals(1, capture.received.size(), "the held attribution was never delivered");
        assertEquals("ABC123", capture.received.get(0).getCode());
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

            public void requestReferrer(InstallReferrerCallback callback) {
                callback.onReferrer(
                        "utm_source=cn1_invite&utm_medium=referral&cn1_invite=ABC123",
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
                        .getRequestBody().contains("ABC123"));
            }
        }
        assertTrue(sawClaim, "expected a deterministic claim");
    }

    @FormTest
    void noStoreReferrerFallsBackToTheStatisticalMatch() {
        InviteTestSupport.freshInstall();
        implementation.clearQueuedRequests();
        implementation.setAutoProcessConnections(false);
        Invites.registerInstallReferrerSource(new InstallReferrerSource() {
            public boolean isSupported() {
                return true;
            }

            public void requestReferrer(InstallReferrerCallback callback) {
                callback.onUnavailable(Invites.REASON_NO_MATCH);
            }
        });

        Invites.checkForInvite();

        boolean sawMatch = false;
        for (int i = 0; i < implementation.getQueuedRequests().size(); i++) {
            if (implementation.getQueuedRequests().get(i).getUrl().endsWith("/invites/match")) {
                sawMatch = true;
                String body = implementation.getQueuedRequests().get(i).getRequestBody();
                // The server reads the address off the socket; the client must
                // never try to enumerate it.
                assertTrue(!body.contains("\"ip\""), body);
                assertTrue(body.contains("osVersion"), body);
                assertTrue(body.contains("deviceModel"), body);
            }
        }
        assertTrue(sawMatch, "expected the statistical match as the fallback");
    }

    @FormTest
    void aSecondLinkDoesNotRewriteTheFirstTouchCohort() {
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.handleResolution(
                InviteTestSupport.resolvedJson("FIRST", "spring", "sms"),
                Invites.MATCH_REFERRER, true);

        Invites.handleUrl("https://cloud.codenameone.com/i/SECOND");

        InviteAttribution a = Invites.getAttribution();
        assertNotNull(a);
        assertEquals("FIRST", a.getCode(),
                "rewriting the cohort mid-stream makes lifetime value unjoinable");
    }
}
