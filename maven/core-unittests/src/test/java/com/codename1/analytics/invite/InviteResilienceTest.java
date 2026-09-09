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

import com.codename1.junit.EdtTest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import com.codename1.junit.UITestBase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What happens when the server, the storage or the process does not cooperate.
 * Every case here is a way the feature used to lose data quietly rather than
 * loudly, so each one asserts the durable record rather than a return value.
 */
class InviteResilienceTest extends UITestBase {

    @BeforeEach
    void setUp() {
        InviteTestSupport.freshInstall();
    }

    @AfterEach
    void tearDown() {
        InviteTestSupport.tearDown();
    }

    @Test
    @EdtTest
    void aServerErrorDoesNotRetireTheRegistration() {
        // ConnectionRequest reads error bodies by default and then runs the
        // ordinary success path over them, so a 503 reached postResponse()
        // exactly as a 200 did and the outbox entry -- the only durable copy of
        // the campaign, channel, payload and preview of a link already handed
        // out -- was discarded as though the server had accepted it.
        String entry = "{\"code\":\"abc123\",\"campaign\":\"launch\"}";
        InviteStore.writeOutbox(new ArrayList<String>(Arrays.asList(entry)));

        Invites.InviteConnection req = new Invites.InviteConnection(
                Invites.MATCH_DIRECT, false, true, entry, 0);
        req.handleErrorResponseCode(503, "Service Unavailable");
        req.postResponse();

        assertEquals(Arrays.asList(entry), InviteStore.readOutbox(),
                "a 503 retired the registration as though it had been accepted");
    }

    @Test
    @EdtTest
    void aServerErrorIsNotReadAsAnOrganicInstall() {
        // The same fall-through, on the lookup side: an error body parses to
        // nothing that says "resolved", which the resolution path treats as a
        // terminal "you were not invited" -- turning one bad minute on the
        // server into a permanent wrong answer on the device.
        Invites.checkForInvite();
        Invites.InviteConnection req = new Invites.InviteConnection(
                Invites.MATCH_DIRECT, true, false, null, Invites.currentLookupEpochForTest());
        req.handleErrorResponseCode(500, "Internal Server Error");
        // The error body really is read -- that is the whole mechanism -- so
        // the test has to supply one. An error page carries no "resolved", and
        // the resolution path reads that as a settled negative.
        try {
            req.readResponse(new ByteArrayInputStream(
                    "{\"error\":\"upstream unavailable\"}".getBytes("UTF-8")));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        req.postResponse();

        assertFalse(Invites.getState() == Invites.STATE_NONE_FOUND,
                "a server fault was recorded as a settled negative answer");
    }

    @Test
    @EdtTest
    void aNoMatchAnswerIsNotAskedAgainOnTheNextLaunch() {
        // Deleting the pending record was not enough: an absent record reads
        // back as STATE_NONE, so the next launch built a fresh profile and
        // queried again, and an ordinary uninvited install kept contacting the
        // server for ever.
        Invites.checkForInvite();
        Invites.handleResolution("{\"resolved\":false}", Invites.MATCH_FINGERPRINT, true);
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState());

        Invites.forgetLoadedState();
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState(),
                "the terminal answer did not survive a relaunch");
    }

    @Test
    @EdtTest
    void theTerminalMarkerKeepsNoDeviceProfile() {
        // It is durable and it is empty: the profile existed to be matched,
        // and there is nothing left to match it against.
        Invites.checkForInvite();
        Invites.handleResolution("{\"resolved\":false}", Invites.MATCH_FINGERPRINT, true);
        Map<String, String> marker = InviteStore.read(InviteStore.PENDING);
        assertNotNull(marker, "the answer has to be durable");
        assertEquals(1, marker.size(), "the marker kept fields beyond the state: " + marker);
        assertTrue(marker.containsKey("state"));
    }

    @Test
    @EdtTest
    void aPendingReattributionSurvivesAProcessRestart() {
        // With re-attribution on, a later invite writes a new claim while the
        // earlier attribution still stands. Answering STATE_RESOLVED from the
        // old attribution made beginDeferred() return, so a claim interrupted
        // by process death was never retried and last touch lost to first.
        Invites.handleResolution(InviteTestSupport.resolvedJson("first", "c1", "sms"),
                Invites.MATCH_DIRECT, false);
        assertEquals(Invites.STATE_RESOLVED, Invites.getState());

        Invites.setReattribution(true);
        Invites.handleUrl("https://cloud.codenameone.com/i/acme/second");
        Invites.forgetLoadedState();

        assertEquals(Invites.STATE_PENDING, Invites.getState(),
                "the pending re-attribution claim was lost behind the old attribution");
    }

    @Test
    @EdtTest
    void withoutReattributionAStalePendingRecordDoesNotReopenAnAttribution() {
        // The other half of the same rule, and the reason the check is scoped:
        // first touch stands, so a leftover pending record must never unsettle
        // an attribution that has already resolved.
        Invites.checkForInvite();
        Invites.handleResolution(InviteTestSupport.resolvedJson("first", "c1", "sms"),
                Invites.MATCH_DIRECT, false);
        Invites.forgetLoadedState();
        assertEquals(Invites.STATE_RESOLVED, Invites.getState());
    }

    @Test
    @EdtTest
    void aFailedOutboxWriteIsReportedRatherThanSwallowed() {
        // The caller has to be able to tell, because an entry that never
        // reached the outbox is a registration nothing can reconstruct.
        List<String> ok = new ArrayList<String>(Arrays.asList("{\"code\":\"a\"}"));
        assertTrue(InviteStore.writeOutbox(ok), "a healthy store must report success");
        assertEquals(ok, InviteStore.readOutbox());
    }

    @Test
    @EdtTest
    void aDisabledAttributionWindowIsAnsweredOnceAndSurvivesARelaunch() {
        // setState() only rewrites a record that already exists, and on a fresh
        // install none does -- so this answer lived only in memory and the
        // listener heard it again on every launch.
        Invites.setAttributionWindow(0);
        final int[] told = new int[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
            }

            public void attributionUnavailable(String reason) {
                told[0]++;
            }
        });
        Invites.checkForInvite();
        assertEquals(1, told[0]);
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState());

        Invites.forgetLoadedState();
        Invites.checkForInvite();
        assertEquals(1, told[0], "the listener was told again after a relaunch");
    }

    @Test
    @EdtTest
    void reenablingTheWindowReopensThatOneTerminalMarker() {
        // The disabled-window marker is the only terminal answer that can stop
        // being true, so it is the only one that is reopened. An application
        // that ships a non-zero window later is asking for attribution again.
        Invites.setAttributionWindow(0);
        Invites.checkForInvite();
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState());

        Invites.setAttributionWindow(Invites.DEFAULT_ATTRIBUTION_WINDOW);
        Invites.forgetLoadedState();
        Invites.checkForInvite();
        assertEquals(Invites.STATE_PENDING, Invites.getState(),
                "a re-enabled window did not reopen the lookup");
    }

    @Test
    @EdtTest
    void aNoMatchDoesNotSettleTheInstallWhileAReferrerRetryIsOutstanding() {
        // The Play referrer failed transiently, so the source deliberately left
        // its once-only flag unset and a later launch can still read the exact
        // referrer. Settling the install as organic on the statistical
        // fallback's answer would throw that deterministic result away.
        Invites.checkForInvite();
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        assertNotNull(pending);
        pending.put("referrerRetry", "true");
        InviteStore.write(InviteStore.PENDING, pending);

        Invites.handleResolution("{\"resolved\":false}", Invites.MATCH_FINGERPRINT, true);

        Invites.forgetLoadedState();
        assertEquals(Invites.STATE_PENDING, Invites.getState(),
                "a transient store outage settled the install as organic");
    }

    @Test
    @EdtTest
    void aDirectlySentRegistrationIsNotRegisteredUntilItIsAcknowledged() {
        // Absence from the outbox is not acknowledgement. When the store cannot
        // be written the registration is sent directly and never queued, so the
        // outbox says nothing about it -- and reading that silence as success
        // reported an in-flight, possibly failed, registration as acknowledged.
        Invite invite = Invites.create(InviteRequest.create().campaign("launch").build());
        assertNotNull(invite);
        // Exactly what a failed enqueue leaves behind: nothing in the durable
        // queue, and a request on the wire. The outbox is emptied to stand in
        // for the write that did not happen.
        InviteStore.writeOutbox(new ArrayList<String>());
        Invites.markSentDirectlyForTest(invite.getCode());
        assertFalse(Invites.isRegistered(invite),
                "an unacknowledged direct send reported itself as registered");
    }
}
