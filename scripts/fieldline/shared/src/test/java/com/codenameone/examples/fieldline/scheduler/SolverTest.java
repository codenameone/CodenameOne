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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SolverTest {
    @Test
    void placesEverythingOnAnEasyDay() {
        Instances in = Instances.city(1, 4, 12, 1);
        Problem p = in.problem();
        Plan plan = new Solver(p, 7).iterations(300).solve().toPlan();
        assertEquals(new ArrayList<String>(), PlanChecker.violations(p, plan));
        assertTrue(plan.getUnassigned().size() <= 1, "unassigned: " + plan.getUnassigned());
    }

    @Test
    void neverViolatesAHardConstraint() {
        for (long seed = 1; seed <= 25; seed++) {
            Instances in = Instances.city(seed, 3 + (int) (seed % 6), 20 + (int) (seed * 3 % 40), 1 + (int) (seed % 3));
            if (seed % 4 == 0) {
                // Someone off for an afternoon.
                Technician t = in.techs.remove(0);
                Technician.Builder b = Technician.builder(t.id).skills(t.skills).home(t.homeLat, t.homeLon)
                        .hours(t.regularMinutes, t.maxMinutes).maxDrivePerShift(t.maxDriveMinutesPerShift)
                        .timeOff(12 * 60, 17 * 60);
                for (int i = 0; i < t.availability.length; i += 2) {
                    b.available(t.availability[i], t.availability[i + 1]);
                }
                in.techs.add(0, b.build());
            }
            Problem p = in.problem();
            Plan plan = new Solver(p, seed).iterations(200).solve().toPlan();
            assertEquals(new ArrayList<String>(), PlanChecker.violations(p, plan), "seed " + seed);
        }
    }

    @Test
    void sameSeedSamePlan() {
        Problem p = Instances.city(42, 8, 60, 3).problem();
        Plan a = new Solver(p, 99).iterations(400).solve().toPlan();
        Plan b = new Solver(p, 99).iterations(400).solve().toPlan();
        assertEquals(a.getCost(), b.getCost());
        assertEquals(a.getVisits().size(), b.getVisits().size());
        for (int i = 0; i < a.getVisits().size(); i++) {
            assertEquals(a.getVisits().get(i).jobId, b.getVisits().get(i).jobId);
            assertEquals(a.getVisits().get(i).start, b.getVisits().get(i).start);
        }
    }

    @Test
    void searchImprovesOnConstruction() {
        long totalConstruct = 0;
        long totalFinal = 0;
        for (long seed = 1; seed <= 5; seed++) {
            Problem p = Instances.city(seed * 31, 10, 80, 5).problem();
            Solver s = new Solver(p, seed).iterations(1500);
            Schedule best = s.solve();
            assertTrue(best.cost() <= s.getConstructionCost());
            totalConstruct += s.getConstructionCost();
            totalFinal += best.cost();
        }
        // The benchmark set should gain at least 3% over regret-2 alone.
        assertTrue(totalFinal * 100 <= totalConstruct * 97,
                "construction " + totalConstruct + " final " + totalFinal);
    }

    @Test
    void emergencyDisplacesRoutineWork() {
        List<Technician> techs = new ArrayList<Technician>();
        techs.add(Technician.builder("A").skills(Instances.HVAC).home(Instances.CENTER_LAT, Instances.CENTER_LON)
                .available(8 * 60, 10 * 60).build());
        List<Job> jobs = new ArrayList<Job>();
        jobs.add(Job.builder("routine").at(Instances.CENTER_LAT + 1000, Instances.CENTER_LON)
                .skills(Instances.HVAC).duration(90).window(8 * 60, 9 * 60).priority(1).build());
        jobs.add(Job.builder("leak").at(Instances.CENTER_LAT - 1000, Instances.CENTER_LON)
                .skills(Instances.HVAC).duration(90).window(8 * 60, 9 * 60).priority(5).build());
        Problem p = new Problem(techs, jobs, Instances.travel(), new Weights());
        Plan plan = new Solver(p, 1).iterations(100).solve().toPlan();
        assertNotNull(plan.visitOf("leak"));
        assertNull(plan.visitOf("routine"));
    }

    @Test
    void timeOffSplitsAShift() {
        Technician t = Technician.builder("A").available(8 * 60, 17 * 60).timeOff(12 * 60, 13 * 60).build();
        Shift[] s = Problem.buildShifts(new Technician[] {t});
        assertEquals(2, s.length);
        assertEquals(12 * 60, s[0].end);
        assertEquals(13 * 60, s[1].start);
    }

    @Test
    void integerSquareRoot() {
        assertEquals(0, GeoTravel.isqrt(0));
        assertEquals(3, GeoTravel.isqrt(15));
        assertEquals(4, GeoTravel.isqrt(16));
        assertEquals(1000000, GeoTravel.isqrt(1000000000000L));
    }
}
