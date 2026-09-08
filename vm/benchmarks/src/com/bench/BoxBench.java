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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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

package com.bench;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Allocation-shaped benchmark for the tagged immediates. Distinct from Bench/CommonWorkloads,
 * which is fixed at exactly ten workload ids (port_status.py requires that), and from
 * MapBench, which is about probe sequences rather than boxing.
 *
 * The motivating shape is JSON: CodenameOne's JSONParser boxes every numeric token as
 * Double.valueOf or Long.valueOf into a Map, so an app talking to a REST API allocates one
 * heap object per number in every response. jsonLikeParse reproduces that here rather than
 * calling JSONParser itself, because the bench classpath is JavaAPI only.
 *
 * Reports a TAG COVERAGE line as well as timings. Long and Double are only PARTIALLY
 * taggable -- 61 bits of payload, and a double must have three clear low mantissa bits --
 * so a speedup with no coverage figure beside it says nothing about real data.
 */
public final class BoxBench {
    private static final int WARMUP = 3;
    private static final int MEASURE = 5;

    private interface BenchFn { long run(); }

    private static void runBench(String name, BenchFn fn) {
        for (int w = 0; w < WARMUP; w++) {
            fn.run();
        }
        for (int r = 0; r < MEASURE; r++) {
            long started = System.nanoTime();
            long checksum = fn.run();
            long elapsed = System.nanoTime() - started;
            System.out.println("BENCH " + name + " rep " + r
                    + " ns=" + elapsed + " checksum=" + checksum);
        }
    }

    /**
     * The JSON shape: a map of string keys to boxed Long/Double, built then read.
     *
     * The numbers are a deliberately MIXED distribution, not a convenient one. An earlier
     * version used only integers and exact quarters, and reported 100% tag coverage for
     * both Long and Double -- a benchmark measuring the best case and calling it the case.
     * Real payloads carry prices, ratios and measurements whose low mantissa bits are set
     * and which therefore take the heap fallback, so a quarter of the doubles here are
     * money values and a quarter are irrational.
     */
    static double jsonNumber(int doc, int i) {
        switch ((doc + i) & 3) {
            case 0: return doc + i;              // a JSON integer, always taggable
            case 1: return (doc + i) / 4.0;      // an exact quarter, taggable
            case 2: return (doc + i) / 100.0;    // money, mostly not taggable
            default: return Math.sqrt(doc + i);  // irrational, essentially never taggable
        }
    }

    static long jsonLikeParse() {
        String[] keys = new String[24];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = "field" + i;
        }
        long acc = 0;
        for (int doc = 0; doc < 20000; doc++) {
            Map m = new HashMap();
            for (int i = 0; i < keys.length; i++) {
                if ((i & 1) == 0) {
                    m.put(keys[i], Long.valueOf(doc * 31L + i));
                } else {
                    m.put(keys[i], Double.valueOf(jsonNumber(doc, i)));
                }
            }
            for (int i = 0; i < keys.length; i++) {
                Object v = m.get(keys[i]);
                if (v instanceof Long) {
                    acc += ((Long) v).longValue();
                } else {
                    acc += (long) ((Double) v).doubleValue();
                }
            }
        }
        return acc;
    }

    /** Long-keyed map churn: the id/timestamp shape, all inside the taggable range. */
    static long longKeyMap() {
        long acc = 0;
        for (int round = 0; round < 40; round++) {
            Map m = new HashMap();
            for (int i = 0; i < 4000; i++) {
                m.put(Long.valueOf(1757000000000L + i), Long.valueOf(i));
            }
            for (int i = 0; i < 4000; i++) {
                acc += ((Long) m.get(Long.valueOf(1757000000000L + i))).longValue();
            }
        }
        return acc;
    }

    /** Boxed Double reduce through a List, the numeric-column shape. */
    static long doubleListReduce() {
        long acc = 0;
        for (int round = 0; round < 60; round++) {
            List list = new ArrayList();
            for (int i = 0; i < 5000; i++) {
                list.add(Double.valueOf(i / 2.0));
            }
            double sum = 0;
            for (int i = 0; i < list.size(); i++) {
                sum += ((Double) list.get(i)).doubleValue();
            }
            acc += (long) sum;
        }
        return acc;
    }

    /** Character boxing over a text scan, plus a Character-keyed frequency map. */
    static long charBoxing() {
        String text = "the quick brown fox jumps over the lazy dog 0123456789";
        long acc = 0;
        for (int round = 0; round < 4000; round++) {
            Map freq = new HashMap();
            for (int i = 0; i < text.length(); i++) {
                Character c = Character.valueOf(text.charAt(i));
                Object prev = freq.get(c);
                freq.put(c, Integer.valueOf(prev == null ? 1 : ((Integer) prev).intValue() + 1));
            }
            for (int i = 0; i < text.length(); i++) {
                acc += ((Integer) freq.get(Character.valueOf(text.charAt(i)))).intValue();
            }
        }
        return acc;
    }

    /** Mixed boxed types through one Object-typed collection: dispatch on immediates. */
    static long mixedBoxedChurn() {
        long acc = 0;
        for (int round = 0; round < 3000; round++) {
            List list = new ArrayList();
            for (int i = 0; i < 200; i++) {
                switch (i % 5) {
                    case 0: list.add(Integer.valueOf(i)); break;
                    case 1: list.add(Long.valueOf(i)); break;
                    case 2: list.add(Double.valueOf(i)); break;
                    case 3: list.add(Float.valueOf(i)); break;
                    default: list.add(Short.valueOf((short) i)); break;
                }
            }
            for (int i = 0; i < list.size(); i++) {
                acc += ((Number) list.get(i)).longValue() + list.get(i).hashCode();
            }
        }
        return acc;
    }

    /**
     * How much of the workload's own data the partial encodings can actually represent.
     * Printed, never checksummed: it is a property of the target, not of the computation.
     *
     * Detected by its observable consequence -- two separately-obtained boxes of one value
     * are the same reference only for an immediate -- and NOT by reading tag bits out of
     * identityHashCode, which folds a tagged word's halves and no longer exposes them. The
     * values here are all far outside the -128..127 caches, so a shared cache entry cannot
     * be mistaken for an immediate.
     */
    static void reportCoverage() {
        int longHits = 0, longTotal = 0, dblHits = 0, dblTotal = 0;
        for (int doc = 0; doc < 20000; doc++) {
            for (int i = 0; i < 24; i++) {
                if ((i & 1) == 0) {
                    long v = doc * 31L + i;
                    longTotal++;
                    if (Long.valueOf(v) == Long.valueOf(v)) longHits++;
                } else {
                    double d = jsonNumber(doc, i);
                    dblTotal++;
                    if (Double.valueOf(d) == Double.valueOf(d)) dblHits++;
                }
            }
        }
        System.out.println("COVERAGE jsonLikeParse long=" + (longHits * 100 / longTotal)
                + "% double=" + (dblHits * 100 / dblTotal) + "%");
    }

    public static void main(String[] args) {
        runBench("jsonLikeParse", new BenchFn() {
            public long run() { return jsonLikeParse(); }
        });
        runBench("longKeyMap", new BenchFn() {
            public long run() { return longKeyMap(); }
        });
        runBench("doubleListReduce", new BenchFn() {
            public long run() { return doubleListReduce(); }
        });
        runBench("charBoxing", new BenchFn() {
            public long run() { return charBoxing(); }
        });
        runBench("mixedBoxedChurn", new BenchFn() {
            public long run() { return mixedBoxedChurn(); }
        });
        reportCoverage();
        System.out.println("DONE");
    }
}
