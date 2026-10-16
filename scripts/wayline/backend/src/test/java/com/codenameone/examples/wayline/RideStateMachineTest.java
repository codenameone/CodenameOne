/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
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
