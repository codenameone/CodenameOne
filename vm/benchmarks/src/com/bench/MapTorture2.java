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
import java.util.Map;

/**
 * GC driver for maps whose keys and values are REAL HEAP OBJECTS.
 *
 * Every map driver in the verifier's list -- MapTorture, GcStress, MtStress -- uses
 * HashMap&lt;Integer,Integer&gt;, and an Integer is a TAGGED IMMEDIATE: it never allocates and
 * it is never traced. Once HashMap's storage moved from Java arrays into C blocks those
 * workloads stopped allocating on the Java heap almost entirely, and MapTorture under
 * CN1_GC_VERIFY went to ZERO completed GC cycles -- the gate reported itself vacuous,
 * which is exactly what it is built to do.
 *
 * The gap that exposed is older and worse than the vacuity: nothing in the verifier ever
 * held a map full of real references across a collection. That is precisely the case the
 * block storage depends on, because a C block is reachable to the collector ONLY through
 * the owner's generated __GC_MARK_ (the conservative scanner walks native stacks, not
 * arbitrary malloc blocks). If that hook were missing or wrong, every key and value in
 * every map would be swept while the map still pointed at them.
 *
 * So: String keys and Object values, maps that grow through several rehashes, some held
 * live across forced collections and some dropped, and every surviving entry read back
 * and checksummed. A dangling reference shows up as a wrong checksum or a crash rather
 * than as silence.
 */
public class MapTorture2 {
    static final class Val {
        final int id;
        final String tag;
        Val(int id) {
            this.id = id;
            this.tag = "v" + id;
        }
    }

    public static void main(String[] args) {
        long ck = 0;
        // Held live across every collection below: if the mark hook does not trace the
        // blocks, these entries are swept and the read-back fails.
        HashMap<String, Val> held = new HashMap<String, Val>();
        for (int i = 0; i < 4000; i++) {
            held.put("k" + i, new Val(i));
        }
        for (int round = 0; round < 6; round++) {
            // Churn: maps that grow through several rehashes and are then dropped. The
            // rehash is the interesting part -- it reads out of the block the fields
            // still point at and writes into new ones.
            for (int m = 0; m < 40; m++) {
                HashMap<String, Val> tmp = new HashMap<String, Val>();
                for (int i = 0; i < 600; i++) {
                    tmp.put("t" + m + "_" + i, new Val(i));
                }
                ck += tmp.size();
                Val probe = tmp.get("t" + m + "_" + 599);
                if (probe == null || probe.id != 599) {
                    System.out.println("LOST churn entry in round " + round);
                    return;
                }
            }
            System.gc();
            // Read the held map back IN FULL after the collection.
            for (int i = 0; i < 4000; i++) {
                Val v = held.get("k" + i);
                if (v == null) {
                    System.out.println("LOST held key k" + i + " after gc in round " + round);
                    return;
                }
                if (v.id != i || !v.tag.equals("v" + i)) {
                    System.out.println("CORRUPT held key k" + i + " in round " + round);
                    return;
                }
                ck += v.id;
            }
            // Remove half, forcing tombstones, then refill: exercises the clear/blank
            // paths that go through the bulk SATB barrier.
            for (int i = 0; i < 4000; i += 2) {
                held.remove("k" + i);
            }
            for (int i = 0; i < 4000; i += 2) {
                held.put("k" + i, new Val(i));
            }
            ck += held.size();
        }
        // entrySet iteration over real references, after everything above
        long sum = 0;
        for (Map.Entry<String, Val> e : held.entrySet()) {
            sum += e.getValue().id + e.getKey().length();
        }
        System.out.println("checksum=" + ck + " entrySum=" + sum + " size=" + held.size());
    }
}
