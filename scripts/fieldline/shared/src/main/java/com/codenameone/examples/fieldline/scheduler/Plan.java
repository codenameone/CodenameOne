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
import java.util.Collections;
import java.util.List;

/// The published form of a schedule: plain values a server can store and send
/// and a client can draw on a Gantt board or a map, with the totals a
/// dispatcher compares between two plans.
public final class Plan {
    /// One job on one technician's day.
    public static final class Visit {
        public final String jobId;
        public final String techId;
        public final int start;
        public final int end;
        /// Driving minutes from the previous stop, or from home.
        public final int travelBefore;
        /// Position in the technician's route for that shift, from 0.
        public final int sequence;

        public Visit(String jobId, String techId, int start, int end, int travelBefore, int sequence) {
            this.jobId = jobId;
            this.techId = techId;
            this.start = start;
            this.end = end;
            this.travelBefore = travelBefore;
            this.sequence = sequence;
        }
    }

    /// A technician's meal break, placed by the solver inside the route.
    public static final class Break {
        public final String techId;
        public final int start;
        public final int end;

        public Break(String techId, int start, int end) {
            this.techId = techId;
            this.start = start;
            this.end = end;
        }
    }

    /// Why a job was left unplaced, in words a dispatcher can act on.
    public static final class Reason {
        public final String jobId;
        public final String code;
        public final String text;

        Reason(String jobId, String code, String text) {
            this.jobId = jobId;
            this.code = code;
            this.text = text;
        }
    }

    private List<Break> breaks = new ArrayList<Break>();
    private List<Reason> reasons = new ArrayList<Reason>();
    private int lateJobs;
    private int lateMinutes;
    private final List<Visit> visits;
    private final List<String> unassigned;
    private final long cost;
    private final int driveMinutes;
    private final int overtimeMinutes;

    private Plan(List<Visit> visits, List<String> unassigned, long cost, int drive, int overtime) {
        this.visits = Collections.unmodifiableList(visits);
        this.unassigned = Collections.unmodifiableList(unassigned);
        this.cost = cost;
        this.driveMinutes = drive;
        this.overtimeMinutes = overtime;
    }

    /// A plan as it was published and stored: the visits alone. Its cost and
    /// totals are not known until it is evaluated against a problem, which
    /// [Reoptimizer#evaluate] does.
    public static Plan published(List<Visit> visits) {
        return new Plan(new ArrayList<Visit>(visits), new ArrayList<String>(), 0L, 0, 0);
    }

    static Plan of(Schedule sc) {
        Problem p = sc.p;
        List<Visit> visits = new ArrayList<Visit>();
        int[] starts = new int[p.jobs.length + 1];
        for (int s = 0; s < sc.route.length; s++) {
            int n = sc.size[s];
            if (n == 0) {
                continue;
            }
            sc.evaluate(s, sc.route[s], n, starts);
            int tech = p.shifts[s].tech;
            for (int i = 0; i < n; i++) {
                int j = sc.route[s][i];
                visits.add(new Visit(p.jobs[j].id, p.techs[tech].id, starts[i],
                        starts[i] + p.jobs[j].duration, sc.lastLegs[i], i));
            }
        }
        List<Break> breaks = new ArrayList<Break>();
        int lateJobs = 0;
        int lateMinutes = 0;
        for (int s = 0; s < sc.route.length; s++) {
            if (sc.size[s] == 0) {
                continue;
            }
            sc.evaluate(s, sc.route[s], sc.size[s], null);
            int at = sc.lastBreakStart();
            Technician t = p.techs[p.shifts[s].tech];
            if (at >= 0) {
                breaks.add(new Break(t.id, at, at + t.breakMinutes));
            }
            lateMinutes += sc.lastLateMinutes();
        }
        for (Visit v : visits) {
            Job job = p.jobs[p.jobIndex(v.jobId)];
            if (v.end > job.due && job.pinnedTech == null) {
                lateJobs++;
            }
        }
        List<String> un = new ArrayList<String>();
        List<Reason> reasons = new ArrayList<Reason>();
        for (int j = 0; j < p.jobs.length; j++) {
            if (sc.jobShift[j] < 0) {
                un.add(p.jobs[j].id);
                reasons.add(reason(sc, j));
            }
        }
        Plan plan = new Plan(visits, un, sc.cost(), sc.totalDriveMinutes(), sc.totalOvertimeMinutes());
        plan.breaks = Collections.unmodifiableList(breaks);
        plan.reasons = Collections.unmodifiableList(reasons);
        plan.lateJobs = lateJobs;
        plan.lateMinutes = lateMinutes;
        return plan;
    }

    /// The first rule, from the most to the least fundamental, that keeps
    /// every technician off job `j`.
    static Reason reason(Schedule sc, int j) {
        Problem p = sc.p;
        Job job = p.jobs[j];
        for (int q : p.preds[j]) {
            if (sc.jobShift[q] < 0) {
                return new Reason(job.id, "WAITING", "Waits on " + p.jobs[q].id + ", which is not planned");
            }
        }
        boolean skilled = false;
        String rule = null;
        boolean serviceable = false;
        for (int s = 0; s < p.shifts.length; s++) {
            Technician t = p.techs[p.shifts[s].tech];
            if ((t.skills & job.requiredSkills) != job.requiredSkills) {
                continue;
            }
            skilled = true;
            String b = p.blocked(job, t, p.shifts[s]);
            if (b != null) {
                rule = rule == null ? b : rule;
                continue;
            }
            serviceable |= p.canServe[j][s];
        }
        if (!skilled) {
            return new Reason(job.id, "SKILL", "No technician has the skills");
        }
        if (!serviceable && rule != null) {
            if (rule.startsWith("certification")) {
                return new Reason(job.id, "CERTIFICATION", "No qualified technician holds a valid certification");
            }
            if ("territory".equals(rule)) {
                return new Reason(job.id, "TERRITORY", "No qualified technician covers this territory");
            }
            return new Reason(job.id, "EXCLUDED", "The only qualified technicians are excluded by the customer");
        }
        if (!serviceable) {
            return new Reason(job.id, "WINDOW", "No qualified technician works inside its window");
        }
        return new Reason(job.id, "CAPACITY", "Every qualified technician is fully booked");
    }

    public List<Visit> getVisits() {
        return visits;
    }

    public List<String> getUnassigned() {
        return unassigned;
    }

    public long getCost() {
        return cost;
    }

    public int getDriveMinutes() {
        return driveMinutes;
    }

    public int getOvertimeMinutes() {
        return overtimeMinutes;
    }

    public List<Break> getBreaks() {
        return breaks;
    }

    public List<Reason> getReasons() {
        return reasons;
    }

    /// Jobs planned to finish after their SLA deadline.
    public int getLateJobs() {
        return lateJobs;
    }

    public int getLateMinutes() {
        return lateMinutes;
    }

    public Visit visitOf(String jobId) {
        for (Visit v : visits) {
            if (v.jobId.equals(jobId)) {
                return v;
            }
        }
        return null;
    }
}
