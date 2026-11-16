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

/// Builds and improves schedules with Adaptive Large Neighbourhood Search.
///
/// 1. **Construction**: pinned work first, then regret-2 insertion. The job
///    whose best placement is most ahead of its second-best goes first, so jobs
///    with one realistic technician are not crowded out by flexible ones.
/// 2. **ALNS**: each iteration removes 10-35% of the placed jobs with one of
///    four destroy operators (random, worst, related, whole route) and puts
///    them back with greedy or regret insertion. Operators that produce better
///    schedules are chosen more often (adaptive roulette, Ropke and Pisinger
///    2006 scores 33/9/13).
/// 3. **Acceptance** is threshold acceptance, the deterministic cousin of
///    simulated annealing: a worse candidate is kept while it is within a
///    threshold that shrinks linearly to zero, which lets the search walk out
///    of a local optimum early and settles it at the end.
/// 4. **Route polish**: 2-opt and or-opt on every route the iteration touched.
///
/// The run is bounded by iterations and seeded, so a seed and a problem always
/// produce the same plan, on any runtime.
public final class Solver {
    static final int DESTROY_RANDOM = 0;
    static final int DESTROY_WORST = 1;
    static final int DESTROY_RELATED = 2;
    static final int DESTROY_ROUTE = 3;
    static final int REPAIR_GREEDY = 0;
    static final int REPAIR_REGRET = 1;
    static final String[] DESTROY_NAMES = {"random", "worst", "related", "route"};
    static final String[] REPAIR_NAMES = {"greedy", "regret-2"};

    private final Problem p;
    private final Rng rng;
    private int iterations = 2000;
    private int segment = 100;

    private final int[] destroyWeight = {100, 100, 100, 100};
    private final int[] repairWeight = {100, 100};
    private final int[] destroyScore = new int[4];
    private final int[] repairScore = new int[2];
    private final int[] destroyUses = new int[4];
    private final int[] repairUses = new int[2];

    private long constructionCost;
    private int improvements;

    public Solver(Problem p, long seed) {
        this.p = p;
        this.rng = new Rng(seed);
    }

    public Solver iterations(int n) {
        iterations = n;
        return this;
    }

    /// Construction followed by the full search.
    public Schedule solve() {
        Schedule s = new Schedule(p);
        construct(s);
        return improve(s);
    }

    /// Places the pinned work that is not placed yet, earliest first.
    void placePinned(Schedule s) {
        int[] pinned = new int[p.jobs.length];
        int np = 0;
        for (int j = 0; j < p.jobs.length; j++) {
            if (p.jobs[j].pinnedTech != null && s.jobShift[j] < 0) {
                pinned[np++] = j;
            }
        }
        sortPinnedByStart(pinned, np);
        greedyInsert(s, pinned, np);
    }

    /// Places pinned work and then every job it can, by regret-2.
    void construct(Schedule s) {
        placePinned(s);
        int[] rest = new int[p.jobs.length];
        int nr = 0;
        for (int j = 0; j < p.jobs.length; j++) {
            if (s.jobShift[j] < 0 && p.jobs[j].pinnedTech == null) {
                rest[nr++] = j;
            }
        }
        regretInsert(s, rest, nr);
        constructionCost = s.cost();
    }

    private void sortPinnedByStart(int[] a, int n) {
        for (int i = 1; i < n; i++) {
            int v = a[i];
            int k = i - 1;
            while (k >= 0 && p.jobs[a[k]].pinnedStart > p.jobs[v].pinnedStart) {
                a[k + 1] = a[k];
                k--;
            }
            a[k + 1] = v;
        }
    }

    /// Runs ALNS from a feasible schedule and returns the best one found.
    Schedule improve(Schedule start) {
        Schedule current = start.copy();
        Schedule best = start.copy();
        Schedule cand = start.copy();
        long currentCost = current.cost();
        long bestCost = currentCost;
        long threshold0 = Math.max(1, currentCost / 50);
        int placed = p.jobs.length;
        int[] removed = new int[placed];
        boolean[] touched = new boolean[p.shifts.length];
        for (int it = 0; it < iterations; it++) {
            cand.copyFrom(current);
            int d = roulette(destroyWeight);
            int r = roulette(repairWeight);
            destroyUses[d]++;
            repairUses[r]++;
            for (int k = 0; k < touched.length; k++) {
                touched[k] = false;
            }
            int nRemoved = destroy(cand, d, removed, touched);
            if (nRemoved == 0) {
                continue;
            }
            shuffle(removed, nRemoved);
            sortByPriorityDesc(removed, nRemoved);
            if (r == REPAIR_GREEDY) {
                greedyInsert(cand, removed, nRemoved);
            } else {
                regretInsert(cand, removed, nRemoved);
            }
            for (int k = 0; k < nRemoved; k++) {
                int sh = cand.jobShift[removed[k]];
                if (sh >= 0) {
                    touched[sh] = true;
                }
            }
            for (int sh = 0; sh < touched.length; sh++) {
                if (touched[sh]) {
                    polishRoute(cand, sh);
                }
            }
            long c = cand.cost();
            long threshold = threshold0 * (iterations - it) / iterations;
            int score = 0;
            if (c < bestCost) {
                best.copyFrom(cand);
                bestCost = c;
                score = 33;
                improvements++;
            } else if (c < currentCost) {
                score = 9;
            } else if (c < currentCost + threshold) {
                score = 13;
            }
            if (score > 0) {
                current.copyFrom(cand);
                currentCost = c;
            }
            destroyScore[d] += score;
            repairScore[r] += score;
            if ((it + 1) % segment == 0) {
                adapt();
            }
        }
        for (int sh = 0; sh < p.shifts.length; sh++) {
            polishRoute(best, sh);
        }
        return best;
    }

    private void adapt() {
        for (int i = 0; i < destroyWeight.length; i++) {
            destroyWeight[i] = Math.max(10, (destroyWeight[i] * 8 + destroyScore[i] * 2 * 100 / Math.max(1, segment)) / 10 + 2);
            destroyScore[i] = 0;
        }
        for (int i = 0; i < repairWeight.length; i++) {
            repairWeight[i] = Math.max(10, (repairWeight[i] * 8 + repairScore[i] * 2 * 100 / Math.max(1, segment)) / 10 + 2);
            repairScore[i] = 0;
        }
    }

    private int roulette(int[] w) {
        int total = 0;
        for (int v : w) {
            total += v;
        }
        int x = rng.nextInt(total);
        for (int i = 0; i < w.length; i++) {
            x -= w[i];
            if (x < 0) {
                return i;
            }
        }
        return w.length - 1;
    }

    // ---- destroy ----------------------------------------------------------

    private int destroy(Schedule s, int op, int[] out, boolean[] touched) {
        int[] movable = new int[p.jobs.length];
        int nm = 0;
        for (int j = 0; j < p.jobs.length; j++) {
            if (s.jobShift[j] >= 0 && p.jobs[j].pinnedTech == null) {
                movable[nm++] = j;
            }
        }
        int n = 0;
        // Unplaced jobs always get another chance.
        for (int j = 0; j < p.jobs.length; j++) {
            if (s.jobShift[j] < 0) {
                out[n++] = j;
            }
        }
        if (nm == 0) {
            return n;
        }
        int q = Math.max(1, nm * (10 + rng.nextInt(26)) / 100);
        if (op == DESTROY_ROUTE) {
            int sh = randomNonEmptyRoute(s);
            if (sh >= 0) {
                int[] r = new int[s.size[sh]];
                System.arraycopy(s.route[sh], 0, r, 0, r.length);
                for (int j : r) {
                    if (p.jobs[j].pinnedTech == null && s.jobShift[j] >= 0 && s.remove(j)) {
                        out[n++] = j;
                        touched[sh] = true;
                    }
                }
            }
            return n;
        }
        if (op == DESTROY_WORST) {
            return n + destroyWorst(s, movable, nm, q, out, n, touched);
        }
        if (op == DESTROY_RELATED) {
            return n + destroyRelated(s, movable, nm, q, out, n, touched);
        }
        shuffle(movable, nm);
        int taken = 0;
        for (int i = 0; i < nm && taken < q; i++) {
            int sh = s.jobShift[movable[i]];
            if (sh < 0) {
                continue; // already taken out with the job it waits on
            }
            if (s.remove(movable[i])) {
                out[n + taken++] = movable[i];
                touched[sh] = true;
            }
        }
        return n + taken;
    }

    private int randomNonEmptyRoute(Schedule s) {
        int count = 0;
        for (int sh = 0; sh < s.size.length; sh++) {
            if (s.size[sh] > 0) {
                count++;
            }
        }
        if (count == 0) {
            return -1;
        }
        int pick = rng.nextInt(count);
        for (int sh = 0; sh < s.size.length; sh++) {
            if (s.size[sh] > 0 && pick-- == 0) {
                return sh;
            }
        }
        return -1;
    }

    /// Removes the jobs whose removal saves the most, with a randomized rank
    /// (Ropke's p = 3) so the same jobs are not removed every time.
    private int destroyWorst(Schedule s, int[] movable, int nm, int q, int[] out, int off, boolean[] touched) {
        long[] saving = new long[nm];
        int[] order = new int[nm];
        for (int i = 0; i < nm; i++) {
            saving[i] = removalSaving(s, movable[i]);
            order[i] = i;
        }
        for (int i = 1; i < nm; i++) {
            int v = order[i];
            int k = i - 1;
            while (k >= 0 && saving[order[k]] < saving[v]) {
                order[k + 1] = order[k];
                k--;
            }
            order[k + 1] = v;
        }
        boolean[] used = new boolean[nm];
        int taken = 0;
        int remaining = nm;
        while (taken < q && remaining > 0) {
            int y = rng.nextInt(1000);
            int idx = (int) ((long) remaining * y * y / 1000L * y / 1000000L);
            int seen = -1;
            int pick = -1;
            for (int i = 0; i < nm; i++) {
                if (!used[order[i]] && ++seen == idx) {
                    pick = order[i];
                    break;
                }
            }
            used[pick] = true;
            remaining--;
            int j = movable[pick];
            int sh = s.jobShift[j];
            if (sh < 0) {
                continue; // already taken out with the job it waits on
            }
            if (s.remove(j)) {
                out[off + taken++] = j;
                touched[sh] = true;
            }
        }
        return taken;
    }

    private long removalSaving(Schedule s, int j) {
        int sh = s.jobShift[j];
        if (sh < 0) {
            return Long.MIN_VALUE;
        }
        int n = s.size[sh];
        int[] seq = new int[n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            if (s.route[sh][i] != j) {
                seq[k++] = s.route[sh][i];
            }
        }
        long c = s.evaluate(sh, seq, k, null);
        return c == Schedule.INFEASIBLE ? Long.MIN_VALUE : s.routeCost[sh] - c;
    }

    /// Shaw removal: a seed job and the jobs closest to it in space and start
    /// time, which are the ones that can trade places with each other.
    private int destroyRelated(Schedule s, int[] movable, int nm, int q, int[] out, int off, boolean[] touched) {
        int[] starts = s.startTimes();
        int seed = movable[rng.nextInt(nm)];
        int sp = p.pointOfJob(seed);
        long[] rel = new long[nm];
        int[] order = new int[nm];
        for (int i = 0; i < nm; i++) {
            int j = movable[i];
            rel[i] = 3L * p.travel[sp][p.pointOfJob(j)] + Math.abs(starts[j] - starts[seed]) / 4;
            order[i] = i;
        }
        for (int i = 1; i < nm; i++) {
            int v = order[i];
            int k = i - 1;
            while (k >= 0 && rel[order[k]] > rel[v]) {
                order[k + 1] = order[k];
                k--;
            }
            order[k + 1] = v;
        }
        int taken = 0;
        for (int i = 0; i < nm && taken < q; i++) {
            int j = movable[order[i]];
            int sh = s.jobShift[j];
            if (sh < 0) {
                continue; // already taken out with the job it waits on
            }
            if (s.remove(j)) {
                out[off + taken++] = j;
                touched[sh] = true;
            }
        }
        return taken;
    }

    // ---- repair -----------------------------------------------------------

    /// Each job, in the order given, at its cheapest feasible place.
    void greedyInsert(Schedule s, int[] jobs, int n) {
        for (int i = 0; i < n; i++) {
            int j = jobs[i];
            long best = Schedule.INFEASIBLE;
            int bs = -1;
            int bp = -1;
            for (int sh = 0; sh < p.shifts.length; sh++) {
                long[] r = s.bestPosition(j, sh);
                if (r != null && r[0] < best) {
                    best = r[0];
                    bs = sh;
                    bp = (int) r[1];
                }
            }
            if (bs >= 0 && best < unassignedPenalty(j)) {
                s.insert(j, bs, bp);
            }
        }
    }

    /// Regret-2 with a per-route cache: inserting into a route only invalidates
    /// that route's column, so each step costs one route's worth of evaluation
    /// per remaining job instead of the whole problem.
    void regretInsert(Schedule s, int[] jobs, int n) {
        int ns = p.shifts.length;
        long[][] delta = new long[n][ns];
        int[][] pos = new int[n][ns];
        boolean[] done = new boolean[n];
        for (int i = 0; i < n; i++) {
            for (int sh = 0; sh < ns; sh++) {
                fill(s, jobs[i], sh, delta[i], pos[i]);
            }
        }
        int left = n;
        while (left > 0) {
            int pick = -1;
            long pickRegret = -1;
            long pickBest = 0;
            int pickShift = -1;
            for (int i = 0; i < n; i++) {
                if (done[i]) {
                    continue;
                }
                long b1 = Schedule.INFEASIBLE;
                long b2 = Schedule.INFEASIBLE;
                int bs = -1;
                for (int sh = 0; sh < ns; sh++) {
                    long d = delta[i][sh];
                    if (d < b1) {
                        b2 = b1;
                        b1 = d;
                        bs = sh;
                    } else if (d < b2) {
                        b2 = d;
                    }
                }
                if (bs < 0 || b1 >= unassignedPenalty(jobs[i])) {
                    done[i] = true;
                    left--;
                    continue;
                }
                long regret = b2 == Schedule.INFEASIBLE
                        ? Long.MAX_VALUE / 4 + p.jobs[jobs[i]].priority
                        : b2 - b1 + (long) p.jobs[jobs[i]].priority * 50;
                if (regret > pickRegret || (regret == pickRegret && b1 < pickBest)) {
                    pick = i;
                    pickRegret = regret;
                    pickBest = b1;
                    pickShift = bs;
                }
            }
            if (pick < 0) {
                break;
            }
            if (s.insertionDelta(jobs[pick], pickShift, pos[pick][pickShift]) == Schedule.INFEASIBLE) {
                // A dependency on another route moved since this was cached.
                for (int sh = 0; sh < ns; sh++) {
                    fill(s, jobs[pick], sh, delta[pick], pos[pick]);
                }
                continue;
            }
            s.insert(jobs[pick], pickShift, pos[pick][pickShift]);
            done[pick] = true;
            left--;
            int tech = p.shifts[pickShift].tech;
            for (int i = 0; i < n; i++) {
                if (!done[i]) {
                    // Hours are per technician, so every shift of that technician
                    // may have changed feasibility.
                    // A job tied to another by a dependency can be affected
                    // on any route.
                    boolean linked = p.preds[jobs[i]].length > 0 || p.succs[jobs[i]].length > 0;
                    for (int sh = 0; sh < ns; sh++) {
                        if (linked || p.shifts[sh].tech == tech) {
                            fill(s, jobs[i], sh, delta[i], pos[i]);
                        }
                    }
                }
            }
        }
    }

    private void fill(Schedule s, int j, int sh, long[] delta, int[] pos) {
        long[] r = s.bestPosition(j, sh);
        if (r == null) {
            delta[sh] = Schedule.INFEASIBLE;
        } else {
            delta[sh] = r[0];
            pos[sh] = (int) r[1];
        }
    }

    private long unassignedPenalty(int j) {
        long pr = p.jobs[j].priority;
        return p.weights.unassigned * pr * pr;
    }

    // ---- route polish -----------------------------------------------------

    /// First-improvement 2-opt and or-opt (segments of 1 to 3) inside one
    /// route, until neither finds a cheaper feasible order.
    void polishRoute(Schedule s, int sh) {
        int n = s.size[sh];
        if (n < 2) {
            return;
        }
        int[] seq = new int[n];
        boolean improved = true;
        int guard = 0;
        while (improved && guard++ < 50) {
            improved = false;
            for (int i = 0; i < n - 1 && !improved; i++) {
                for (int k = i + 1; k < n && !improved; k++) {
                    System.arraycopy(s.route[sh], 0, seq, 0, n);
                    for (int a = i, b = k; a < b; a++, b--) {
                        int t = seq[a];
                        seq[a] = seq[b];
                        seq[b] = t;
                    }
                    improved = tryOrder(s, sh, seq, n);
                }
            }
            for (int len = 1; len <= 3 && !improved; len++) {
                for (int i = 0; i + len <= n && !improved; i++) {
                    for (int to = 0; to <= n - len && !improved; to++) {
                        if (to == i) {
                            continue;
                        }
                        moveSegment(s.route[sh], n, i, len, to, seq);
                        improved = tryOrder(s, sh, seq, n);
                    }
                }
            }
        }
    }

    private static boolean tryOrder(Schedule s, int sh, int[] seq, int n) {
        long c = s.evaluate(sh, seq, n, null);
        if (c == Schedule.INFEASIBLE || c >= s.routeCost[sh]) {
            return false;
        }
        long before = s.cost();
        int[] keep = new int[n];
        System.arraycopy(s.route[sh], 0, keep, 0, n);
        if (!s.setOrder(sh, seq, n)) {
            return false;
        }
        if (s.cost() >= before) {
            s.setOrder(sh, keep, n);
            return false;
        }
        return true;
    }

    static void moveSegment(int[] src, int n, int from, int len, int to, int[] out) {
        int[] rest = new int[n - len];
        int k = 0;
        for (int i = 0; i < n; i++) {
            if (i < from || i >= from + len) {
                rest[k++] = src[i];
            }
        }
        int o = 0;
        for (int i = 0; i < to; i++) {
            out[o++] = rest[i];
        }
        for (int i = 0; i < len; i++) {
            out[o++] = src[from + i];
        }
        for (int i = to; i < rest.length; i++) {
            out[o++] = rest[i];
        }
    }

    // ---- utilities --------------------------------------------------------

    private void shuffle(int[] a, int n) {
        for (int i = n - 1; i > 0; i--) {
            int k = rng.nextInt(i + 1);
            int t = a[i];
            a[i] = a[k];
            a[k] = t;
        }
    }

    /// Stable, so the shuffle still breaks ties among equal priorities.
    private void sortByPriorityDesc(int[] a, int n) {
        for (int i = 1; i < n; i++) {
            int v = a[i];
            int k = i - 1;
            while (k >= 0 && p.jobs[a[k]].priority < p.jobs[v].priority) {
                a[k + 1] = a[k];
                k--;
            }
            a[k + 1] = v;
        }
    }

    public long getConstructionCost() {
        return constructionCost;
    }

    public int getImprovements() {
        return improvements;
    }

    /// How often each operator ran, for the dispatcher's "why" panel.
    public String operatorReport() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < DESTROY_NAMES.length; i++) {
            sb.append(DESTROY_NAMES[i]).append('=').append(destroyUses[i]).append(' ');
        }
        for (int i = 0; i < REPAIR_NAMES.length; i++) {
            sb.append(REPAIR_NAMES[i]).append('=').append(repairUses[i]).append(' ');
        }
        return sb.toString().trim();
    }
}
