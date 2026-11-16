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

/// A complete assignment of jobs to shifts, in visiting order, with the cost
/// of every route kept current.
///
/// A schedule held by the solver is always *feasible*: every route meets every
/// window, its shift end and its driving limit, and no technician exceeds their
/// hour ceiling. Unplaced jobs are allowed and are charged for. Moves that would
/// break a hard constraint are refused rather than penalized, which keeps the
/// search inside the space a dispatcher could actually publish.
public final class Schedule {
    static final long INFEASIBLE = Long.MAX_VALUE;

    final Problem p;
    final int[][] route;
    final int[] size;
    final long[] routeCost;
    final int[] routeWork;
    final int[] routeDrive;
    final int[] jobShift;
    final int[] techWork;

    private int[] scratch;
    private int lastWork;
    private int lastDrive;
    private int lastBreak;
    /// The drive before each stop of the route last evaluated with starts.
    int[] lastLegs = new int[0];
    private int lastLate;
    private int[] localStarts;
    /// Start of every placed job, kept so a dependency on another route can be
    /// checked without evaluating that route again.
    final int[] startOf;

    Schedule(Problem p) {
        this.p = p;
        int s = p.shifts.length;
        route = new int[s][];
        for (int i = 0; i < s; i++) {
            route[i] = new int[4];
        }
        size = new int[s];
        routeCost = new long[s];
        routeWork = new int[s];
        routeDrive = new int[s];
        jobShift = new int[p.jobs.length];
        for (int j = 0; j < jobShift.length; j++) {
            jobShift[j] = -1;
        }
        techWork = new int[p.techs.length];
        scratch = new int[p.jobs.length + 1];
        localStarts = new int[p.jobs.length + 1];
        startOf = new int[p.jobs.length];
        for (int j = 0; j < startOf.length; j++) {
            startOf[j] = -1;
        }
    }

    Schedule copy() {
        Schedule c = new Schedule(p);
        c.copyFrom(this);
        return c;
    }

    void copyFrom(Schedule o) {
        for (int s = 0; s < route.length; s++) {
            if (route[s].length < o.size[s]) {
                route[s] = new int[o.route[s].length];
            }
            System.arraycopy(o.route[s], 0, route[s], 0, o.size[s]);
            size[s] = o.size[s];
        }
        System.arraycopy(o.routeCost, 0, routeCost, 0, routeCost.length);
        System.arraycopy(o.routeWork, 0, routeWork, 0, routeWork.length);
        System.arraycopy(o.routeDrive, 0, routeDrive, 0, routeDrive.length);
        System.arraycopy(o.jobShift, 0, jobShift, 0, jobShift.length);
        System.arraycopy(o.techWork, 0, techWork, 0, techWork.length);
        System.arraycopy(o.startOf, 0, startOf, 0, startOf.length);
    }

    /// Cost of visiting `seq[0..n)` on shift `s`, or `INFEASIBLE`. Fills
    /// `starts` with each start time when it is not null.
    ///
    /// The meal break floats: it is taken at the first stop after it may
    /// begin, or earlier if the next job would run past the latest moment it
    /// may begin. Waiting for a window absorbs it where it can.
    long evaluate(int s, int[] seq, int n, int[] starts) {
        lastWork = 0;
        lastDrive = 0;
        lastBreak = -1;
        lastLate = 0;
        if (n == 0) {
            return 0;
        }
        Shift sh = p.shifts[s];
        Technician tech = p.techs[sh.tech];
        if (n > tech.maxJobsPerShift) {
            return INFEASIBLE;
        }
        Weights w = p.weights;
        int home = sh.tech;
        int[] local = localStarts;
        boolean took = tech.breakMinutes <= 0;
        int breakFrom = sh.start + tech.breakAfter;
        int breakBy = sh.start + tech.breakBefore;
        int prev = home;
        int t = sh.start;
        int drive = 0;
        int wait = 0;
        int work = 0;
        long cost = 0;
        for (int i = 0; i < n; i++) {
            int j = seq[i];
            Job job = p.jobs[j];
            int pt = p.pointOfJob(j);
            int leg = p.leg(prev, pt, t);
            int earliest = p.earliestStart(j);
            if (p.hasDependencies) {
                int ready = ready(s, seq, i, local);
                if (ready < 0) {
                    return INFEASIBLE;
                }
                earliest = Math.max(earliest, ready);
            }
            int start = Math.max(t + leg, earliest);
            if (!took && (t >= breakFrom || start + job.duration > breakBy)) {
                int at = Math.max(t, breakFrom);
                if (at > breakBy) {
                    return INFEASIBLE;
                }
                lastBreak = at;
                t = at + tech.breakMinutes;
                took = true;
                leg = p.leg(prev, pt, t);
                start = Math.max(t + leg, earliest);
            }
            if (i > 0) {
                wait += start - (t + leg);
            }
            if (start > p.latestStart(j)) {
                return INFEASIBLE;
            }
            local[i] = start;
            if (starts != null) {
                starts[i] = start;
                if (lastLegs.length < n) {
                    lastLegs = new int[n];
                }
                lastLegs[i] = leg;
            }
            drive += leg;
            work += job.duration;
            t = start + job.duration;
            prev = pt;
            if (p.hasDependencies && !successorsClear(s, seq, n, j, t)) {
                return INFEASIBLE;
            }
            if (t > job.due && job.pinnedTech == null) {
                lastLate += t - job.due;
                cost += (long) (t - job.due) * w.lateness * job.priority;
            }
            if (!w.strictTerritories && Problem.outOfTerritory(job, tech)) {
                cost += w.outOfTerritory;
            }
            int pref = p.preferredTechIndex[j];
            if (pref >= 0 && pref != sh.tech) {
                cost += w.continuity;
            }
            int before = p.previousTechIndex[j];
            if (before >= 0 && before != sh.tech) {
                cost += w.disruption;
            }
        }
        if (!took && t > breakFrom) {
            // A day that runs past the earliest break takes it before heading home.
            lastBreak = t;
            t += tech.breakMinutes;
        }
        int back = p.leg(prev, home, t);
        if (t + back > sh.end) {
            return INFEASIBLE;
        }
        drive += back;
        if (drive > tech.maxDriveMinutesPerShift || work + drive > tech.maxShiftMinutes) {
            return INFEASIBLE;
        }
        lastWork = work + drive;
        lastDrive = drive;
        return cost + (long) drive * w.travel + (long) wait * w.waiting;
    }

    /// The earliest start the predecessors of `seq[i]` allow, or -1 when one
    /// of them is not placed before it.
    private int ready(int s, int[] seq, int i, int[] local) {
        int j = seq[i];
        int ready = 0;
        for (int q : p.preds[j]) {
            int pos = indexOf(seq, i, q);
            int done;
            if (pos >= 0) {
                done = local[pos] + p.jobs[q].duration;
            } else if (jobShift[q] >= 0 && jobShift[q] != s) {
                done = startOf[q] + p.jobs[q].duration;
            } else {
                return -1;
            }
            ready = Math.max(ready, done);
        }
        return ready;
    }

    /// Whether `j`, finishing at `end`, still lets every successor placed on
    /// another route start on time. Successors on this route are checked when
    /// they are reached.
    private boolean successorsClear(int s, int[] seq, int n, int j, int end) {
        for (int r : p.succs[j]) {
            if (indexOf(seq, n, r) < 0 && jobShift[r] >= 0 && jobShift[r] != s && startOf[r] < end) {
                return false;
            }
        }
        return true;
    }

    private static int indexOf(int[] seq, int n, int v) {
        for (int k = 0; k < n; k++) {
            if (seq[k] == v) {
                return k;
            }
        }
        return -1;
    }

    long overtimeCost(int tech, int work) {
        int over = work - p.techs[tech].regularMinutes;
        return over > 0 ? (long) over * p.weights.overtime : 0;
    }

    /// Added cost of putting job `j` at `pos` on shift `s`, or `INFEASIBLE`.
    long insertionDelta(int j, int s, int pos) {
        int n = size[s];
        int[] r = route[s];
        int[] seq = scratch;
        System.arraycopy(r, 0, seq, 0, pos);
        seq[pos] = j;
        System.arraycopy(r, pos, seq, pos + 1, n - pos);
        long c = evaluate(s, seq, n + 1, null);
        if (c == INFEASIBLE) {
            return INFEASIBLE;
        }
        int tech = p.shifts[s].tech;
        int newWork = techWork[tech] - routeWork[s] + lastWork;
        if (newWork > p.techs[tech].maxMinutes) {
            return INFEASIBLE;
        }
        return c - routeCost[s] + overtimeCost(tech, newWork) - overtimeCost(tech, techWork[tech]);
    }

    /// The cheapest feasible position for `j` on `s`, packed as
    /// `{delta, pos}`, or null.
    long[] bestPosition(int j, int s) {
        if (!p.canServe[j][s]) {
            return null;
        }
        long best = INFEASIBLE;
        int bestPos = -1;
        for (int pos = 0; pos <= size[s]; pos++) {
            long d = insertionDelta(j, s, pos);
            if (d < best) {
                best = d;
                bestPos = pos;
            }
        }
        return bestPos < 0 ? null : new long[] {best, bestPos};
    }

    void insert(int j, int s, int pos) {
        int n = size[s];
        if (route[s].length == n) {
            int[] g = new int[n * 2 + 2];
            System.arraycopy(route[s], 0, g, 0, n);
            route[s] = g;
        }
        int[] r = route[s];
        System.arraycopy(r, pos, r, pos + 1, n - pos);
        r[pos] = j;
        size[s] = n + 1;
        jobShift[j] = s;
        refresh(s);
    }

    /// Removes `j` from its route. Returns false and leaves the schedule as it
    /// was when the shortened route would be infeasible, which rounding in a
    /// travel model can cause in rare cases.
    boolean remove(int j) {
        int s = jobShift[j];
        if (s < 0) {
            return true;
        }
        // A job cannot stay planned once the job it waits on is taken out.
        for (int r : p.succs[j]) {
            if (!remove(r)) {
                return false;
            }
        }
        int n = size[s];
        int[] r = route[s];
        int pos = 0;
        while (r[pos] != j) {
            pos++;
        }
        System.arraycopy(r, pos + 1, r, pos, n - pos - 1);
        size[s] = n - 1;
        if (evaluate(s, r, n - 1, null) == INFEASIBLE) {
            System.arraycopy(r, pos, r, pos + 1, n - pos - 1);
            r[pos] = j;
            size[s] = n;
            return false;
        }
        jobShift[j] = -1;
        startOf[j] = -1;
        refresh(s);
        return true;
    }

    /// Replaces the order of route `s` if the new order is feasible and fits
    /// the technician's hours. Returns whether it was applied.
    boolean setOrder(int s, int[] seq, int n) {
        long c = evaluate(s, seq, n, null);
        if (c == INFEASIBLE) {
            return false;
        }
        int tech = p.shifts[s].tech;
        if (techWork[tech] - routeWork[s] + lastWork > p.techs[tech].maxMinutes) {
            return false;
        }
        System.arraycopy(seq, 0, route[s], 0, n);
        refresh(s);
        return true;
    }

    private void refresh(int s) {
        long c = evaluate(s, route[s], size[s], null);
        for (int i = 0; i < size[s]; i++) {
            startOf[route[s][i]] = localStarts[i];
        }
        if (c == INFEASIBLE) {
            throw new IllegalStateException("route " + s + " became infeasible");
        }
        int tech = p.shifts[s].tech;
        techWork[tech] += lastWork - routeWork[s];
        routeCost[s] = c;
        routeWork[s] = lastWork;
        routeDrive[s] = lastDrive;
    }

    /// The total objective: routes, overtime, workload balance and the penalty
    /// for every unplaced job.
    public long cost() {
        Weights w = p.weights;
        long c = 0;
        for (int s = 0; s < routeCost.length; s++) {
            c += routeCost[s];
        }
        long sum = 0;
        int counted = 0;
        boolean[] hasShift = new boolean[p.techs.length];
        for (int s = 0; s < p.shifts.length; s++) {
            hasShift[p.shifts[s].tech] = true;
        }
        for (int t = 0; t < techWork.length; t++) {
            c += overtimeCost(t, techWork[t]);
            if (hasShift[t]) {
                sum += techWork[t];
                counted++;
            }
        }
        if (counted > 1) {
            long mean = sum / counted;
            for (int t = 0; t < techWork.length; t++) {
                if (hasShift[t]) {
                    long d = techWork[t] - mean;
                    c += w.balance * d * d / 3600L;
                }
            }
        }
        for (int j = 0; j < jobShift.length; j++) {
            if (jobShift[j] < 0) {
                long pr = p.jobs[j].priority;
                c += w.unassigned * pr * pr;
            }
        }
        return c;
    }

    public int unassignedCount() {
        int n = 0;
        for (int s : jobShift) {
            if (s < 0) {
                n++;
            }
        }
        return n;
    }

    /// The meal break `evaluate` placed on its last call, or -1.
    int lastBreakStart() {
        return lastBreak;
    }

    /// Minutes past SLA deadlines on the last call of `evaluate`.
    int lastLateMinutes() {
        return lastLate;
    }

    public int totalDriveMinutes() {
        int d = 0;
        for (int v : routeDrive) {
            d += v;
        }
        return d;
    }

    public int totalOvertimeMinutes() {
        int o = 0;
        for (int t = 0; t < techWork.length; t++) {
            o += Math.max(0, techWork[t] - p.techs[t].regularMinutes);
        }
        return o;
    }

    /// Start time of every placed job, or -1.
    int[] startTimes() {
        int[] out = new int[p.jobs.length];
        for (int j = 0; j < out.length; j++) {
            out[j] = -1;
        }
        int[] starts = new int[p.jobs.length + 1];
        for (int s = 0; s < route.length; s++) {
            evaluate(s, route[s], size[s], starts);
            for (int i = 0; i < size[s]; i++) {
                out[route[s][i]] = starts[i];
            }
        }
        return out;
    }

    public Plan toPlan() {
        return Plan.of(this);
    }
}
