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
package com.codenameone.examples.savor;

import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.delivery.DeliveryStateMachine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryStateMachineTest {
    private static final DeliveryState[] ALL = {DeliveryState.REQUESTED, DeliveryState.OFFERED,
        DeliveryState.ACCEPTED, DeliveryState.ARRIVED, DeliveryState.IN_PROGRESS, DeliveryState.COMPLETED,
        DeliveryState.CANCELLED_BY_CUSTOMER, DeliveryState.CANCELLED_BY_COURIER, DeliveryState.NO_COURIERS};

    @Test
    void aDeliveryGoesForwardOneStepAtATime() {
        assertTrue(DeliveryStateMachine.allowed(DeliveryState.REQUESTED, DeliveryState.OFFERED));
        assertTrue(DeliveryStateMachine.allowed(DeliveryState.OFFERED, DeliveryState.ACCEPTED));
        assertTrue(DeliveryStateMachine.allowed(DeliveryState.ACCEPTED, DeliveryState.ARRIVED));
        assertTrue(DeliveryStateMachine.allowed(DeliveryState.ARRIVED, DeliveryState.IN_PROGRESS));
        assertTrue(DeliveryStateMachine.allowed(DeliveryState.IN_PROGRESS, DeliveryState.COMPLETED));
        // No skipping: a delivery nobody accepted cannot start, and one that has
        // not started cannot be completed.
        assertFalse(DeliveryStateMachine.allowed(DeliveryState.REQUESTED, DeliveryState.ACCEPTED));
        assertFalse(DeliveryStateMachine.allowed(DeliveryState.OFFERED, DeliveryState.IN_PROGRESS));
        assertFalse(DeliveryStateMachine.allowed(DeliveryState.ACCEPTED, DeliveryState.COMPLETED));
    }

    @Test
    void anOfferNobodyTookGoesBackToWaiting() {
        assertTrue(DeliveryStateMachine.allowed(DeliveryState.OFFERED, DeliveryState.REQUESTED));
        assertFalse(DeliveryStateMachine.allowed(DeliveryState.ACCEPTED, DeliveryState.REQUESTED));
    }

    @Test
    void cancellingStopsWhenTheTripStarts() {
        assertTrue(DeliveryStateMachine.allowed(DeliveryState.REQUESTED, DeliveryState.CANCELLED_BY_CUSTOMER));
        assertTrue(DeliveryStateMachine.allowed(DeliveryState.ARRIVED, DeliveryState.CANCELLED_BY_CUSTOMER));
        assertFalse(DeliveryStateMachine.allowed(DeliveryState.IN_PROGRESS, DeliveryState.CANCELLED_BY_CUSTOMER));
        // A courier can only cancel a delivery they took.
        assertFalse(DeliveryStateMachine.allowed(DeliveryState.OFFERED, DeliveryState.CANCELLED_BY_COURIER));
        assertTrue(DeliveryStateMachine.allowed(DeliveryState.ACCEPTED, DeliveryState.CANCELLED_BY_COURIER));
        assertFalse(DeliveryStateMachine.allowed(DeliveryState.IN_PROGRESS, DeliveryState.CANCELLED_BY_COURIER));
    }

    @Test
    void nothingLeavesAFinishedState() {
        DeliveryState[] finished = {DeliveryState.COMPLETED, DeliveryState.CANCELLED_BY_CUSTOMER,
            DeliveryState.CANCELLED_BY_COURIER, DeliveryState.NO_COURIERS};
        for (int from = 0; from < finished.length; from++) {
            for (int to = 0; to < ALL.length; to++) {
                assertFalse(DeliveryStateMachine.allowed(finished[from], ALL[to]),
                        finished[from] + " -> " + ALL[to]);
            }
        }
        assertEquals(0, DeliveryStateMachine.sources(DeliveryState.NONE).length);
        assertTrue(DeliveryState.ARRIVED.isActive());
        assertFalse(DeliveryState.COMPLETED.isActive());
    }
}
