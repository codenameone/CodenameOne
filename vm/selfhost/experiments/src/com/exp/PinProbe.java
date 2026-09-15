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
package com.exp;

/**
 * Does ParparVM's conservative stack scan pin objects that are provably dead?
 *
 * Three arms, three distinct classes so one run compares them in one census:
 *
 *   PinShallow  allocated and dropped in a shallow frame
 *   PinDeep     allocated at the bottom of a deep recursion, then unwound
 *   PinScrub    same as PinDeep, then the stack is overwritten before collecting
 *
 * Every instance is unreachable by the time the collector runs -- nothing holds a
 * reference. So a correct precise collector reclaims all of them, and any survivor
 * is something the conservative scan mistook a stale stack word for. If Deep >>
 * Shallow the depth is what pins; if Scrub << Deep the stale words are the
 * mechanism and overwriting them frees the objects.
 */
public class PinProbe {
    static final int BATCH = 200000;
    static final int DEPTH = 400;

    // Sinks so the allocations cannot be optimised away, without retaining anything.
    static int shallowSink, deepSink, scrubSink, scrubberSink;

    static class PinShallow { int a; }
    static class PinDeep    { int a; }
    static class PinScrub   { int a; }

    static void allocShallow() {
        for (int i = 0; i < BATCH; i++) {
            PinShallow p = new PinShallow();
            p.a = i;
            shallowSink += p.a;
        }
    }

    static void allocDeep(int depth) {
        if (depth > 0) {
            allocDeep(depth - 1);
            return;
        }
        for (int i = 0; i < BATCH; i++) {
            PinDeep p = new PinDeep();
            p.a = i;
            deepSink += p.a;
        }
    }

    static void allocScrub(int depth) {
        if (depth > 0) {
            allocScrub(depth - 1);
            return;
        }
        for (int i = 0; i < BATCH; i++) {
            PinScrub p = new PinScrub();
            p.a = i;
            scrubSink += p.a;
        }
    }

    /**
     * Walks back down to the same depth writing non-pointer values into locals, so
     * every stack slot the allocation loops left behind is overwritten with an
     * integer that cannot be mistaken for a heap address.
     */
    static void scrub(int depth) {
        int a = depth * 3 + 1, b = depth * 5 + 2, c = depth * 7 + 3, d = depth * 11 + 4;
        int e = depth * 13 + 5, f = depth * 17 + 6, g = depth * 19 + 7, h = depth * 23 + 8;
        if (depth > 0) {
            scrub(depth - 1);
        }
        scrubberSink += a + b + c + d + e + f + g + h;
    }

    static void collect(String label) throws Exception {
        System.gc();
        // gc() only signals the collector; give it room to finish a cycle so the
        // census that follows is reading fresh marks.
        Thread.sleep(1500);
        System.err.println("[PROBE] after " + label);
    }

    public static void main(String[] args) throws Exception {
        System.err.println("[PROBE] batch=" + BATCH + " depth=" + DEPTH);

        allocShallow();
        collect("shallow");

        allocDeep(DEPTH);
        collect("deep");

        allocScrub(DEPTH);
        scrub(DEPTH);
        collect("deep+scrub");

        System.err.println("[PROBE] sinks " + shallowSink + " " + deepSink + " "
                + scrubSink + " " + scrubberSink);
        System.out.println("DONE");
    }
}
