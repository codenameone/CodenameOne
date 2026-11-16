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

/// An immutable planning instance: the technicians, the jobs, the shifts cut
/// from their availability, and every travel time the solver will read.
///
/// Everything the inner loops need is precomputed here into arrays indexed by
/// position, so the search never touches a map or an id string.
public final class Problem {
    /// Shifts shorter than this after time off is removed are dropped.
    static final int MIN_SHIFT_MINUTES = 30;

    final Technician[] techs;
    final Job[] jobs;
    final Shift[] shifts;
    final Weights weights;
    /// `travel[a][b]`: points `0..techs-1` are homes, then one per job.
    final int[][] travel;
    /// `canServe[job][shift]`.
    final boolean[][] canServe;
    final int[] preferredTechIndex;
    final int[] previousTechIndex;
    final int[] pinnedTechIndex;
    /// `preds[j]`: jobs that must finish before `j` starts; `succs` the reverse.
    final int[][] preds;
    final int[][] succs;
    final boolean hasDependencies;

    /// Rush hours: pairs of minute-of-day bounds, `[from, to)`, during which
    /// a drive that sets off takes `rushPct` percent longer.
    private int[] rush = new int[0];
    private int rushPct;
    /// Horizon minute at which each local day begins. A day is not always
    /// 1440 minutes (daylight saving), so the minute of day of a departure
    /// is measured from the start of its own day, never by a remainder.
    private int[] dayStarts = new int[0];

    /// Makes drives that start inside any of `windows` (minute-of-day pairs,
    /// from inclusive, to exclusive) take `pct` percent longer. Travel then
    /// depends on when a technician leaves, so the same route can be
    /// feasible at ten and not at five.
    public Problem rushHours(int pct, int... windows) {
        if (windows.length % 2 != 0) {
            throw new IllegalArgumentException("Rush hours come in from/to pairs");
        }
        rushPct = Math.max(0, pct);
        rush = windows.clone();
        return this;
    }

    /// The horizon minute each local day starts at, in order. Without them
    /// days are taken as 1440 minutes from minute 0.
    public Problem dayStarts(int... starts) {
        dayStarts = starts.clone();
        return this;
    }

    /// The minute of its own local day at which horizon minute `t` falls.
    int minuteOfDay(int t) {
        int start = 0;
        if (dayStarts.length == 0) {
            int day = t / (24 * 60);
            if (t < 0 && t % (24 * 60) != 0) {
                day--;
            }
            start = day * 24 * 60;
        } else {
            start = dayStarts[0] > t ? dayStarts[0] - 24 * 60 : dayStarts[0];
            for (int d : dayStarts) {
                if (d <= t) {
                    start = d;
                }
            }
        }
        return t - start;
    }

    public int getRushPct() {
        return rushPct;
    }

    /// The drive from point `a` to point `b` setting off at minute `depart`
    /// of the horizon.
    int leg(int a, int b, int depart) {
        int base = travel[a][b];
        if (rushPct == 0 || base == 0) {
            return base;
        }
        int minute = minuteOfDay(depart);
        for (int i = 0; i < rush.length; i += 2) {
            if (minute >= rush[i] && minute < rush[i + 1]) {
                return base + (base * rushPct + 99) / 100;
            }
        }
        return base;
    }

    public Problem(List<Technician> techList, List<Job> jobList, TravelModel model, Weights weights) {
        this.techs = techList.toArray(new Technician[techList.size()]);
        this.jobs = jobList.toArray(new Job[jobList.size()]);
        this.weights = weights == null ? new Weights() : weights;
        this.shifts = buildShifts(techs);
        int points = techs.length + jobs.length;
        travel = new int[points][points];
        for (int a = 0; a < points; a++) {
            for (int b = 0; b < points; b++) {
                if (a != b) {
                    travel[a][b] = model.minutes(lat(a), lon(a), lat(b), lon(b));
                }
            }
        }
        preferredTechIndex = new int[jobs.length];
        previousTechIndex = new int[jobs.length];
        pinnedTechIndex = new int[jobs.length];
        for (int j = 0; j < jobs.length; j++) {
            preferredTechIndex[j] = techIndex(jobs[j].preferredTech);
            previousTechIndex[j] = techIndex(jobs[j].previousTech);
            pinnedTechIndex[j] = techIndex(jobs[j].pinnedTech);
            if (jobs[j].pinnedTech != null && pinnedTechIndex[j] < 0) {
                throw new IllegalArgumentException("Job " + jobs[j].id
                        + " is pinned to unknown technician " + jobs[j].pinnedTech);
            }
        }
        preds = new int[jobs.length][];
        int[] succCount = new int[jobs.length];
        boolean deps = false;
        for (int j = 0; j < jobs.length; j++) {
            int[] tmp = new int[jobs[j].after.length];
            int n = 0;
            for (String id : jobs[j].after) {
                int q = jobIndex(id);
                if (q >= 0 && q != j) {
                    tmp[n++] = q;
                    succCount[q]++;
                }
            }
            preds[j] = new int[n];
            System.arraycopy(tmp, 0, preds[j], 0, n);
            deps |= n > 0;
        }
        hasDependencies = deps;
        succs = new int[jobs.length][];
        for (int j = 0; j < jobs.length; j++) {
            succs[j] = new int[succCount[j]];
            succCount[j] = 0;
        }
        for (int j = 0; j < jobs.length; j++) {
            for (int q : preds[j]) {
                succs[q][succCount[q]++] = j;
            }
        }
        canServe = new boolean[jobs.length][shifts.length];
        for (int j = 0; j < jobs.length; j++) {
            for (int s = 0; s < shifts.length; s++) {
                canServe[j][s] = compatible(j, s);
            }
        }
    }

    private boolean compatible(int j, int s) {
        Job job = jobs[j];
        Shift sh = shifts[s];
        Technician t = techs[sh.tech];
        if ((t.skills & job.requiredSkills) != job.requiredSkills) {
            return false;
        }
        if (pinnedTechIndex[j] >= 0 && pinnedTechIndex[j] != sh.tech) {
            return false;
        }
        if (job.pinnedTech == null && blocked(job, t, sh) != null) {
            return false;
        }
        int home = sh.tech;
        int p = pointOfJob(j);
        int earliestArrival = sh.start + travel[home][p];
        int start = Math.max(earliestArrival, earliestStart(j));
        return start <= latestStart(j) && start + job.duration + travel[p][home] <= sh.end;
    }

    /// Why technician `t` may never do `job` on shift `sh`, apart from skills
    /// and time, or null when nothing forbids it. Pinned work is exempt: it
    /// is already under way.
    String blocked(Job job, Technician t, Shift sh) {
        for (String x : job.excluded) {
            if (x.equals(t.id)) {
                return "customer excluded";
            }
        }
        for (int bit = 0; bit < 64; bit++) {
            if ((job.certs & (1L << bit)) != 0 && !t.certified(bit, sh.end)) {
                return (t.certs & (1L << bit)) != 0 ? "certification lapsed" : "certification";
            }
        }
        if (weights.strictTerritories && outOfTerritory(job, t)) {
            return "territory";
        }
        return null;
    }

    static boolean outOfTerritory(Job job, Technician t) {
        return job.zone != 0 && t.territories != 0 && (job.zone & t.territories) == 0;
    }

    int earliestStart(int j) {
        Job job = jobs[j];
        return job.pinnedTech != null ? job.pinnedStart : job.windowStart;
    }

    int latestStart(int j) {
        Job job = jobs[j];
        return job.pinnedTech != null ? job.pinnedStart : job.windowEnd;
    }

    int pointOfJob(int j) {
        return techs.length + j;
    }

    private int lat(int point) {
        return point < techs.length ? techs[point].homeLat : jobs[point - techs.length].lat;
    }

    private int lon(int point) {
        return point < techs.length ? techs[point].homeLon : jobs[point - techs.length].lon;
    }

    int techIndex(String id) {
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < techs.length; i++) {
            if (techs[i].id.equals(id)) {
                return i;
            }
        }
        return -1;
    }

    int jobIndex(String id) {
        for (int i = 0; i < jobs.length; i++) {
            if (jobs[i].id.equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public int getJobCount() {
        return jobs.length;
    }

    public int getShiftCount() {
        return shifts.length;
    }

    public Technician getTechnician(int i) {
        return techs[i];
    }

    public Job getJob(int i) {
        return jobs[i];
    }

    public List<Technician> getTechnicians() {
        List<Technician> r = new ArrayList<Technician>();
        for (Technician t : techs) {
            r.add(t);
        }
        return r;
    }

    public List<Job> getJobs() {
        List<Job> r = new ArrayList<Job>();
        for (Job j : jobs) {
            r.add(j);
        }
        return r;
    }

    public Weights getWeights() {
        return weights;
    }

    /// Availability minus time off, per technician, ordered by technician then
    /// start time so that the shift index order is deterministic.
    static Shift[] buildShifts(Technician[] techs) {
        List<Shift> out = new ArrayList<Shift>();
        for (int t = 0; t < techs.length; t++) {
            Technician tech = techs[t];
            List<int[]> pieces = new ArrayList<int[]>();
            for (int i = 0; i + 1 < tech.availability.length; i += 2) {
                pieces.add(new int[] {tech.availability[i], tech.availability[i + 1]});
            }
            for (int k = 0; k + 1 < tech.timeOff.length; k += 2) {
                int offStart = tech.timeOff[k];
                int offEnd = tech.timeOff[k + 1];
                List<int[]> next = new ArrayList<int[]>();
                for (int[] p : pieces) {
                    if (offEnd <= p[0] || offStart >= p[1]) {
                        next.add(p);
                        continue;
                    }
                    if (offStart > p[0]) {
                        next.add(new int[] {p[0], offStart});
                    }
                    if (offEnd < p[1]) {
                        next.add(new int[] {offEnd, p[1]});
                    }
                }
                pieces = next;
            }
            sortByStart(pieces);
            for (int[] p : pieces) {
                if (p[1] - p[0] >= MIN_SHIFT_MINUTES) {
                    out.add(new Shift(t, p[0], p[1]));
                }
            }
        }
        return out.toArray(new Shift[out.size()]);
    }

    private static void sortByStart(List<int[]> list) {
        for (int i = 1; i < list.size(); i++) {
            int[] v = list.get(i);
            int k = i - 1;
            while (k >= 0 && list.get(k)[0] > v[0]) {
                list.set(k + 1, list.get(k));
                k--;
            }
            list.set(k + 1, v);
        }
    }
}
