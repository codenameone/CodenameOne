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

import java.util.ArrayList;
import java.util.List;

/// The game on top of training: points for turning up and for what a member
/// records about it, levels for the points, and badges for milestones.
///
/// Everything is worked out from the visits and workouts already stored, so
/// nothing here can drift from them and there is nothing to migrate when a
/// rule changes. The rules favour the habit over the effort: a visit is worth
/// more than a long workout, and a workout with a heart rate is worth more
/// than one without, most of all one measured by a chest strap or a watch,
/// which is what the class view and the trainer's screen are built on.
public final class Points {
    /// A visit to the studio.
    static final int VISIT = 10;
    /// Each week of the current streak.
    static final int STREAK_WEEK = 5;
    /// Each five minutes of a recorded workout.
    static final int FIVE_MINUTES = 1;
    /// A workout recorded with a heart rate.
    static final int HEART_RATE = 15;
    /// A workout measured by a strap or a watch rather than estimated.
    static final int WORN_SENSOR = 10;

    /// The points each level starts at.
    static final int[] LEVELS = {0, 100, 250, 500, 1000, 2000, 4000};

    /// The workout sources that were measured by something worn.
    public static final String STRAP = "STRAP";
    public static final String WATCH = "WATCH";

    /// Every badge, in the order a member earns them, more or less.
    public static final String[] BADGES = {"FIRST_CLASS", "TEN_CLASSES", "FIFTY_CLASSES",
        "STREAK_4", "STREAK_12", "HEART_TRACKED", "STRAPPED_IN", "ZONE_5", "TEN_HOURS"};

    private Points() {
    }

    /// The points for a record of visits, a streak and workouts.
    static int score(int visits, int streak, List<Workout> workouts) {
        int points = visits * VISIT + streak * STREAK_WEEK;
        for (int iter = 0; iter < workouts.size(); iter++) {
            Workout row = workouts.get(iter);
            points += row.getMinutes() / 5 * FIVE_MINUTES;
            if (row.getAvgHr() > 0) {
                points += HEART_RATE;
                if (worn(row.getSource())) {
                    points += WORN_SENSOR;
                }
            }
        }
        return points;
    }

    /// The level, from 1, for a number of points.
    static int level(int points) {
        int level = 1;
        while (level < LEVELS.length && points >= LEVELS[level]) {
            level++;
        }
        return level;
    }

    /// The points a level starts at.
    static int floor(int level) {
        return LEVELS[Math.max(0, Math.min(LEVELS.length, level) - 1)];
    }

    /// The points the level after this one starts at, or this one's at the top.
    static int next(int level) {
        return level < LEVELS.length ? LEVELS[level] : floor(level);
    }

    /// The badges earned, in [#BADGES] order.
    static List<String> badges(int visits, int streak, List<Workout> workouts) {
        boolean heart = false;
        boolean worn = false;
        boolean peak = false;
        int minutes = 0;
        for (int iter = 0; iter < workouts.size(); iter++) {
            Workout row = workouts.get(iter);
            minutes += row.getMinutes();
            heart |= row.getAvgHr() > 0;
            worn |= row.getAvgHr() > 0 && worn(row.getSource());
            peak |= Engagement.zoneMinutes(row.getZones(), 5) > 0;
        }
        boolean[] earned = {visits >= 1, visits >= 10, visits >= 50, streak >= 4, streak >= 12,
            heart, worn, peak, minutes >= 600};
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < BADGES.length; iter++) {
            if (earned[iter]) {
                out.add(BADGES[iter]);
            }
        }
        return out;
    }

    static boolean worn(String source) {
        return STRAP.equals(source) || WATCH.equals(source);
    }
}
