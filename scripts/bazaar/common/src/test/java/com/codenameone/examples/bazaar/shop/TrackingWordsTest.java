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
package com.codenameone.examples.bazaar.shop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/// What the tracking screen and the rewards card say, without the UI: step
/// times, the carrier's name, the tracking number, the delivery window, the
/// badge counts and the money in the tier ring.
class TrackingWordsTest {
    @Test
    void everyReachedStepHasATimeThatNeverRunsBackwards() {
        long[] at = {100, 0, 300, 250, 0, 0};
        assertArrayEquals(new long[] {100, 100, 300, 300, 300, 0}, TrackStages.fill(at, 4));
        assertArrayEquals(new long[] {100, 0, 0, 0, 0, 0}, TrackStages.fill(at, 0));
        assertEquals(TrackStages.PAID, TrackStages.eventStage("CAPTURED"));
        assertEquals(TrackStages.OUT_FOR_DELIVERY,
                TrackStages.eventStage("FULFILMENT_OUT_FOR_DELIVERY"));
        assertEquals(-1, TrackStages.eventStage("NOTE"));
    }

    @Test
    void theCarrierAndNumberReadLikeALabel() {
        assertEquals("Bazaar Express", TrackStages.carrierName("simulated"));
        assertEquals("USPS", TrackStages.carrierName("usps"));
        assertEquals("FedEx", TrackStages.carrierName("FEDEX"));
        assertEquals("Acme", TrackStages.carrierName("Acme"));
        assertEquals("SIM 2552 0207 4779", TrackStages.trackingNumber("SIM255202074779"));
        assertEquals("AB 123 4567", TrackStages.trackingNumber("AB1234567"));
        assertEquals("1234", TrackStages.trackingNumber("1234"));
        assertEquals("", TrackStages.trackingNumber(null));
    }

    @Test
    void theDeliveryWindowFollowsTheStageAndTheHour() {
        assertArrayEquals(new int[] {16, 18, 0}, TrackStages.window(TrackStages.OUT_FOR_DELIVERY,
                15));
        assertArrayEquals(new int[] {10, 12, 0}, TrackStages.window(TrackStages.OUT_FOR_DELIVERY,
                7));
        assertArrayEquals(new int[] {9, 12, 1}, TrackStages.window(TrackStages.OUT_FOR_DELIVERY,
                21));
        assertArrayEquals(new int[] {10, 14, 1}, TrackStages.window(TrackStages.SHIPPED, 9));
        assertNull(TrackStages.window(TrackStages.DELIVERED, 9));
        assertNull(TrackStages.window(TrackStages.CANCELLED, 9));
        assertEquals("4-6 pm", TrackStages.span(16, 18));
        assertEquals("11 am-1 pm", TrackStages.span(11, 13));
        assertEquals("12 pm", TrackStages.hour(12));
    }

    @Test
    void lockedBadgesCountWhatTheyNeed() {
        assertEquals("2/5", Rewards.badgeCount(Rewards.REGULAR, 2, 0, 1));
        assertEquals("3/3", Rewards.badgeCount(Rewards.EXPLORER, 0, 0, 7));
        assertNull(Rewards.badgeCount(Rewards.REVIEWER, 4, 0, 2));
        assertEquals(Rewards.TITLES.length, Rewards.SHORT_HINTS.length);
    }

    @Test
    void theRingIsInWholeDollars() {
        assertEquals("$877", Money.whole(87750));
        assertEquals("$1,250", Money.whole(125000));
        assertEquals("$0", Money.whole(99));
    }
}
