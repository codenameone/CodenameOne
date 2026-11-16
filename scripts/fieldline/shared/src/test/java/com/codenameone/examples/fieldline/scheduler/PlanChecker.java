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

/// An independent re-check of every hard constraint on a published plan. It
/// shares no code with `Schedule.evaluate`, so a bug there cannot hide here.
final class PlanChecker {
    private PlanChecker() {
    }

    static List<String> violations(Problem p, Plan plan) {
        List<String> out = new ArrayList<String>();
        TravelModel tm = Instances.travel();
        int[] work = new int[p.techs.length];
        for (int t = 0; t < p.techs.length; t++) {
            Technician tech = p.techs[t];
            List<Plan.Visit> mine = new ArrayList<Plan.Visit>();
            for (Plan.Visit v : plan.getVisits()) {
                if (v.techId.equals(tech.id)) {
                    mine.add(v);
                }
            }
            for (int i = 0; i < mine.size(); i++) {
                Plan.Visit v = mine.get(i);
                Job job = p.jobs[p.jobIndex(v.jobId)];
                if ((tech.skills & job.requiredSkills) != job.requiredSkills) {
                    out.add(v.jobId + " lacks skill on " + tech.id);
                }
                if (job.pinnedTech == null) {
                    for (String x : job.excluded) {
                        if (x.equals(tech.id)) {
                            out.add(v.jobId + " given to excluded " + tech.id);
                        }
                    }
                    for (int bit = 0; bit < 64; bit++) {
                        if ((job.certs & (1L << bit)) != 0 && !tech.certified(bit, v.end)) {
                            out.add(v.jobId + " needs a certification " + tech.id + " lacks");
                        }
                    }
                    if (p.weights.strictTerritories && Problem.outOfTerritory(job, tech)) {
                        out.add(v.jobId + " outside the territory of " + tech.id);
                    }
                }
                for (int q : p.preds[p.jobIndex(v.jobId)]) {
                    Plan.Visit pv = plan.visitOf(p.jobs[q].id);
                    if (pv == null || pv.end > v.start) {
                        out.add(v.jobId + " starts before " + p.jobs[q].id + " is done");
                    }
                }
                for (Plan.Break br : plan.getBreaks()) {
                    if (br.techId.equals(tech.id) && v.start < br.end && v.end > br.start) {
                        out.add(v.jobId + " overlaps the break of " + tech.id);
                    }
                }
                int lo = job.pinnedTech != null ? job.pinnedStart : job.windowStart;
                int hi = job.pinnedTech != null ? job.pinnedStart : job.windowEnd;
                if (v.start < lo || v.start > hi) {
                    out.add(v.jobId + " starts outside window");
                }
                if (job.pinnedTech != null && !job.pinnedTech.equals(tech.id)) {
                    out.add(v.jobId + " pinned job moved");
                }
                if (!inside(tech.availability, v.start, v.end) || overlaps(tech.timeOff, v.start, v.end)) {
                    out.add(v.jobId + " outside availability of " + tech.id);
                }
                if (i > 0) {
                    Plan.Visit prev = mine.get(i - 1);
                    Job pj = p.jobs[p.jobIndex(prev.jobId)];
                    if (prev.start / Instances.DAY == v.start / Instances.DAY) {
                        int leg = tm.minutes(pj.lat, pj.lon, job.lat, job.lon);
                        if (prev.end + leg > v.start) {
                            out.add(v.jobId + " cannot be reached from " + prev.jobId);
                        }
                    }
                }
                work[t] += job.duration + v.travelBefore;
            }
        }
        for (Shift sh : p.shifts) {
            Technician tech = p.techs[sh.tech];
            int jobs = 0;
            int last = -1;
            int busy = 0;
            for (Plan.Visit v : plan.getVisits()) {
                if (v.techId.equals(tech.id) && v.start >= sh.start && v.start < sh.end) {
                    jobs++;
                    last = Math.max(last, v.end);
                    busy += v.end - v.start + v.travelBefore;
                }
            }
            if (jobs > tech.maxJobsPerShift) {
                out.add(tech.id + " has " + jobs + " jobs in one shift");
            }
            if (busy > tech.maxShiftMinutes) {
                out.add(tech.id + " works past the daily limit");
            }
            if (tech.breakMinutes > 0 && jobs > 0 && last > sh.start + tech.breakAfter) {
                boolean found = false;
                for (Plan.Break br : plan.getBreaks()) {
                    if (br.techId.equals(tech.id) && br.start >= sh.start + tech.breakAfter
                            && br.start <= sh.start + tech.breakBefore) {
                        found = true;
                    }
                }
                if (!found) {
                    out.add(tech.id + " gets no break in the shift at " + sh.start);
                }
            }
        }
        for (int t = 0; t < work.length; t++) {
            if (work[t] > p.techs[t].maxMinutes) {
                out.add(p.techs[t].id + " exceeds hour ceiling");
            }
        }
        return out;
    }

    private static boolean inside(int[] pairs, int a, int b) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (a >= pairs[i] && b <= pairs[i + 1]) {
                return true;
            }
        }
        return false;
    }

    private static boolean overlaps(int[] pairs, int a, int b) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (a < pairs[i + 1] && b > pairs[i]) {
                return true;
            }
        }
        return false;
    }
}
