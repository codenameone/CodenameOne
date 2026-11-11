/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.AfterEach;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// The framework's attribution observer beside the application's listener:
/// both hear a resolved attribution, and neither takes the other's place.
class InviteObserverTest extends UITestBase {
    private static final String CODE = "ABC123xxxxxxxxxxxxxxxx";

    @AfterEach
    void cleanUp() {
        Analytics.setAttributionObserver(null);
        InviteTestSupport.tearDown();
    }

    private static final class Capture implements InviteListener {
        final List<InviteAttribution> received = new ArrayList<InviteAttribution>();

        public void inviteReceived(InviteAttribution attribution) {
            received.add(attribution);
        }

        public void attributionUnavailable(String reason) {
            // not under test
        }
    }

    @FormTest
    void theObserverHearsAnAttributionOnceAndTheListenerStillHearsIt() {
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        final int[] told = new int[1];
        Analytics.setAttributionObserver(new Runnable() {
            @Override
            public void run() {
                told[0]++;
            }
        });
        Capture capture = new Capture();
        Invites.setInviteListener(capture);
        assertEquals(0, told[0]);

        Invites.handleResolution(InviteTestSupport.resolvedJson(CODE, "spring", "sms"),
                Invites.MATCH_REFERRER, true);
        assertEquals(1, told[0]);
        assertEquals(1, capture.received.size());
        assertSame(capture, Invites.getInviteListener());

        Invites.checkForInvite();
        Invites.setInviteListener(capture);
        assertEquals(1, told[0], "the observer was told twice");
        assertEquals(1, capture.received.size());
    }

    @FormTest
    void anObserverRegisteredLaterHearsWhatIsAlreadyKnownAndNeedsNoListener() {
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        // No listener at all: the observer does not depend on one.
        Invites.handleResolution(InviteTestSupport.resolvedJson(CODE, "spring", "sms"),
                Invites.MATCH_REFERRER, true);
        final int[] told = new int[1];
        Analytics.setAttributionObserver(new Runnable() {
            @Override
            public void run() {
                told[0]++;
            }
        });
        assertEquals(1, told[0]);

        // The application's listener is still owed its one delivery.
        Capture capture = new Capture();
        Invites.setInviteListener(capture);
        assertEquals(1, capture.received.size());

        // Forgetting the attribution is forgetting it here too.
        Invites.reset();
        told[0] = 0;
        Analytics.setAttributionObserver(new Runnable() {
            @Override
            public void run() {
                told[0]++;
            }
        });
        assertEquals(0, told[0]);
    }
}
