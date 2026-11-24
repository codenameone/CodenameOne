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
package com.codenameone.examples.bazaar.pos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ShiftGoalTest {
    @Test
    void crossesEachMilestoneOnceAndTheHighestOfAJump() {
        ShiftGoal g = new ShiftGoal();
        g.target(10000);
        g.begin("s1", 0);
        assertEquals(0, g.record(2000));
        assertEquals(25, g.record(500));
        assertEquals(0, g.record(100));
        assertEquals(75, g.record(5000));
        assertEquals(100, g.record(9000));
        assertEquals(100, g.percent());
        assertEquals(0, g.record(9000));
    }

    @Test
    void aNewShiftStartsOverAndTheSameOneNeverGoesBack() {
        ShiftGoal g = new ShiftGoal();
        g.begin("s1", 4000);
        g.record(1000);
        g.begin("s1", 3000);
        assertEquals(5000, g.sold());
        g.begin("s2", 700);
        assertEquals(700, g.sold());
    }

    @Test
    void survivesStorage() {
        ShiftGoal g = new ShiftGoal();
        g.target(20000);
        g.begin("abc", 5000);
        ShiftGoal back = ShiftGoal.load(g.save());
        assertEquals("abc", back.shift());
        assertEquals(5000, back.sold());
        assertEquals(250, back.permille());
        assertEquals(ShiftGoal.DEFAULT_TARGET, ShiftGoal.load("junk").target());
    }
}
