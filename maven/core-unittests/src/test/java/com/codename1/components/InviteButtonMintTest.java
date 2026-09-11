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
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
