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
package com.codenameone.examples.bazaar.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/// The seller's goals, streak, milestones and counter easing.
class SellerGoalsTest {
    @Test
    void dailyGoalIsARoundAmountAboveTheUsualDay() {
        // Average of the days with sales (before today) is $50; plus a fifth is
        // $60, already a $5 step.
        assertEquals(6000, SellerGoals.dailyGoal(new long[] {4000, 0, 6000, 1234}));
        assertEquals(10000, SellerGoals.dailyGoal(new long[] {0, 0, 0}));
        assertEquals(15000, SellerGoals.dailyGoal(new long[] {12000, 12100, 0}));
        assertEquals(SellerGoals.dailyGoal(new long[] {4000, 0}) * 7,
                SellerGoals.weeklyGoal(new long[] {4000, 0}));
    }

    @Test
    void progressIsCappedAndWeekSumsTheLastSevenDays() {
        assertEquals(5000, SellerGoals.progressBp(50, 100));
        assertEquals(10000, SellerGoals.progressBp(500, 100));
        assertEquals(0, SellerGoals.progressBp(10, 0));
        assertEquals(7, SellerGoals.week(new long[] {100, 1, 1, 1, 1, 1, 1, 1}));
    }

    @Test
    void streakForgivesAMorningWithNoSaleYet() {
        assertEquals(3, SellerGoals.streak(new long[] {0, 5, 5, 5}));
        assertEquals(2, SellerGoals.streak(new long[] {5, 0, 5, 5, 0}));
        assertEquals(0, SellerGoals.streak(new long[] {5, 0, 0}));
    }

    @Test
    void milestonesPickTheMostNotable() {
        long[] record = {100, 200, 300, 900};
        assertEquals(SellerGoals.HUNDREDS, SellerGoals.milestone(record, 3, 99, 101));
        assertEquals(SellerGoals.RECORD_DAY, SellerGoals.milestone(record, 3, 50, 51));
        assertEquals(SellerGoals.FIRST_SALE,
                SellerGoals.milestone(new long[] {100, 200, 300, 50}, 1, 50, 51));
        assertEquals(SellerGoals.NONE, SellerGoals.milestone(new long[] {100, 0}, 0, 5, 5));
    }

    @Test
    void counterEasesOutAndLandsExactly() {
        assertEquals(0, SellerGoals.counted(1000, 0, 900));
        assertEquals(1000, SellerGoals.counted(1000, 900, 900));
        long half = SellerGoals.counted(1000, 450, 900);
        assertTrue(half > 500 && half < 1000, "ease-out is past half at half time: " + half);
    }
}
