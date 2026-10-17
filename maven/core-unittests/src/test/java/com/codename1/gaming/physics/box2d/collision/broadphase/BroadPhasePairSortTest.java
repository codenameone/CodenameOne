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
package com.codename1.gaming.physics.box2d.collision.broadphase;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The broad phase sorts its pair buffer on every step, in place, so that a
/// world with many moving proxies allocates nothing to do it. The order has
/// to be the one `Arrays.sort` gave: the contacts a step makes follow it.
class BroadPhasePairSortTest {

    @Test
    void sortsLikeArraysSortForEverySizeAndLeavesTheRestAlone() {
        int seed = 12345;
        for (int count = 0; count <= 200; count++) {
            Pair[] pairs = new Pair[count + 3];
            Pair[] expected = new Pair[count];
            for (int i = 0; i < pairs.length; i++) {
                seed = seed * 1103515245 + 12345;
                pairs[i] = new Pair();
                // Few distinct ids, so that duplicates and ties on the first id are common.
                pairs[i].proxyIdA = (seed >>> 16) % 13;
                seed = seed * 1103515245 + 12345;
                pairs[i].proxyIdB = (seed >>> 16) % 17;
                if (i < count) {
                    expected[i] = pairs[i];
                }
            }
            Pair[] tail = {pairs[count], pairs[count + 1], pairs[count + 2]};
            Arrays.sort(expected);
            BroadPhase.sortPairs(pairs, count);
            for (int i = 0; i < count; i++) {
                assertEquals(expected[i].proxyIdA, pairs[i].proxyIdA, "first id at " + i + " of " + count);
                assertEquals(expected[i].proxyIdB, pairs[i].proxyIdB, "second id at " + i + " of " + count);
            }
            for (int i = 0; i < 3; i++) {
                assertEquals(tail[i], pairs[count + i], "the element past the end at " + i + " of " + count);
            }
        }
    }

    private static Pair pair(int a, int b) {
        Pair p = new Pair();
        p.proxyIdA = a;
        p.proxyIdB = b;
        return p;
    }

    /// What `updatePairs` hands its callback: each distinct pair once, in order.
    private static String reported(Pair[] sorted, int count) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < count) {
            Pair primary = sorted[i];
            sb.append(primary.proxyIdA).append(':').append(primary.proxyIdB).append(' ');
            i++;
            while (i < count && sorted[i].proxyIdA == primary.proxyIdA && sorted[i].proxyIdB == primary.proxyIdB) {
                i++;
            }
        }
        return sb.toString();
    }

    /// Many random sets against `Arrays.sort`, the sort this replaced: dense ids
    /// (mostly duplicates), sparse ids (hardly any), buffers far past the size at
    /// which `Arrays.sort` stops being an insertion sort, and the orders a shell
    /// sort is weakest on -- sorted, reversed, all equal. The ids must match
    /// position for position, and so must what the caller reports after it has
    /// skipped the duplicates.
    @Test
    void matchesArraysSortOverRandomSetsWithDuplicates() {
        int[] sizes = {1, 2, 3, 4, 5, 7, 8, 13, 14, 31, 32, 33, 40, 41, 64, 121, 122, 255, 364, 365, 1000, 1093, 1094,
            3000};
        int[] ranges = {1, 2, 5, 40, 1000, 1 << 20};
        java.util.Random random = new java.util.Random(20240917L);
        int sets = 0;
        for (int size : sizes) {
            for (int range : ranges) {
                for (int shape = 0; shape < 4; shape++) {
                    for (int round = 0; round < (size > 500 ? 3 : 10); round++) {
                        Pair[] pairs = new Pair[size];
                        for (int i = 0; i < size; i++) {
                            pairs[i] = pair(random.nextInt(range), random.nextInt(range));
                        }
                        if (shape == 1) {
                            Arrays.sort(pairs);
                        } else if (shape == 2) {
                            Arrays.sort(pairs, java.util.Collections.reverseOrder());
                        } else if (shape == 3) {
                            // Every query of a moving proxy found the same neighbours again.
                            for (int i = size / 2; i < size; i++) {
                                pairs[i] = pair(pairs[i - size / 2].proxyIdA, pairs[i - size / 2].proxyIdB);
                            }
                        }
                        Pair[] expected = pairs.clone();
                        Arrays.sort(expected);
                        java.util.IdentityHashMap<Pair, Pair> before = new java.util.IdentityHashMap<Pair, Pair>();
                        for (Pair p : pairs) {
                            before.put(p, p);
                        }
                        BroadPhase.sortPairs(pairs, size);
                        String at = " (size " + size + ", ids below " + range + ", shape " + shape + ")";
                        for (int i = 0; i < size; i++) {
                            if (expected[i].proxyIdA != pairs[i].proxyIdA || expected[i].proxyIdB != pairs[i].proxyIdB) {
                                assertEquals(expected[i].proxyIdA + ":" + expected[i].proxyIdB,
                                        pairs[i].proxyIdA + ":" + pairs[i].proxyIdB, "pair " + i + at);
                            }
                            // The buffer is a pool: every object must still be in it, once.
                            assertEquals(pairs[i], before.remove(pairs[i]), "a pooled pair was lost or doubled" + at);
                        }
                        assertEquals(reported(expected, size), reported(pairs, size), "the pairs reported" + at);
                        sets++;
                    }
                }
            }
        }
        assertEquals(true, sets > 5000, "sets compared: " + sets);
    }
}
