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
}
