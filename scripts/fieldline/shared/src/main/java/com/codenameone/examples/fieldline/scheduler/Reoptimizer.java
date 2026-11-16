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

/// Re-plans after something changed -- a sick call, approved time off, an
/// emergency job, a visit that overran -- while keeping as much of the
/// published plan as it can.
///
/// Three things keep a re-plan calm:
///
/// - Work that has started (or finished) by `now` is **pinned**: same
///   technician, same start. The search cannot move it.
/// - Every other job remembers its previous technician, and moving it costs the
///   disruption weight, so a job changes hands only when that buys more than a
///   phone call to the customer costs.
/// - The search starts *from the old plan*, replayed onto the new availability,
///   rather than from scratch. Only what no longer fits is repaired.
public final class Reoptimizer {
    /// One difference between the published plan and the new one.
    public static final class Change {
        public final String jobId;
        public final String fromTech;
        public final String toTech;
        public final int fromStart;
        public final int toStart;
        public final String reason;

        Change(String jobId, String fromTech, String toTech, int fromStart, int toStart, String reason) {
            this.jobId = jobId;
            this.fromTech = fromTech;
            this.toTech = toTech;
            this.fromStart = fromStart;
            this.toStart = toStart;
            this.reason = reason;
        }

        @Override
        public String toString() {
            return jobId + ": " + (fromTech == null ? "-" : fromTech) + " -> "
                    + (toTech == null ? "unassigned" : toTech) + " (" + reason + ")";
        }
    }

    /// The new plan and how it differs from the old one.
    public static final class Result {
        public final Plan plan;
        public final List<Change> changes;

        Result(Plan plan, List<Change> changes) {
            this.plan = plan;
            this.changes = changes;
        }
    }

    /// Movement smaller than this is not reported as a change.
    static final int RESCHEDULE_TOLERANCE = 15;

    private Reoptimizer() {
    }

    /// Marks each job with the technician that held it, and pins anything that
    /// started before `now`. Jobs not in the previous plan pass through.
    public static List<Job> carryOver(List<Job> jobs, Plan previous, int now) {
        List<Job> out = new ArrayList<Job>();
        for (Job j : jobs) {
            Plan.Visit v = previous.visitOf(j.id);
            if (v == null || j.pinnedTech != null) {
                out.add(j);
                continue;
            }
            Job.Builder b = j.toBuilder().previousTech(v.techId);
            if (v.start < now) {
                b.pinned(v.techId, v.start);
            }
            out.add(b.build());
        }
        return out;
    }

    /// Re-plans `problem`, whose jobs should come from `carryOver`.
    public static Result replan(Problem problem, Plan previous, long seed, int iterations) {
        Schedule s = new Schedule(problem);
        Solver solver = new Solver(problem, seed).iterations(iterations);
        // Pinned work first: it is fixed in time, and a dispatcher's drag is
        // pinned too. Replayed first, the old plan could hold the very slot the
        // drag asked for, and the pin would then fail where it was put.
        solver.placePinned(s);
        replay(s, previous);
        solver.construct(s);
        Schedule best = solver.improve(s);
        Plan plan = best.toPlan();
        return new Result(plan, diff(problem, previous, plan));
    }

    /// The published plan as it stands against `problem`, which may have
    /// changed under it: the visits that still fit are kept, in their order,
    /// and the rest are reported unassigned. No search runs. This is what a
    /// board shows between runs, and the "before" of the next one.
    public static Plan evaluate(Problem problem, Plan previous) {
        Schedule s = new Schedule(problem);
        replay(s, previous);
        return s.toPlan();
    }

    /// Puts each previous visit back on its technician's matching shift, in the
    /// old order, wherever it is still feasible.
    static void replay(Schedule s, Plan previous) {
        Problem p = s.p;
        for (Plan.Visit v : previous.getVisits()) {
            int j = p.jobIndex(v.jobId);
            int t = p.techIndex(v.techId);
            if (j < 0 || t < 0 || s.jobShift[j] >= 0) {
                continue;
            }
            int sh = shiftAt(p, t, v.start);
            if (sh < 0 || !p.canServe[j][sh]) {
                continue;
            }
            if (s.insertionDelta(j, sh, s.size[sh]) != Schedule.INFEASIBLE) {
                s.insert(j, sh, s.size[sh]);
            }
        }
    }

    static int shiftAt(Problem p, int tech, int minute) {
        for (int sh = 0; sh < p.shifts.length; sh++) {
            Shift x = p.shifts[sh];
            if (x.tech == tech && minute >= x.start && minute < x.end) {
                return sh;
            }
        }
        return -1;
    }

    /// How `after` differs from `before`, job by job, with the reason for each
    /// change: what a dispatcher reads to approve a plan made from scratch.
    public static List<Change> compare(Problem p, Plan before, Plan after) {
        return diff(p, before, after);
    }

    static List<Change> diff(Problem p, Plan before, Plan after) {
        List<Change> out = new ArrayList<Change>();
        for (Job job : p.jobs) {
            Plan.Visit a = before.visitOf(job.id);
            Plan.Visit b = after.visitOf(job.id);
            if (a == null && b == null) {
                continue;
            }
            if (a == null) {
                out.add(new Change(job.id, null, b.techId, -1, b.start, "newly scheduled"));
                continue;
            }
            if (b == null) {
                out.add(new Change(job.id, a.techId, null, a.start, -1,
                        "no qualified technician is free inside its window"));
                continue;
            }
            if (!a.techId.equals(b.techId)) {
                int t = p.techIndex(a.techId);
                String why = t >= 0 && shiftAt(p, t, a.start) < 0
                        ? p.techs[t].name + " is no longer available then"
                        : "reassigned to make room or cut driving";
                out.add(new Change(job.id, a.techId, b.techId, a.start, b.start, why));
            } else if (Math.abs(a.start - b.start) > RESCHEDULE_TOLERANCE) {
                out.add(new Change(job.id, a.techId, b.techId, a.start, b.start, "re-sequenced in the same route"));
            }
        }
        return out;
    }
}
