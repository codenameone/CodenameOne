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

/**
 * The narrowest test of the thing the server benchmark kept pointing at: how
 * long does a mutator stop when the collector runs?
 *
 * No sockets, no HTTP, no scheduler -- one thread allocating short-lived objects
 * against a fixed live set, timing EVERY iteration. Steady work per iteration
 * means every large gap is the collector and nothing else, so the distribution's
 * tail IS the pause distribution. The Go twin (gcpause.go) is the same loop with
 * the same live-set size and iteration count.
 *
 * Reported as a log2 histogram rather than a mean: a mean over millions of fast
 * iterations hides exactly the rare multi-millisecond stall this exists to find.
 */
public class GcPause {
    static final class Node {
        int v;
        Node next;
        Node(int v, Node next) { this.v = v; this.next = next; }
    }

    static int envInt(String name, int def) {
        String v = System.getenv(name);
        if(v == null || v.length() == 0) {
            return def;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException err) {
            return def;
        }
    }

    public static void main(String[] args) {
        int iterations = envInt("ITERS", 20000000);
        int liveSize = envInt("LIVE", 4096);      // power of two, for the mask
        Node[] live = new Node[liveSize];
        long[] buckets = new long[48];
        long worst = 0;
        long checksum = 0;

        // Untimed warm-up so first-touch page faults and the first collection are
        // not charged to the measurement.
        for(int i = 0 ; i < 1000000 ; i++) {
            live[i & (liveSize - 1)] = new Node(i, null);
        }

        long prev = System.nanoTime();
        for(int i = 0 ; i < iterations ; i++) {
            // Each new node points at an older live one, so the collector has a
            // real graph to trace rather than a field of isolated leaves.
            Node n = new Node(i, live[(i * 7) & (liveSize - 1)]);
            live[i & (liveSize - 1)] = n;
            checksum += n.v;
            long now = System.nanoTime();
            long d = now - prev;
            prev = now;
            int b = 0;
            long x = d;
            while(x > 0 && b < 47) {
                x >>= 1;
                b++;
            }
            buckets[b]++;
            if(d > worst) {
                worst = d;
            }
        }
        report(buckets, worst, checksum, iterations);
    }

    static void report(long[] buckets, long worst, long checksum, long iterations) {
        System.out.println("GCPAUSE iterations=" + iterations + " checksum=" + checksum);
        System.out.println("GCPAUSE maxNs=" + worst);
        long total = 0;
        for(int b = 0 ; b < buckets.length ; b++) {
            total += buckets[b];
        }
        printPercentile("p50", buckets, total, 0.50);
        printPercentile("p99", buckets, total, 0.99);
        printPercentile("p999", buckets, total, 0.999);
        printPercentile("p9999", buckets, total, 0.9999);
        // Everything at or above 64us: with steady per-iteration work nothing but
        // a collection reaches that, so this counts pauses directly.
        long stalls = 0;
        for(int b = 17 ; b < buckets.length ; b++) {
            stalls += buckets[b];
        }
        System.out.println("GCPAUSE stallsOver64us=" + stalls);
        for(int b = 17 ; b < buckets.length ; b++) {
            if(buckets[b] != 0) {
                System.out.println("GCPAUSE bucket=" + (1L << (b - 1)) + "ns count=" + buckets[b]);
            }
        }
    }

    static void printPercentile(String name, long[] buckets, long total, double q) {
        long want = (long)(q * (double)total);
        long seen = 0;
        for(int b = 0 ; b < buckets.length ; b++) {
            seen += buckets[b];
            if(seen > want) {
                System.out.println("GCPAUSE " + name + "Ns=" + (b == 0 ? 0L : (1L << (b - 1))));
                return;
            }
        }
    }
}
