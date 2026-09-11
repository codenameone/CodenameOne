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
import com.codename1.io.ConnectionRequest;
import com.codename1.security.Hash;
import com.codename1.util.Base64;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InviteMintTest extends UITestBase {

    @AfterEach
    void cleanUp() {
        InviteTestSupport.tearDown();
    }

    @FormTest
    void createReturnsUsableInviteWithNoNetwork() {
        InviteTestSupport.freshInstall();
        implementation.clearQueuedRequests();
        implementation.setAutoProcessConnections(false);

        Invite invite = Invites.create(InviteRequest.create()
                .campaign("spring").channel("sms").build());

        // The whole point of minting on the device: an invite is shareable the
        // instant it is asked for, on a plane, in a queue, at a conference.
        assertNotNull(invite);
        assertNotNull(invite.getCode());
        assertTrue(invite.getUrl().startsWith("https://"), invite.getUrl());
        assertTrue(invite.getUrl().endsWith("/i/" + invite.getCode()), invite.getUrl());
        assertEquals("spring", invite.getCampaign());
        assertEquals("sms", invite.getChannel());
        assertFalse(Invites.isRegistered(invite),
                "nothing has acknowledged it yet");
    }

    @FormTest
    void codesAreUrlSafeAndDistinct() {
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < 200; i++) {
            String code = Invites.create(InviteRequest.create().build()).getCode();
            assertTrue(seen.add(code), "duplicate code " + code);
            for (int j = 0; j < code.length(); j++) {
                char c = code.charAt(j);
                boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                        || (c >= '0' && c <= '9') || c == '-' || c == '_';
                assertTrue(ok, "code is not url safe: " + code);
            }
        }
    }

    @FormTest
    void createQueuesRegistrationCarryingTheCodeAndIdentity() {
        InviteTestSupport.freshInstall();
        implementation.clearQueuedRequests();
        implementation.setAutoProcessConnections(false);

        Invite invite = Invites.create(InviteRequest.create()
                .campaign("spring").channel("sms").payload("room-42").build());

        List<ConnectionRequest> requests = implementation.getQueuedRequests();
        assertEquals(1, requests.size(), "expected exactly one registration post");
        ConnectionRequest r = requests.get(0);
        assertTrue(r.getUrl().endsWith("/api/v2/analytics/invites"), r.getUrl());
        assertTrue(r.isPost());
        String body = r.getRequestBody();
        assertTrue(body.contains(invite.getCode()), body);
        // mapToJson pretty prints, so compare with the whitespace removed
        // rather than pinning the exact rendering.
        String compact = body.replace(" ", "").replace("\n", "");
        assertTrue(compact.contains("\"campaign\":\"spring\""), body);
        assertTrue(compact.contains("\"channel\":\"sms\""), body);
        assertTrue(compact.contains("\"payload\":\"room-42\""), body);
        assertTrue(compact.contains("\"clientId\":"), body);
    }

    @FormTest
    void anUnacknowledgedRegistrationStaysInTheOutboxAndIsRetried() {
        InviteTestSupport.freshInstall();
        implementation.clearQueuedRequests();
        implementation.setAutoProcessConnections(false);

        Invite invite = Invites.create(InviteRequest.create().campaign("spring").build());
        assertEquals(1, implementation.getQueuedRequests().size());

        // Nothing has answered, so the entry must survive: the registration
        // carries the campaign and payload, and a click cannot reconstruct
        // them. The offline mint is exactly the case this protects.
        implementation.clearQueuedRequests();
        Invites.flush();

        List<ConnectionRequest> retried = implementation.getQueuedRequests();
        boolean reposted = false;
        for (ConnectionRequest r : retried) {
            if (r.getUrl().endsWith("/api/v2/analytics/invites")
                    && r.getRequestBody().contains(invite.getCode())) {
                reposted = true;
            }
        }
        assertTrue(reposted, "an unacknowledged registration was dropped");
        assertFalse(Invites.isRegistered(invite));
    }

    @FormTest
    void createEmitsInviteCreatedUnderTheReferralCategory() {
        RecordingProvider recorder = InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);

        Invite invite = Invites.create(InviteRequest.create().campaign("spring").build());

        AnalyticsEvent e = recorder.first("invite_created");
        assertNotNull(e, "expected invite_created, saw " + recorder.names());
        assertEquals(Invites.CATEGORY, e.getCategory());
        assertEquals(invite.getCode(), e.getParameters().get("invite_code"));
        assertEquals("spring", e.getParameters().get("campaign"));
    }

    @FormTest
    void linkBaseIsOverridable() {
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.setLinkBase("https://links.example.com/");

        Invite invite = Invites.create(InviteRequest.create().build());

        // The trailing slash on the configured base must not survive into the
        // url, or every link would carry a double slash.
        assertEquals("https://links.example.com/i/" + invite.getCode(), invite.getUrl());
    }

    @FormTest
    void builderRejectsBadInputAtTheCallTheDeveloperCanSee() {
        StringBuilder tooLong = new StringBuilder();
        for (int i = 0; i <= InviteRequest.MAX_PAYLOAD_LENGTH; i++) {
            tooLong.append('x');
        }
        final String payload = tooLong.toString();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() {
                        InviteRequest.create().payload(payload).build();
                    }
                });
        assertTrue(e.getMessage().contains("payload"), e.getMessage());

        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() {
                        InviteRequest.create().campaign("spring sale!").build();
                    }
                });
        assertTrue(e2.getMessage().contains("campaign"), e2.getMessage());
    }
    @FormTest
    void aburstOfInvitesSendsOneRequestEach() {
        // Entries leave the outbox only when their OWN response acknowledges
        // them, which is right -- the campaign, channel, payload and preview
        // cannot be reconstructed from a click -- but it leaves an entry
        // drainable while its request is outstanding. create() calls flush()
        // unconditionally, so a burst reposted the whole queue each time: N
        // invites produced N(N+1)/2 requests, and the 512-entry cap puts that
        // past 131,000 for a full queue.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        implementation.clearQueuedRequests();

        int burst = 6;
        for (int i = 0; i < burst; i++) {
            Invites.create(InviteRequest.create().campaign("c" + i).build());
        }

        assertEquals(burst, implementation.getQueuedRequests().size(),
                "a burst of " + burst + " invites did not send one request each");
    }

    @FormTest
    void theCodeIsTheTruncatedDigestOfTheProof() {
        // The CONTRACT with the server, pinned on both sides against the same
        // vector. The server accepts a registration for an unknown code only
        // when the proof digests to it, so a disagreement about the digest,
        // the alphabet, the padding or the truncation refuses every mint --
        // safe, and not a failure anybody would enjoy diagnosing from either
        // repository alone. InviteService.provesCreation has the twin of this.
        byte[] secret = new byte[16];
        for (int i = 0; i < secret.length; i++) {
            secret[i] = (byte) (i + 1);
        }
        String proof = Base64.encodeUrlSafe(secret);
        String digest = Base64.encodeUrlSafe(Hash.sha256(secret));

        assertEquals("AQIDBAUGBwgJCgsMDQ4PEA", proof,
                "the proof encoding drifted from the one the server decodes");
        assertEquals("Xfur7t8xi_M8CSfEPXYw9R", digest.substring(0, 22),
                "the code derivation drifted from the one the server verifies");
    }

    @FormTest
    void aMintedCodeIsNotItsOwnProof() {
        // The whole point: the code is public -- it is in the share url -- and
        // must not be enough to register itself. Before this, an invite shared
        // while its registration sat in the offline outbox could be registered
        // by whoever was sent the link, and every install and payout on it
        // went to them.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);

        Invite invite = Invites.create(InviteRequest.create().campaign("spring").build());

        assertEquals(22, invite.getCode().length(),
                "the code length changed, which the server truncates to");
        boolean carriesProof = false;
        for (ConnectionRequest r : implementation.getQueuedRequests()) {
            String body = r.getRequestBody();
            if (body != null && body.contains("\"proof\"")) {
                carriesProof = true;
                assertFalse(body.contains("\"proof\":\"" + invite.getCode() + "\""),
                        "the proof is the code, so anyone holding the link can register it");
            }
        }
        assertTrue(carriesProof,
                "the registration carried no proof, so the server cannot tell the "
                        + "minter from anyone who was sent the link");
        assertFalse(invite.getUrl().contains("AQIDBAUGBwgJCgsMDQ4PEA"),
                "the url carries a proof");
    }

    @FormTest
    void anInviteCannotCarryUnboundedFieldsIntoTheOutbox() {
        // The registration json is persisted in the outbox BEFORE anything is
        // sent and before any server sees it. The outbox caps its entry COUNT,
        // which bounds nothing if one entry can be any size -- so an
        // unbounded image address, or a parameter map built in a loop, went
        // straight to storage. Payload, title and description were already
        // refused at build(); these two were the way past all of them.
        StringBuilder huge = new StringBuilder("https://example.com/");
        for (int i = 0; i < InviteRequest.MAX_IMAGE_URL_LENGTH; i++) {
            huge.append('x');
        }
        try {
            InviteRequest.create().imageUrl(huge.toString()).build();
            fail("an image address longer than the limit was accepted and would be "
                    + "written to storage");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("imageUrl"), expected.getMessage());
        }

        InviteRequest.Builder many = InviteRequest.create();
        for (int i = 0; i <= InviteRequest.MAX_PARAMETERS; i++) {
            many.param("k" + i, "v");
        }
        try {
            many.build();
            fail("an unbounded parameter map was accepted");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("parameters"), expected.getMessage());
        }

        StringBuilder bigValue = new StringBuilder();
        for (int i = 0; i <= InviteRequest.MAX_PARAM_VALUE_LENGTH; i++) {
            bigValue.append('y');
        }
        try {
            InviteRequest.create().param("note", bigValue.toString()).build();
            fail("an unbounded parameter value was accepted");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("note"), expected.getMessage());
        }
    }

    @FormTest
    void anInviteWithinTheLimitsIsStillAccepted() {
        // The bound must not refuse the ordinary case it exists to cap.
        InviteRequest r = InviteRequest.create()
                .imageUrl("https://example.com/preview.png")
                .param("tier", "gold")
                .build();
        assertNotNull(r, "an ordinary invite was refused by the new bounds");
    }
}
