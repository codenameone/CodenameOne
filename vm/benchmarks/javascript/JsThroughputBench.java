/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Dispatch-shaped throughput benchmark for the JavaScript target.
 *
 * WHY THIS EXISTS. The JS backend compiles a method that can block into a JS
 * generator and every call to it into ``yield*``. Reducing how many methods
 * and call sites need that is the main lever the backend has, and until this
 * file there was no way to say what reducing it was WORTH: the bundle barely
 * changes (``yield* `` is seven characters), the screenshot suite is pass/fail,
 * and the lifecycle harness only reports milestones. So a real reduction in
 * generator density and a change that did nothing looked identical.
 *
 * Every workload here is therefore chosen to be dominated by CALL DISPATCH
 * rather than by arithmetic, allocation or host round-trips, and the suite
 * deliberately contains controls that the dispatch work CANNOT improve
 * (``arithControl``, ``suspendControl``) so a global speedup -- a faster
 * machine, a quieter host, a JIT that warmed differently -- is distinguishable
 * from a real one.
 *
 * EVERY WORKLOAD REPORTS A CHECKSUM, and the runner refuses a result whose
 * checksum moved. A benchmark that gets faster by doing less work is the
 * failure mode this suite is most exposed to, because "fewer generators" and
 * "fewer calls" are easy to confuse.
 *
 * Output is one ``BENCH`` line per workload plus a ``BENCHSUITE`` line; see
 * scripts/run-javascript-throughput-benchmark.sh, which parses them.
 */
public class JsThroughputBench {

    /** Timed repetitions kept; the runner reports the fastest. */
    private static final int REPS = 7;
    /** Untimed repetitions first, so V8 has tiered up before we measure. */
    private static final int WARMUPS = 3;

    private static long suiteChecksum;

    /**
     * Set by the harness before {@code main} runs (a plain static write into
     * the class's field table). Empty means "run everything".
     *
     * Per-workload isolation is not a convenience, it is a correctness
     * requirement for this suite. Measured in one process, a workload inherits
     * whatever heap and JIT state the workloads before it left behind:
     * ``iteratorWalk`` takes 8.5ms run first and 15ms run eighth, so making an
     * EARLIER workload faster shifts a LATER one and the suite reports a
     * regression in code that is byte-for-byte identical. That happened, and
     * it cost an hour to disprove.
     */
    public static String only = "";

    public static void main(String[] args) {
        run("monoVirtual", 1);
        run("polyVirtual", 2);
        run("megaVirtual", 3);
        run("ifaceDispatch", 4);
        run("toStringHeavy", 5);
        run("equalsHeavy", 6);
        run("hashCodeHeavy", 7);
        run("iteratorWalk", 8);
        run("stringOps", 9);
        run("mapChurn", 10);
        run("arithControl", 11);
        run("suspendControl", 12);
        System.out.println("BENCHSUITE checksum=" + suiteChecksum);
    }

    /**
     * Runs one workload and prints its best time. The fastest of N is used
     * rather than the mean because the slow tail here is the host (GC, another
     * process, a timer) and not a property of the code under test; the mean
     * would measure this machine's mood.
     */
    private static void run(String id, int which) {
        if (only != null && only.length() > 0 && !only.equals(id)) {
            return;
        }
        long best = Long.MAX_VALUE;
        long checksum = 0;
        for (int i = 0; i < WARMUPS; i++) {
            checksum = dispatch(which);
        }
        for (int i = 0; i < REPS; i++) {
            long start = System.nanoTime();
            checksum = dispatch(which);
            long elapsed = System.nanoTime() - start;
            if (elapsed < best) {
                best = elapsed;
            }
        }
        suiteChecksum = suiteChecksum * 31 + checksum;
        System.out.println("BENCH id=" + id + " ns=" + best + " checksum=" + checksum);
    }

    private static long dispatch(int which) {
        switch (which) {
            case 1: return monoVirtual();
            case 2: return polyVirtual();
            case 3: return megaVirtual();
            case 4: return ifaceDispatch();
            case 5: return toStringHeavy();
            case 6: return equalsHeavy();
            case 7: return hashCodeHeavy();
            case 8: return iteratorWalk();
            case 9: return stringOps();
            case 10: return mapChurn();
            case 11: return arithControl();
            case 12: return suspendControl();
            default: return 0;
        }
    }

    // ---------------------------------------------------------------- shapes

    abstract static class Shape {
        abstract int area(int n);
        public String toString() { return "Shape"; }
    }
    static final class Sq extends Shape {
        int area(int n) { return n * n; }
        public String toString() { return "Sq"; }
    }
    static final class Tri extends Shape {
        int area(int n) { return (n * n) / 2; }
        public String toString() { return "Tri"; }
    }
    static final class Cir extends Shape {
        int area(int n) { return 3 * n * n; }
        public String toString() { return "Cir"; }
    }
    static final class Hex extends Shape { int area(int n) { return 6 * n; } }
    static final class Oct extends Shape { int area(int n) { return 8 * n; } }
    static final class Pen extends Shape { int area(int n) { return 5 * n; } }
    static final class Rho extends Shape { int area(int n) { return 4 * n; } }
    static final class Trp extends Shape { int area(int n) { return 7 * n; } }

    interface Sink { int accept(int v); }
    static final class AddSink implements Sink { public int accept(int v) { return v + 1; } }
    static final class MulSink implements Sink { public int accept(int v) { return v * 2; } }
    static final class XorSink implements Sink { public int accept(int v) { return v ^ 7; } }
    static final class SubSink implements Sink { public int accept(int v) { return v - 3; } }

    // ------------------------------------------------------------- workloads

    /** One impl behind a supertype reference: the devirtualizable shape. */
    private static long monoVirtual() {
        Shape s = new Sq();
        long acc = 0;
        for (int i = 0; i < 400000; i++) {
            acc += s.area(i & 63);
        }
        return acc;
    }

    /** Three impls in rotation: too many to devirtualize, few enough to be a
     *  polymorphic inline cache hit in V8 IF the call is a direct call. */
    private static long polyVirtual() {
        Shape[] shapes = new Shape[]{ new Sq(), new Tri(), new Cir() };
        long acc = 0;
        for (int i = 0; i < 400000; i++) {
            acc += shapes[i % 3].area(i & 63);
        }
        return acc;
    }

    /** Eight impls: megamorphic, so the dispatch mechanism itself dominates. */
    private static long megaVirtual() {
        Shape[] shapes = new Shape[]{ new Sq(), new Tri(), new Cir(), new Hex(),
                new Oct(), new Pen(), new Rho(), new Trp() };
        long acc = 0;
        for (int i = 0; i < 400000; i++) {
            acc += shapes[i & 7].area(i & 63);
        }
        return acc;
    }

    private static long ifaceDispatch() {
        Sink[] sinks = new Sink[]{ new AddSink(), new MulSink(), new XorSink(), new SubSink() };
        long acc = 0;
        for (int i = 0; i < 400000; i++) {
            acc += sinks[i & 3].accept(i & 1023);
        }
        return acc;
    }

    /** ``toString()`` is one of the two signatures the bridge protects
     *  program-wide, so this is the direct read on that protection. */
    private static long toStringHeavy() {
        Object[] objs = new Object[]{ new Sq(), new Tri(), new Cir(), "literal",
                Integer.valueOf(7), new StringBuilder("sb") };
        long acc = 0;
        for (int i = 0; i < 120000; i++) {
            acc += objs[i % 6].toString().length();
        }
        return acc;
    }

    /** ``equals(Object)`` is the other bridge-protected signature. */
    private static long equalsHeavy() {
        Object[] objs = new Object[]{ "alpha", "beta", Integer.valueOf(3),
                Integer.valueOf(4), Character.valueOf('x'), Long.valueOf(9L) };
        long acc = 0;
        for (int i = 0; i < 200000; i++) {
            if (objs[i % 6].equals(objs[(i + 1) % 6])) {
                acc++;
            }
            acc += 2;
        }
        return acc;
    }

    private static long hashCodeHeavy() {
        Object[] objs = new Object[]{ "alpha", "beta", Integer.valueOf(3),
                Integer.valueOf(4), Character.valueOf('x'), Boolean.TRUE };
        long acc = 0;
        for (int i = 0; i < 200000; i++) {
            acc += objs[i % 6].hashCode() & 1023;
        }
        return acc;
    }

    /** ``hasNext()`` / ``next()`` are interface dispatch in the hottest loop
     *  shape ordinary Codename One code writes. */
    private static List iteratorList;

    private static long iteratorWalk() {
        // Built ONCE, outside the timed region. Allocating 400 boxed Integers
        // and growing an ArrayList inside the measurement made this workload
        // dominated by allocation and GC timing rather than by the iterator
        // dispatch it is named for -- it measured 8.5ms in one harness mode
        // and 14.9ms in another for byte-identical code, and reported an 11%
        // regression against a bundle whose iteratorWalk and all nineteen of
        // its iterator callees were unchanged.
        if (iteratorList == null) {
            List list = new ArrayList();
            for (int i = 0; i < 400; i++) {
                list.add(Integer.valueOf(i));
            }
            iteratorList = list;
        }
        List list = iteratorList;
        long acc = 0;
        for (int rep = 0; rep < 300; rep++) {
            Iterator it = list.iterator();
            while (it.hasNext()) {
                acc += ((Integer) it.next()).intValue() & 63;
            }
        }
        return acc;
    }

    /** ``length()`` / ``charAt()`` / ``substring()`` -- leaf String methods
     *  that signature-wide poisoning turned into suspension points. */
    private static long stringOps() {
        String base = "the quick brown fox jumps over the lazy dog";
        long acc = 0;
        for (int i = 0; i < 120000; i++) {
            int len = base.length();
            acc += len;
            acc += base.charAt(i % len);
            acc += base.substring(i % 8, (i % 8) + 5).length();
        }
        return acc;
    }

    /** The published port-status suite reports the JS port ~190x slower than
     *  native here, far worse than any other workload, so keep a read on it. */
    private static long mapChurn() {
        Map map = new HashMap();
        long acc = 0;
        for (int rep = 0; rep < 40; rep++) {
            for (int i = 0; i < 2000; i++) {
                map.put(Integer.valueOf(i), Integer.valueOf(i * 3));
            }
            for (int i = 0; i < 2000; i++) {
                Object v = map.get(Integer.valueOf(i));
                if (v != null) {
                    acc += ((Integer) v).intValue() & 255;
                }
                // A miss as well as a hit: an open-addressed table's miss path
                // is the one a hit-only benchmark cannot see.
                if (map.get(Integer.valueOf(i + 100000)) != null) {
                    acc++;
                }
            }
            map.clear();
        }
        return acc;
    }

    /** CONTROL. No calls in the loop, so nothing the dispatch work does can
     *  move this. If it moves, the measurement is measuring the host. */
    private static long arithControl() {
        long acc = 0;
        for (int i = 0; i < 3000000; i++) {
            acc += (i * 31) ^ (i >> 3);
        }
        return acc;
    }

    /** CONTROL. A ``synchronized`` block makes the callee genuinely
     *  suspending, so this workload must stay on the generator path however
     *  precise the analysis becomes. It bounds how much of any measured win
     *  could have come from somewhere other than dispatch. */
    private static long suspendControl() {
        Counter c = new Counter();
        long acc = 0;
        for (int i = 0; i < 200000; i++) {
            acc += c.bump(i & 15);
        }
        return acc;
    }

    static final class Counter {
        private int total;
        synchronized int bump(int by) {
            total = (total + by) & 65535;
            return total;
        }
    }
}
