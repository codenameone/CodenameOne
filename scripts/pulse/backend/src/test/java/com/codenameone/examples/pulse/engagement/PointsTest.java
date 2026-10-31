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
package com.codenameone.examples.pulse.engagement;

import com.codenameone.examples.pulse.domain.Workout;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PointsTest {
    private static Workout workout(int minutes, int avgHr, String source, String zones) {
        Workout row = new Workout();
        row.setMinutes(minutes);
        row.setAvgHr(avgHr);
        row.setSource(source);
        row.setZones(zones);
        return row;
    }

    @Test
    void aStrapIsWorthMoreThanAnEstimateAndAnEstimateMoreThanNothing() {
        List<Workout> none = new ArrayList<Workout>();
        assertEquals(10 * 3 + 5 * 2, Points.score(3, 2, none));
        int bare = Points.score(0, 0, Arrays.asList(workout(45, 0, "HEALTH", "0,0,0,0,0")));
        int phone = Points.score(0, 0, Arrays.asList(workout(45, 130, "HEALTH", "5,5,5,5,0")));
        int strap = Points.score(0, 0, Arrays.asList(workout(45, 130, Points.STRAP, "5,5,5,5,0")));
        assertEquals(9, bare);
        assertEquals(bare + Points.HEART_RATE, phone);
        assertEquals(phone + Points.WORN_SENSOR, strap);
    }

    @Test
    void levelsClimbAndStopAtTheTop() {
        assertEquals(1, Points.level(0));
        assertEquals(2, Points.level(100));
        assertEquals(100, Points.floor(2));
        assertEquals(250, Points.next(2));
        int top = Points.level(1000000);
        assertEquals(Points.LEVELS.length, top);
        assertEquals(Points.floor(top), Points.next(top));
    }

    @Test
    void badgesComeFromTheRecord() {
        List<String> earned = Points.badges(12, 4,
                Arrays.asList(workout(600, 150, Points.WATCH, "1,2,3,4,6")));
        assertEquals(Arrays.asList("FIRST_CLASS", "TEN_CLASSES", "STREAK_4", "HEART_TRACKED",
                "STRAPPED_IN", "ZONE_5", "TEN_HOURS"), earned);
        assertTrue(Points.badges(0, 0, new ArrayList<Workout>()).isEmpty());
        assertEquals(0, Engagement.zoneMinutes(null, 5));
        assertEquals(6, Engagement.zoneMinutes("1,2,3,4,6", 5));
        assertEquals(0, Engagement.zoneMinutes("1,2", 5));
    }
}
