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
package com.codenameone.examples.wayline;

import com.codenameone.examples.wayline.api.RideStates;
import com.codenameone.examples.wayline.ride.RideStateMachine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RideStateMachineTest {
    private static final String[] ALL = {RideStates.REQUESTED, RideStates.OFFERED,
        RideStates.ACCEPTED, RideStates.ARRIVED, RideStates.IN_PROGRESS, RideStates.COMPLETED,
        RideStates.CANCELLED_BY_RIDER, RideStates.CANCELLED_BY_DRIVER, RideStates.NO_DRIVERS};

    @Test
    void aRideGoesForwardOneStepAtATime() {
        assertTrue(RideStateMachine.allowed(RideStates.REQUESTED, RideStates.OFFERED));
        assertTrue(RideStateMachine.allowed(RideStates.OFFERED, RideStates.ACCEPTED));
        assertTrue(RideStateMachine.allowed(RideStates.ACCEPTED, RideStates.ARRIVED));
        assertTrue(RideStateMachine.allowed(RideStates.ARRIVED, RideStates.IN_PROGRESS));
        assertTrue(RideStateMachine.allowed(RideStates.IN_PROGRESS, RideStates.COMPLETED));
        // No skipping: a ride nobody accepted cannot start, and one that has
        // not started cannot be completed.
        assertFalse(RideStateMachine.allowed(RideStates.REQUESTED, RideStates.ACCEPTED));
        assertFalse(RideStateMachine.allowed(RideStates.OFFERED, RideStates.IN_PROGRESS));
        assertFalse(RideStateMachine.allowed(RideStates.ACCEPTED, RideStates.COMPLETED));
    }

    @Test
    void anOfferNobodyTookGoesBackToWaiting() {
        assertTrue(RideStateMachine.allowed(RideStates.OFFERED, RideStates.REQUESTED));
        assertFalse(RideStateMachine.allowed(RideStates.ACCEPTED, RideStates.REQUESTED));
    }

    @Test
    void cancellingStopsWhenTheTripStarts() {
        assertTrue(RideStateMachine.allowed(RideStates.REQUESTED, RideStates.CANCELLED_BY_RIDER));
        assertTrue(RideStateMachine.allowed(RideStates.ARRIVED, RideStates.CANCELLED_BY_RIDER));
        assertFalse(RideStateMachine.allowed(RideStates.IN_PROGRESS, RideStates.CANCELLED_BY_RIDER));
        // A driver can only cancel a ride they took.
        assertFalse(RideStateMachine.allowed(RideStates.OFFERED, RideStates.CANCELLED_BY_DRIVER));
        assertTrue(RideStateMachine.allowed(RideStates.ACCEPTED, RideStates.CANCELLED_BY_DRIVER));
        assertFalse(RideStateMachine.allowed(RideStates.IN_PROGRESS, RideStates.CANCELLED_BY_DRIVER));
    }

    @Test
    void nothingLeavesAFinishedState() {
        String[] finished = {RideStates.COMPLETED, RideStates.CANCELLED_BY_RIDER,
            RideStates.CANCELLED_BY_DRIVER, RideStates.NO_DRIVERS};
        for (int from = 0; from < finished.length; from++) {
            for (int to = 0; to < ALL.length; to++) {
                assertFalse(RideStateMachine.allowed(finished[from], ALL[to]),
                        finished[from] + " -> " + ALL[to]);
            }
        }
        assertEquals(0, RideStateMachine.sources(RideStates.NONE).length);
        assertTrue(RideStates.isActive(RideStates.ARRIVED));
        assertFalse(RideStates.isActive(RideStates.COMPLETED));
    }
}
