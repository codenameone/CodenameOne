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

import java.util.ArrayList;

/**
 * Rotates a large list of OLD objects with {@code add(remove(0))} while a second thread
 * keeps the collector marking, so markers scan the list's storage block while it is
 * being shifted.
 *
 * <p>{@code remove(0)} moves every element down one slot with a single memmove, and the
 * memmove runs upward faster than a marker walking the same block. When it overtakes
 * the marker, the element it carries from the unscanned side of the scan position to the
 * scanned side is seen in neither place. Logging only the one slot whose value leaves the
 * block is therefore unsound ON ITS OWN -- that version shipped, and the self-hosting
 * translator lost a LineNumber out of a live instruction list about one run in twenty.
 * It is sound paired with a re-scan of the block by the collector after the move, which is
 * what a mark now does (DEFERRED BLOCK RE-SCAN in cn1_globals.m).</p>
 *
 * <p>Everything in the list is allocated before the first collection so it is not
 * protected by the one-cycle grace rule, and the list is the ONLY reference to each item,
 * so a missed element is swept and the verifier reports the list's block as a dangling
 * holder. {@code run-gc-verify.sh}'s seventh self-test re-injects the narrowed barrier
 * with {@code CN1_GC_FAULT=moverange} and requires this driver to catch it.</p>
 */
public class MoveRace {
    // Large enough that one marker's walk of the block outlasts one memmove of it.
    private static final int SIZE = 200000;
    private static final long RUN_MS = 3000;

    static final class Item {
        final int id;
        Item(int id) { this.id = id; }
    }

    static volatile boolean done;
    static int[] sink;

    public static void main(String[] args) throws Exception {
        ArrayList<Item> list = new ArrayList<Item>(SIZE);
        long expected = 0;
        for (int i = 0; i < SIZE; i++) {
            list.add(new Item(i));
            expected += i;
        }
        // Runs collections back to back and allocates only a little garbage, so a mark
        // is in progress for most of the time the main thread spends shifting.
        Thread collector = new Thread() {
            public void run() {
                while (!done) {
                    System.gc();
                }
            }
        };
        collector.start();
        long end = System.currentTimeMillis() + RUN_MS;
        while (System.currentTimeMillis() < end) {
            for (int r = 0; r < 10; r++) {
                list.add(list.remove(0));
            }
            // Shifting allocates nothing, and an allocation is the only safepoint this
            // loop can reach. Without one every cycle waits out the collector's
            // force-stop timeout and the run finishes a single mark. Above the BiBOP
            // limit, so each one takes the legacy allocator's handshake.
            sink = new int[1024];
        }
        done = true;
        collector.join();
        long sum = 0;
        for (int i = 0; i < list.size(); i++) {
            sum += list.get(i).id;
        }
        System.out.println("MoveRace size=" + list.size() + " intact=" + (sum == expected));
    }
}
