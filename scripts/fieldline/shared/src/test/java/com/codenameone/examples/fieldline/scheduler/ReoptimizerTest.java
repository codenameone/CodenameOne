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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReoptimizerTest {
    /// Technician T0 calls in sick for day 2 (index 1). Their day-2 jobs must
    /// move or be released; nobody else's day should be reshuffled wholesale.
    @Test
    void sickDayMovesOnlyWhatItMustAndKeepsStartedWork() {
        Instances in = Instances.city(7, 10, 70, 3);
        Problem before = in.problem();
        Plan published = new Solver(before, 3).iterations(800).solve().toPlan();

        int now = Instances.DAY + 10 * 60; // day 2, 10:00
        List<Technician> techs = new ArrayList<Technician>();
        for (Technician t : in.techs) {
            if (t.id.equals("T0")) {
                Technician.Builder b = Technician.builder(t.id).name(t.name).skills(t.skills)
                        .home(t.homeLat, t.homeLon).hours(t.regularMinutes, t.maxMinutes)
                        .maxDrivePerShift(t.maxDriveMinutesPerShift)
                        .timeOff(now, 2 * Instances.DAY);
                for (int i = 0; i < t.availability.length; i += 2) {
                    b.available(t.availability[i], t.availability[i + 1]);
                }
                techs.add(b.build());
            } else {
                techs.add(t);
            }
        }
        List<Job> jobs = Reoptimizer.carryOver(in.jobs, published, now);
        Problem after = new Problem(techs, jobs, Instances.travel(), new Weights());
        Reoptimizer.Result r = Reoptimizer.replan(after, published, 3, 600);

        assertEquals(new ArrayList<String>(), PlanChecker.violations(after, r.plan));
        int moved = 0;
        for (Reoptimizer.Change c : r.changes) {
            if (c.fromTech != null && !c.fromTech.equals(c.toTech)) {
                moved++;
            }
        }
        int affected = 0;
        for (Plan.Visit v : published.getVisits()) {
            if (v.techId.equals("T0") && v.start >= now && v.start < 2 * Instances.DAY) {
                affected++;
                Plan.Visit nv = r.plan.visitOf(v.jobId);
                assertTrue(nv == null || !nv.techId.equals("T0"), v.jobId + " still on the sick technician");
            }
            if (v.start < now) {
                Plan.Visit nv = r.plan.visitOf(v.jobId);
                assertNotNull(nv, "started job " + v.jobId + " dropped");
                assertEquals(v.techId, nv.techId);
                assertEquals(v.start, nv.start);
            }
        }
        // Stability: reassignments stay close to what the absence forced.
        assertTrue(moved <= affected * 2 + 3, "moved " + moved + " for " + affected + " affected");
    }

    @Test
    void urgentJobIsSlottedIntoAPublishedDay() {
        Instances in = Instances.city(11, 6, 30, 1);
        Problem before = in.problem();
        Plan published = new Solver(before, 5).iterations(400).solve().toPlan();
        List<Job> jobs = new ArrayList<Job>(in.jobs);
        jobs.add(Job.builder("EMERGENCY").at(Instances.CENTER_LAT, Instances.CENTER_LON)
                .skills(Instances.ELECTRICAL).duration(60).window(12 * 60, 14 * 60).priority(5).build());
        int now = 9 * 60;
        Problem after = new Problem(in.techs, Reoptimizer.carryOver(jobs, published, now),
                Instances.travel(), new Weights());
        Reoptimizer.Result r = Reoptimizer.replan(after, published, 5, 300);
        assertNotNull(r.plan.visitOf("EMERGENCY"));
        assertEquals(new ArrayList<String>(), PlanChecker.violations(after, r.plan));
    }
}
