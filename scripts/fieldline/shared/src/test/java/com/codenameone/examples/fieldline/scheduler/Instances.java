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
package com.codenameone.examples.fieldline.scheduler;

import java.util.ArrayList;
import java.util.List;

/// Synthetic week-long instances around one city, for the tests.
final class Instances {
    static final int DAY = 24 * 60;
    static final long ELECTRICAL = 1;
    static final long HVAC = 2;
    static final long PLUMBING = 4;
    static final long INSPECTOR = 8;
    static final long[] SKILLS = {ELECTRICAL, HVAC, PLUMBING, INSPECTOR};
    static final int CENTER_LAT = 38720000;
    static final int CENTER_LON = -9140000;

    final List<Technician> techs = new ArrayList<Technician>();
    final List<Job> jobs = new ArrayList<Job>();

    static GeoTravel travel() {
        return new GeoTravel(780, 1350, 40, 5);
    }

    /// `days` working days of 08:00-17:00, 8h regular, 10h ceiling.
    static Instances city(long seed, int techCount, int jobCount, int days) {
        Rng r = new Rng(seed);
        Instances in = new Instances();
        for (int t = 0; t < techCount; t++) {
            long skills = SKILLS[t % SKILLS.length] | SKILLS[r.nextInt(SKILLS.length)];
            Technician.Builder b = Technician.builder("T" + t).name("Tech " + t).skills(skills)
                    .home(CENTER_LAT + r.nextInt(120000) - 60000, CENTER_LON + r.nextInt(160000) - 80000)
                    .hours(days * 8 * 60, days * 10 * 60).maxDrivePerShift(240);
            for (int d = 0; d < days; d++) {
                b.available(d * DAY + 8 * 60, d * DAY + 17 * 60);
            }
            in.techs.add(b.build());
        }
        for (int j = 0; j < jobCount; j++) {
            int day = r.nextInt(days);
            int open = day * DAY + 8 * 60 + r.nextInt(5) * 60;
            int width = 60 + r.nextInt(4) * 60;
            int pr = 1 + r.nextInt(3);
            if (r.nextInt(10) == 0) {
                pr = 5;
            }
            in.jobs.add(Job.builder("J" + j).at(CENTER_LAT + r.nextInt(120000) - 60000,
                    CENTER_LON + r.nextInt(160000) - 80000)
                    .duration(30 + r.nextInt(5) * 15).skills(SKILLS[r.nextInt(SKILLS.length)])
                    .window(open, open + width).priority(pr).build());
        }
        return in;
    }

    Problem problem() {
        return new Problem(techs, jobs, travel(), new Weights());
    }
}
