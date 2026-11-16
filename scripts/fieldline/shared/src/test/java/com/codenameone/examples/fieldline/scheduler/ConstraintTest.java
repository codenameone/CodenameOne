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

/// The rules beyond skills, windows and hours: certifications that lapse,
/// territories, customer exclusions, meal breaks, daily caps, SLA deadlines
/// and jobs that wait on other jobs.
class ConstraintTest {
    private static final int DAY = Instances.DAY;
    private static final int GAS_SAFE = 0;
    private static final long NORTH = 1;
    private static final long SOUTH = 2;

    private static Technician.Builder tech(String id) {
        return Technician.builder(id).skills(Instances.HVAC)
                .home(Instances.CENTER_LAT, Instances.CENTER_LON)
                .hours(8 * 60, 10 * 60).available(8 * 60, 17 * 60);
    }

    private static Job.Builder job(String id, int open, int close) {
        return Job.builder(id).at(Instances.CENTER_LAT + 5000, Instances.CENTER_LON + 5000)
                .duration(60).skills(Instances.HVAC).window(open, close);
    }

    private static Plan solve(List<Technician> t, List<Job> j, Weights w) {
        Problem p = new Problem(t, j, Instances.travel(), w);
        Plan plan = new Solver(p, 3).iterations(300).solve().toPlan();
        assertEquals(new ArrayList<String>(), PlanChecker.violations(p, plan));
        return plan;
    }

    private static List<Technician> list(Technician... t) {
        List<Technician> out = new ArrayList<Technician>();
        for (Technician x : t) {
            out.add(x);
        }
        return out;
    }

    private static List<Job> jobs(Job... j) {
        List<Job> out = new ArrayList<Job>();
        for (Job x : j) {
            out.add(x);
        }
        return out;
    }

    @Test
    void aLapsedCertificationIsNotEnough() {
        Technician lapsed = tech("A").certification(GAS_SAFE, 7 * 60).build();
        Plan plan = solve(list(lapsed), jobs(job("J", 9 * 60, 12 * 60).certifications(1L << GAS_SAFE).build()),
                new Weights());
        assertEquals(1, plan.getUnassigned().size());
        assertEquals("CERTIFICATION", plan.getReasons().get(0).code);

        Technician valid = tech("B").certification(GAS_SAFE, Integer.MAX_VALUE).build();
        plan = solve(list(lapsed, valid), jobs(job("J", 9 * 60, 12 * 60).certifications(1L << GAS_SAFE).build()),
                new Weights());
        assertEquals("B", plan.visitOf("J").techId);
    }

    @Test
    void aDriveInRushHourTakesLonger() {
        List<Job> one = jobs(job("J", 8 * 60, 12 * 60).build());
        Plan calm = solve(list(tech("A").build()), one, new Weights());
        int base = calm.visitOf("J").travelBefore;
        assertTrue(base > 0, "the job is a drive away");
        Problem p = new Problem(list(tech("A").build()), one, Instances.travel(), new Weights())
                .rushHours(50, 7 * 60, 9 * 60);
        Plan rush = new Solver(p, 3).iterations(300).solve().toPlan();
        assertEquals(new ArrayList<String>(), PlanChecker.violations(p, rush));
        assertEquals(base + (base * 50 + 99) / 100, rush.visitOf("J").travelBefore);
        assertEquals(8 * 60 + rush.visitOf("J").travelBefore, rush.visitOf("J").start);
        // A drive outside the window is not slowed.
        Problem late = new Problem(list(tech("A").build()), one, Instances.travel(), new Weights())
                .rushHours(50, 16 * 60, 18 * 60);
        assertEquals(base, new Solver(late, 3).iterations(300).solve().toPlan().visitOf("J").travelBefore);
    }

    @Test
    void rushHoursFollowTheLocalDayAcrossDaylightSaving() {
        List<Job> one = jobs(job("J", 0, 24 * 60).build());
        Problem p = new Problem(list(tech("A").build()), one, Instances.travel(), new Weights())
                .rushHours(50, 7 * 60, 9 * 60);
        // Without day starts days are 1440 minutes, negative minutes included.
        assertEquals(8 * 60, p.minuteOfDay(24 * 60 + 8 * 60));
        assertEquals(23 * 60, p.minuteOfDay(-60));
        // Day 1 is 23 hours long (clocks went forward), so day 2 starts at
        // horizon minute 47 * 60 and 08:00 on it is 55 * 60, not 56 * 60.
        p.dayStarts(0, 24 * 60, 47 * 60);
        assertEquals(8 * 60, p.minuteOfDay(55 * 60));
        assertEquals(9 * 60, p.minuteOfDay(56 * 60));
    }

    @Test
    void theCustomerCanExcludeATechnician() {
        Plan plan = solve(list(tech("A").build(), tech("B").build()),
                jobs(job("J", 9 * 60, 12 * 60).preferredTech("A").exclude("A").build()), new Weights());
        assertEquals("B", plan.visitOf("J").techId);
    }

    @Test
    void territoriesAreARuleOrAPreference() {
        Technician north = tech("N").territories(NORTH).build();
        Job south = job("J", 9 * 60, 12 * 60).zone(SOUTH).build();
        Plan soft = solve(list(north), jobs(south), new Weights());
        assertNotNull(soft.visitOf("J"), "a preference still lets the job go");
        Weights strict = new Weights();
        strict.strictTerritories = true;
        Plan hard = solve(list(north), jobs(south), strict);
        assertEquals("TERRITORY", hard.getReasons().get(0).code);
        Plan covered = solve(list(north, tech("S").territories(SOUTH).build()), jobs(south), new Weights());
        assertEquals("S", covered.visitOf("J").techId);
    }

    @Test
    void theBreakFloatsInsideItsWindowAndNoJobOverlapsIt() {
        Technician t = tech("A").lunch(30, 3 * 60, 5 * 60).build();
        List<Job> js = new ArrayList<Job>();
        for (int i = 0; i < 6; i++) {
            js.add(job("J" + i, 8 * 60, 16 * 60).duration(50).build());
        }
        Plan plan = solve(list(t), js, new Weights());
        assertEquals(1, plan.getBreaks().size());
        Plan.Break b = plan.getBreaks().get(0);
        assertTrue(b.start >= 11 * 60 && b.start <= 13 * 60, "break at " + b.start);
        assertEquals(30, b.end - b.start);
    }

    @Test
    void dailyCapsHoldWhateverTheWeekAllows() {
        Technician t = tech("A").hours(40 * 60, 50 * 60).maxJobsPerShift(3).build();
        List<Job> js = new ArrayList<Job>();
        for (int i = 0; i < 5; i++) {
            js.add(job("J" + i, 8 * 60, 15 * 60).duration(30).build());
        }
        Plan plan = solve(list(t), js, new Weights());
        assertEquals(3, plan.getVisits().size());
        assertEquals("CAPACITY", plan.getReasons().get(0).code);

        Technician shortDay = tech("B").maxShiftMinutes(3 * 60).build();
        plan = solve(list(shortDay), js, new Weights());
        int busy = 0;
        for (Plan.Visit v : plan.getVisits()) {
            busy += v.end - v.start + v.travelBefore;
        }
        assertTrue(busy <= 3 * 60, "busy " + busy);
    }

    @Test
    void anSlaDeadlineDecidesTheOrder() {
        Job relaxed = job("LATER", 8 * 60, 15 * 60).duration(120).build();
        Job urgent = job("SLA", 8 * 60, 15 * 60).duration(120).due(10 * 60 + 30).build();
        Plan plan = solve(list(tech("A").build()), jobs(relaxed, urgent), new Weights());
        assertTrue(plan.visitOf("SLA").start < plan.visitOf("LATER").start);
        assertEquals(0, plan.getLateJobs());
    }

    @Test
    void lateIsChargedNotRefused() {
        Job a = job("A", 8 * 60, 15 * 60).duration(240).due(10 * 60).build();
        Plan plan = solve(list(tech("T").build()), jobs(a), new Weights());
        assertNotNull(plan.visitOf("A"));
        assertEquals(1, plan.getLateJobs());
        assertTrue(plan.getLateMinutes() > 0);
    }

    @Test
    void aFollowUpWaitsForItsSurveyEvenOnAnotherRoute() {
        Job survey = job("SURVEY", 8 * 60, 15 * 60).duration(120).build();
        Job install = job("INSTALL", 8 * 60, 15 * 60).duration(120).after("SURVEY").build();
        Plan plan = solve(list(tech("A").build(), tech("B").build()), jobs(install, survey), new Weights());
        assertTrue(plan.visitOf("INSTALL").start >= plan.visitOf("SURVEY").end);
    }

    @Test
    void aJobWaitingOnAnImpossibleOneSaysSo() {
        Job never = job("NEVER", 8 * 60, 15 * 60).skills(Instances.PLUMBING).build();
        Job next = job("NEXT", 8 * 60, 15 * 60).after("NEVER").build();
        Plan plan = solve(list(tech("A").build()), jobs(never, next), new Weights());
        assertEquals(2, plan.getUnassigned().size());
        String waiting = null;
        for (Plan.Reason r : plan.getReasons()) {
            if (r.jobId.equals("NEXT")) {
                waiting = r.code;
            }
        }
        assertEquals("WAITING", waiting);
    }

    @Test
    void everyRuleTogetherOnRandomWeeks() {
        for (long seed = 1; seed <= 20; seed++) {
            Instances in = Instances.city(seed, 4 + (int) (seed % 4), 30 + (int) (seed * 7 % 30), 2);
            Rng r = new Rng(seed * 31);
            List<Technician> crew = new ArrayList<Technician>();
            for (Technician t : in.techs) {
                Technician.Builder b = Technician.builder(t.id).skills(t.skills).home(t.homeLat, t.homeLon)
                        .hours(t.regularMinutes, t.maxMinutes).maxDrivePerShift(t.maxDriveMinutesPerShift)
                        .lunch(30, 3 * 60, 5 * 60).maxJobsPerShift(3 + r.nextInt(4))
                        .maxShiftMinutes(7 * 60 + r.nextInt(3) * 60)
                        .territories(r.nextInt(3) == 0 ? 0 : 1L << r.nextInt(2));
                if (r.nextInt(2) == 0) {
                    b.certification(GAS_SAFE, r.nextInt(2) == 0 ? Integer.MAX_VALUE : DAY + 9 * 60);
                }
                for (int i = 0; i < t.availability.length; i += 2) {
                    b.available(t.availability[i], t.availability[i + 1]);
                }
                crew.add(b.build());
            }
            List<Job> js = new ArrayList<Job>();
            for (int i = 0; i < in.jobs.size(); i++) {
                Job.Builder b = in.jobs.get(i).toBuilder().zone(1L << r.nextInt(2));
                if (r.nextInt(5) == 0) {
                    b.certifications(1L << GAS_SAFE);
                }
                if (r.nextInt(6) == 0) {
                    b.exclude(crew.get(r.nextInt(crew.size())).id);
                }
                if (r.nextInt(4) == 0) {
                    b.due(in.jobs.get(i).windowStart + 120);
                }
                if (i > 0 && r.nextInt(8) == 0) {
                    b.after(in.jobs.get(r.nextInt(i)).id);
                }
                js.add(b.build());
            }
            Weights w = new Weights();
            w.strictTerritories = seed % 2 == 0;
            Problem p = new Problem(crew, js, Instances.travel(), w);
            Plan plan = new Solver(p, seed).iterations(150).solve().toPlan();
            assertEquals(new ArrayList<String>(), PlanChecker.violations(p, plan), "seed " + seed);
            assertEquals(plan.getUnassigned().size(), plan.getReasons().size());
        }
    }
}
