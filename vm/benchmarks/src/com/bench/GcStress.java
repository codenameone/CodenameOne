/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.bench;

import java.util.HashMap;

/**
 * Allocation-heavy driver used only to force MANY garbage collections (hence many
 * parallel mark cycles) quickly, e.g. under ThreadSanitizer. Mixes long linked lists
 * (the deep-reference case the iterative worklist must handle), HashMap churn and
 * StringBuilder churn so the parallel drain walks varied object graphs. Folds work
 * into a checksum so nothing is dead-code eliminated. Reduced iteration counts so it
 * finishes in minutes even with TSan's ~10x slowdown.
 */
public class GcStress {
    static final class Node { int v; Node next; Node(int v, Node n){ this.v=v; this.next=n; } }

    static long allocChurn(int iters, int keep) {
        long checksum = 0;
        Node head = null;
        int len = 0;
        for (int i = 0; i < iters; i++) {
            head = new Node(i, head);
            len++;
            if (len >= keep) {
                // walk the whole live chain (parallel mark just snapshot this graph)
                Node p = head; int steps = 0;
                while (p != null) { checksum += p.v; p = p.next; steps++; }
                checksum += steps;
                head = null; len = 0; // drop -> garbage for the next GC
            }
        }
        return checksum;
    }

    static long mapChurn(int iters) {
        HashMap<Integer,Integer> map = new HashMap<Integer,Integer>();
        long checksum = 0;
        for (int i = 0; i < iters; i++) {
            Integer key = Integer.valueOf(i & 0x3FFF);
            Integer prev = map.get(key);
            map.put(key, Integer.valueOf(prev == null ? i : prev.intValue() + i));
            if (prev != null) checksum += prev.intValue();
            if (map.size() > 20000) map.clear();
        }
        return checksum + map.size();
    }

    static long strChurn(int iters) {
        long checksum = 0;
        for (int i = 0; i < iters; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("s-").append(i).append('/').append(i * 31 ^ 0x55AA);
            String s = sb.toString();
            checksum += s.hashCode() + s.length();
        }
        return checksum;
    }

    public static void main(String[] args) {
        long c = 0;
        for (int round = 0; round < 6; round++) {
            c += allocChurn(700000, 400);
            c += mapChurn(500000);
            c += strChurn(150000);
            System.out.println("ROUND " + round + " checksum=" + c);
        }
        System.out.println("DONE " + c);

        // Give the collector one complete cycle over the heap this workload
        // built. This driver's own churn triggers collections, but whether the
        // last one reaches its sweep before the process exits is a race -- so a
        // -DCN1_GC_VERIFY build verified 1 cycle or 0 depending on the run.
        // Prints nothing, so the byte-identical comparison against the host JVM
        // is unaffected.
        System.gc();
        try {
            Thread.sleep(250);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
