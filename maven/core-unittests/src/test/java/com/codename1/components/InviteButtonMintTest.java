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
package com.codename1.components;

import com.codename1.analytics.Analytics;
import com.codename1.analytics.AnalyticsConsent;
import com.codename1.analytics.ConsentMode;
import com.codename1.analytics.invite.Invite;
import com.codename1.analytics.invite.Invites;
import com.codename1.junit.FormTest;
import com.codename1.share.ShareResult;
import com.codename1.ui.events.ActionEvent;
import com.codename1.share.ShareResultListener;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A press mints the invite it is about to share, and a second press before the
 * first sheet has answered must not mint another.
 */
public class InviteButtonMintTest extends UITestBase {

    @FormTest
    void aSecondPressBeforeTheFirstResultSharesTheSameInvite() {
        // The share sheet is modal, so a double tap does not open two of them.
        // Minting on every press left the first code registered, counted as
        // invite_created, and shared by nobody -- and the result, when it
        // arrived, was reported against whichever invite the field held by
        // then rather than the one whose url went into the sheet.
        Invites.reset();
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(AnalyticsConsent.builder().analytics(true).build());
        InviteButton button = new InviteButton("Invite a friend");
        button.setCampaign("spring");

        Invite first = button.mintForShare();
        Invite second = button.mintForShare();

        assertNotNull(first, "the first press minted nothing");
        assertSame(first, second,
                "a second press minted another invite, so one of them is shared by "
                        + "nobody and the result can be reported against the wrong code");
    }

    @FormTest
    void theInviteSurvivesTheResultSoTheAppCanCorrelateIt() {
        // getInvite() is the application's only way to tell which invite a
        // ShareResult belongs to, and its contract is "the invite minted for
        // the most recent press". Clearing the field when the result arrived
        // -- to stop the next press reporting the same outcome twice -- made
        // it answer null from inside the application's own listener, which is
        // the single moment it has to be right.
        Invites.reset();
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(AnalyticsConsent.builder().analytics(true).build());
        InviteButton button = new InviteButton("Invite a friend");

        Invite shared = button.mintForShare();
        final Invite[] seenByTheApp = new Invite[1];
        button.setShareResultListener(new ShareResultListener() {
            @Override
            public void onResult(ShareResult result) {
                seenByTheApp[0] = button.getInvite();
            }
        });
        button.chain.onResult(ShareResult.sharedTo("com.example.chat"));

        assertSame(shared, seenByTheApp[0],
                "getInvite() answered null inside the app's listener, so the app "
                        + "cannot tell which invite the ShareResult belongs to");
        assertSame(shared, button.getInvite(),
                "getInvite() stopped reporting the most recent press after the result");
    }

    @FormTest
    void theNextPressAfterAResultMintsAFreshInvite() {
        // The other half of the same split: the outstanding mark must clear on
        // the result even though getInvite() keeps answering, or a press after
        // a completed share would re-share the code that was already sent and
        // report its outcome a second time.
        Invites.reset();
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(AnalyticsConsent.builder().analytics(true).build());
        InviteButton button = new InviteButton("Invite a friend");

        Invite first = button.mintForShare();
        button.chain.onResult(ShareResult.sharedTo("com.example.chat"));
        Invite second = button.mintForShare();

        assertNotNull(second, "the press after a completed share minted nothing");
        assertNotSame(first, second,
                "a press after the sheet had already answered re-shared the invite "
                        + "that was just sent, so its outcome is reported twice");
    }

    @FormTest
    void aSecondPressWhileTheSheetIsOutstandingDoesNothing() {
        // Reusing the code was only half of it. ShareButton defers to the next
        // EDT cycle and then shares unconditionally, so two presses inside one
        // cycle still enqueued two presentations: two native sheets attempted,
        // the app's listener called twice, and -- because the first result
        // takes the outstanding mark -- the SECOND share reported to nobody.
        // A real share missing from the funnel is the part the reuse caused.
        Invites.reset();
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(AnalyticsConsent.builder().analytics(true).build());
        final int[] presented = new int[1];
        InviteButton button = new InviteButton("Invite a friend") {
            @Override
            void presentShare(ActionEvent evt) {
                // NOT delegated: ShareButton would defer to the next EDT cycle
                // and open a sheet. Counting here is what the press does, and
                // it is the thing the guard changes.
                presented[0]++;
            }
        };

        button.actionPerformed(new ActionEvent(button));
        button.actionPerformed(new ActionEvent(button));

        assertNotNull(button.getInvite(), "the first press minted nothing");
        assertEquals(1, presented[0],
                "a second press while the sheet was still outstanding presented "
                        + "another share, so two sheets are attempted, the app's "
                        + "listener is called twice, and the second share -- whose "
                        + "outstanding mark the first result already took -- is "
                        + "reported to nobody");
    }

    @FormTest
    void aPressAfterTheSheetAnsweredWorksAgain() {
        // The guard must not be a latch: swallowing every later press would
        // make the button dead after one share. Safe to swallow at all only
        // because Display.share() always reports an outcome -- with a null
        // package name where the platform cannot say -- so the outstanding
        // mark is always cleared.
        Invites.reset();
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(AnalyticsConsent.builder().analytics(true).build());
        final int[] presented = new int[1];
        InviteButton button = new InviteButton("Invite a friend") {
            @Override
            void presentShare(ActionEvent evt) {
                presented[0]++;
            }
        };

        button.actionPerformed(new ActionEvent(button));
        Invite first = button.getInvite();
        button.chain.onResult(ShareResult.sharedTo("com.example.chat"));
        button.actionPerformed(new ActionEvent(button));
        Invite second = button.getInvite();

        assertEquals(2, presented[0],
                "the guard is a latch: the press after a completed share never "
                        + "reached the share sheet");

        assertNotNull(second, "the button was dead after one completed share");
        assertNotSame(first, second,
                "the press after a completed share did not mint a fresh invite");
    }
}
