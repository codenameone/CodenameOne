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

import java.util.HashMap;

/**
 * The canonical self-contained micro-benchmark suite used by ParparVM and the
 * generated Codename One port applications. It uses only primitives and a couple of
 * java.util collections, so the identical source compiles & runs on both Java SE
 * and ParparVM (translated to C).
 *
 * Anti-JIT design: every benchmark folds its work into a long checksum returned
 * to its caller (defeats dead-code elimination), and every hot loop carries
 * a cross-iteration data dependency (defeats constant folding / hoisting /
 * auto-vectorizing the whole loop away). Fixed seeds keep results deterministic
 * so the checksum can be cross-checked between the two runtimes.
 */
public final class CommonWorkloads {

    private CommonWorkloads() {
    }

    // ---- 1. integer arithmetic: dependent ALU chain ----
    public static long intArithmetic() {
        int a = 0x12345678;
        int b = 0x9E3779B9;
        long checksum = 0;
        for (int i = 0; i < 40000000; i++) {
            a = (a * 1103515245 + 12345) ^ (b >>> 3);
            b = (b + a) * 5 - (a << 7);
            checksum += (a ^ b) & 0xFFFF;
        }
        return checksum + a + b;
    }

    // ---- 2. long (64-bit) arithmetic: dependent chain ----
    public static long longArithmetic() {
        long a = 0x0123456789ABCDEFL;
        long b = -0x123456789L;
        long checksum = 0;
        for (int i = 0; i < 30000000; i++) {
            a = (a * 6364136223846793005L + 1442695040888963407L) ^ (b >>> 7);
            b = (b ^ (a << 13)) + (a >>> 11);
            checksum += (a + b) & 0xFF;
        }
        return checksum + a + b;
    }

    // ---- 3. floating point + transcendental Math ----
    public static long mathTranscendental() {
        double acc = 1.0;
        double x = 0.5;
        for (int i = 0; i < 8000000; i++) {
            x = x + 0.000001 * (i & 1023);
            acc += Math.sqrt(x) + Math.sin(x) * Math.cos(x) - Math.sqrt(acc % 1000.0 + 1.0);
            if (acc > 1e12 || acc < -1e12) acc = acc % 1000.0; // keep bounded, stay data-dependent
        }
        return Double.doubleToLongBits(acc);
    }

    // ---- 4. sequential array fill + reduce (memory bandwidth, bounds checks) ----
    static int[] seqArr = new int[8000000];
    public static long arraySequential() {
        int[] arr = seqArr;
        int n = arr.length;
        long checksum = 0;
        // fill with runtime-dependent values so the reduce can't be precomputed
        int seed = 0x9E3779B9;
        for (int i = 0; i < n; i++) {
            seed = seed * 1103515245 + 12345;
            arr[i] = seed;
        }
        // several reduction passes
        for (int pass = 0; pass < 4; pass++) {
            long s = 0;
            for (int i = 0; i < n; i++) s += arr[i];
            checksum ^= s + pass;
        }
        return checksum;
    }

    // ---- 5. random-access gather (cache-miss bound, pointer-chase style) ----
    static int[] randArr = new int[4000000];
    public static long arrayRandom() {
        int[] arr = randArr;
        int n = arr.length;
        for (int i = 0; i < n; i++) arr[i] = (i * 2654435761L >>> 8) > 0 ? (int) (i * 2654435761L) : i;
        long checksum = 0;
        int idx = 12345;
        for (int i = 0; i < 20000000; i++) {
            // next index depends on the value just read -> defeats prefetch/vectorization
            int v = arr[(idx & 0x7fffffff) % n];
            checksum += v;
            idx = v ^ (idx * 31 + 7);
        }
        return checksum;
    }

    // ---- 6. object allocation + GC churn ----
    static final class Node {
        int v;
        Node next;
        Node(int v, Node next) { this.v = v; this.next = next; }
    }
    public static long objectAllocation() {
        long checksum = 0;
        Node head = null;
        for (int i = 0; i < 8000000; i++) {
            head = new Node(i, head);
            if ((i & 511) == 0) {
                Node p = head;
                int steps = 0;
                while (p != null && steps < 48) { checksum += p.v; p = p.next; steps++; }
                head = null; // drop -> garbage
            }
        }
        return checksum;
    }

    // ---- 7. HashMap put/get churn (implementation differs per platform) ----
    public static long hashMapChurn() {
        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
        long checksum = 0;
        int window = 50000;
        for (int i = 0; i < 3000000; i++) {
            Integer key = Integer.valueOf(i & 0x3FFFF); // ~262k distinct keys cycling
            Integer prev = map.get(key);
            map.put(key, Integer.valueOf(prev == null ? i : prev.intValue() + i));
            if (prev != null) checksum += prev.intValue();
            if (map.size() > window) map.clear();
        }
        return checksum + map.size();
    }

    // ---- 8. string building + hashing ----
    // The built string ESCAPES into a ring buffer that outlives the iteration
    // and is consumed in batches. The previous consume-and-drop shape measured
    // HotSpot's escape analysis scalar-replacing a String that real code would
    // KEEP (you build a string in order to use it) -- an EA-vs-no-EA artifact,
    // not string-building speed. With the escape, both VMs must materialize
    // every String; the work per iteration is otherwise identical and every
    // string is still hashed exactly once.
    static String[] sbRing = new String[256];
    public static long stringBuilding() {
        long checksum = 0;
        for (int i = 0; i < 400000; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("item-").append(i).append('-').append(i * 31 ^ 0x55AA);
            sb.append(":").append((i & 1) == 0 ? "even" : "odd");
            sbRing[i & 255] = sb.toString();
            if ((i & 255) == 255) {
                for (int j = 0; j < 256; j++) {
                    String s = sbRing[j];
                    checksum += s.hashCode() + s.length();
                }
            }
        }
        return checksum;
    }

    // ---- 9. recursion (method-call overhead) ----
    static long fib(int n) {
        if (n < 2) return n;
        return fib(n - 1) + fib(n - 2);
    }
    public static long recursion() {
        long checksum = 0;
        for (int i = 0; i < 3; i++) checksum += fib(35 + (i & 1));
        return checksum;
    }

    // ---- 10. quicksort (mixed array access / compare / swap / recursion) ----
    static int[] sortArr = new int[1500000];
    static void quicksort(int[] a, int lo, int hi) {
        while (lo < hi) {
            int pivot = a[(lo + hi) >>> 1];
            int i = lo, j = hi;
            while (i <= j) {
                while (a[i] < pivot) i++;
                while (a[j] > pivot) j--;
                if (i <= j) { int t = a[i]; a[i] = a[j]; a[j] = t; i++; j--; }
            }
            // recurse into the smaller side, loop on the larger (bounded stack)
            if (j - lo < hi - i) { quicksort(a, lo, j); lo = i; }
            else { quicksort(a, i, hi); hi = j; }
        }
    }
    public static long quicksortBench() {
        int[] a = sortArr;
        int n = a.length;
        int seed = 0xCAFEBABE;
        for (int i = 0; i < n; i++) { seed = seed * 1103515245 + 12345; a[i] = seed; }
        quicksort(a, 0, n - 1);
        long checksum = 0;
        for (int i = 0; i < n; i += 997) checksum += ((long) a[i]) * (i + 1);
        // checksum that sortedness held
        for (int i = 1; i < n; i++) if (a[i - 1] > a[i]) checksum ^= 0xDEADBEEFL;
        return checksum;
    }

}
