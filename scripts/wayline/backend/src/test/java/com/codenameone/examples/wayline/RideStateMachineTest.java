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

import com.codenameone.examples.wayline.api.RideState;
import com.codenameone.examples.wayline.ride.RideStateMachine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RideStateMachineTest {
    private static final RideState[] ALL = {RideState.REQUESTED, RideState.OFFERED,
        RideState.ACCEPTED, RideState.ARRIVED, RideState.IN_PROGRESS, RideState.COMPLETED,
        RideState.CANCELLED_BY_RIDER, RideState.CANCELLED_BY_DRIVER, RideState.NO_DRIVERS};

    @Test
    void aRideGoesForwardOneStepAtATime() {
        assertTrue(RideStateMachine.allowed(RideState.REQUESTED, RideState.OFFERED));
        assertTrue(RideStateMachine.allowed(RideState.OFFERED, RideState.ACCEPTED));
        assertTrue(RideStateMachine.allowed(RideState.ACCEPTED, RideState.ARRIVED));
        assertTrue(RideStateMachine.allowed(RideState.ARRIVED, RideState.IN_PROGRESS));
        assertTrue(RideStateMachine.allowed(RideState.IN_PROGRESS, RideState.COMPLETED));
        // No skipping: a ride nobody accepted cannot start, and one that has
        // not started cannot be completed.
        assertFalse(RideStateMachine.allowed(RideState.REQUESTED, RideState.ACCEPTED));
        assertFalse(RideStateMachine.allowed(RideState.OFFERED, RideState.IN_PROGRESS));
        assertFalse(RideStateMachine.allowed(RideState.ACCEPTED, RideState.COMPLETED));
    }

    @Test
    void anOfferNobodyTookGoesBackToWaiting() {
        assertTrue(RideStateMachine.allowed(RideState.OFFERED, RideState.REQUESTED));
        assertFalse(RideStateMachine.allowed(RideState.ACCEPTED, RideState.REQUESTED));
    }

    @Test
    void cancellingStopsWhenTheTripStarts() {
        assertTrue(RideStateMachine.allowed(RideState.REQUESTED, RideState.CANCELLED_BY_RIDER));
        assertTrue(RideStateMachine.allowed(RideState.ARRIVED, RideState.CANCELLED_BY_RIDER));
        assertFalse(RideStateMachine.allowed(RideState.IN_PROGRESS, RideState.CANCELLED_BY_RIDER));
        // A driver can only cancel a ride they took.
        assertFalse(RideStateMachine.allowed(RideState.OFFERED, RideState.CANCELLED_BY_DRIVER));
        assertTrue(RideStateMachine.allowed(RideState.ACCEPTED, RideState.CANCELLED_BY_DRIVER));
        assertFalse(RideStateMachine.allowed(RideState.IN_PROGRESS, RideState.CANCELLED_BY_DRIVER));
    }

    @Test
    void nothingLeavesAFinishedState() {
        RideState[] finished = {RideState.COMPLETED, RideState.CANCELLED_BY_RIDER,
            RideState.CANCELLED_BY_DRIVER, RideState.NO_DRIVERS};
        for (int from = 0; from < finished.length; from++) {
            for (int to = 0; to < ALL.length; to++) {
                assertFalse(RideStateMachine.allowed(finished[from], ALL[to]),
                        finished[from] + " -> " + ALL[to]);
            }
        }
        assertEquals(0, RideStateMachine.sources(RideState.NONE).length);
        assertTrue(RideState.ARRIVED.isActive());
        assertFalse(RideState.COMPLETED.isActive());
    }
}
