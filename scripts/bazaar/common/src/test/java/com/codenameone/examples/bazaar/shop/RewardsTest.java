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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codenameone.examples.bazaar.api.RewardsDto;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/// The rewards rules (badges, tier ring, streak, daily drop, points) and the
/// six tracking stages, without the UI.
class RewardsTest {
    @Test
    void badgesFollowTheCounts() {
        boolean[] none = Rewards.badges(0, 0, 0);
        for (boolean b : none) {
            assertFalse(b);
        }
        boolean[] first = Rewards.badges(1, 0, 1);
        assertTrue(first[Rewards.FIRST_ORDER]);
        assertFalse(first[Rewards.REGULAR]);
        boolean[] later = Rewards.badges(5, 1, 3);
        assertTrue(later[Rewards.REGULAR]);
        assertTrue(later[Rewards.REVIEWER]);
        assertTrue(later[Rewards.EXPLORER]);
        assertEquals(Rewards.REGULAR, Rewards.unlocked(Rewards.badges(4, 1, 3), later));
        assertEquals(-1, Rewards.unlocked(later, later));
        assertEquals(Rewards.FIRST_ORDER, Rewards.unlocked(null, first));
        RewardsDto dto = new RewardsDto(120, 300, "Silver", "Gold", 50, 50, 5, 3, 0, 2, 4, 0,
                "Northwind", java.util.Arrays.asList(
                    new com.codenameone.examples.bazaar.api.StoreBalanceDto("a", "Northwind", 120,
                        "Silver"),
                    new com.codenameone.examples.bazaar.api.StoreBalanceDto("b", "Fennel", 40, ""),
                    new com.codenameone.examples.bazaar.api.StoreBalanceDto("c", "Kiln", 2, "")), 120,
                "EUR");
        assertFalse(Rewards.badges(dto)[Rewards.REVIEWER]);
        assertEquals(42, Rewards.otherPoints(dto), "the best store is shown apart");
        assertEquals(0, Rewards.otherPoints(null));
        assertEquals(4, Rewards.badges(null).length);
        assertEquals(800, Rewards.badgeProgress(Rewards.REGULAR, 4, 0, 1));
        assertEquals(1000, Rewards.badgeProgress(Rewards.REGULAR, 9, 0, 1));
        assertEquals(333, Rewards.badgeProgress(Rewards.EXPLORER, 0, 0, 1));
    }

    @Test
    void tierRingFillsTowardTheNextTier() {
        assertEquals(0, Rewards.tierProgress(0, 500));
        assertEquals(500, Rewards.tierProgress(250, 250));
        assertEquals(1000, Rewards.tierProgress(900, 0), "the top tier is full");
        assertEquals(0, Rewards.tierProgress(0, 0));
    }

    @Test
    void streakCountsConsecutiveDays() {
        assertEquals(1, Rewards.streak(0, 100, 0));
        assertEquals(4, Rewards.streak(99, 100, 3));
        assertEquals(3, Rewards.streak(100, 100, 3), "twice in a day is one day");
        assertEquals(1, Rewards.streak(97, 100, 3), "a missed day starts over");
    }

    @Test
    void dailyDropVisitsEveryProduct() {
        for (int count : new int[] {1, 7, 12, 14, 21, 30}) {
            Set<Integer> seen = new HashSet<Integer>();
            for (long day = 0; day < count; day++) {
                int at = Rewards.dropIndex(19000 + day, count);
                assertTrue(at >= 0 && at < count);
                seen.add(at);
            }
            assertEquals(count, seen.size(), "count " + count);
        }
        assertEquals(-1, Rewards.dropIndex(5, 0));
    }

    @Test
    void pointsArePerWholeUnit() {
        assertEquals(42, Rewards.points(4299, 1, 100));
        assertEquals(0, Rewards.points(-5, 1, 100));
        assertEquals(0, Rewards.points(5000, 0, 100));
    }

    @Test
    void trackingStages() {
        assertEquals(TrackStages.PLACED, TrackStages.reached("OPEN", "PENDING", null));
        assertEquals(TrackStages.PAID, TrackStages.reached("OPEN", "AUTHORIZED", null));
        assertEquals(TrackStages.PACKED, TrackStages.reached("OPEN", "PAID",
                Arrays.asList("PACKED")));
        assertEquals(TrackStages.PACKED, TrackStages.reached("OPEN", "PENDING",
                Arrays.asList("LABEL_CREATED")));
        assertEquals(TrackStages.SHIPPED, TrackStages.reached("OPEN", "PAID",
                Arrays.asList("IN_TRANSIT")));
        assertEquals(TrackStages.OUT_FOR_DELIVERY, TrackStages.reached("OPEN", "PAID",
                Arrays.asList("OUT_FOR_DELIVERY", "DELIVERED")));
        assertEquals(TrackStages.DELIVERED, TrackStages.reached("OPEN", "PAID",
                Arrays.asList("DELIVERED", "CANCELLED")));
        assertEquals(TrackStages.CANCELLED, TrackStages.reached("CANCELLED", "VOIDED", null));
        assertEquals(TrackStages.PAID, TrackStages.reached("OPEN", "PAID",
                Arrays.asList("CANCELLED")));
        assertEquals(0, TrackStages.progress(TrackStages.PLACED));
        assertEquals(1000, TrackStages.progress(TrackStages.DELIVERED));
        assertEquals(600, TrackStages.progress(TrackStages.SHIPPED));
        assertEquals("Delivered!", TrackStages.banner(TrackStages.OUT_FOR_DELIVERY,
                TrackStages.DELIVERED));
        assertNull(TrackStages.banner(TrackStages.SHIPPED, TrackStages.SHIPPED));
        assertNull(TrackStages.banner(TrackStages.SHIPPED, TrackStages.CANCELLED));
    }

    @Test
    void rewardsAmountsUseTheStoreCurrency() {
        assertEquals(com.codenameone.examples.bazaar.ops.Amounts.format(1250, "EUR"),
                RewardsView.amount(1250, "EUR"));
        assertEquals(com.codenameone.examples.bazaar.ops.Amounts.format(1250, "JPY"),
                RewardsView.amount(1250, "JPY"));
        assertEquals(com.codenameone.examples.bazaar.ops.Amounts.format(1250, "USD"),
                RewardsView.amount(1250, ""));
    }
}
