/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.demo;

/**
 * A deep stress case for the PARALLEL mark path.
 *
 * gcMarkResolveThreadCount forces one marker because arm64 Linux was corrupting
 * the heap with the pool enabled and a second ordering hole was never found.
 * demo/gcpause does not reach it: one mutator, one reference per object, and no
 * mutation while the mark runs. Everything here exists to attack a concurrent
 * marker specifically.
 *
 *   - Several mutator threads, so marking overlaps real mutation.
 *   - REWIRING of reference fields while the collector is tracing, which is what
 *     the SATB barrier exists for: a reference moved from an unscanned object to
 *     a scanned one is exactly the object a snapshot collector loses.
 *   - Resurrection through a shared stash: objects go unreachable and reachable
 *     again across threads, so a marker that claims an object without publishing
 *     its children shows up as a freed-but-live object.
 *   - Mixed shapes (object, array, String) so more than one markFunction runs,
 *     and DEEP chains so the mark worklist overflows into the grace pass.
 *
 * Detection does not rely on a crash. Every node carries a magic word and a
 * payload whose checksum is derived from its identity, so a prematurely freed
 * and reused node is caught by a value check even when it does not segfault.
 */
public class GcStress {
    static final int MAGIC = 0x5A5AC0DE;

    static final class Node {
        int magic;
        int id;
        int[] payload;
        String name;
        Node left;
        Node right;

        Node(int id) {
            this.magic = MAGIC;
            this.id = id;
            this.payload = new int[8];
            for(int i = 0 ; i < payload.length ; i++) {
                payload[i] = id + i;
            }
            this.name = "node-" + id;
        }

        /** Non-zero describes the damage, so a failure says what was wrong. */
        String check() {
            if(magic != MAGIC) {
                return "magic=" + Integer.toHexString(magic) + " id=" + id;
            }
            if(payload == null || payload.length != 8) {
                return "payload shape id=" + id;
            }
            for(int i = 0 ; i < 8 ; i++) {
                if(payload[i] != id + i) {
                    return "payload[" + i + "]=" + payload[i] + " want " + (id + i);
                }
            }
            if(name == null || !name.equals("node-" + id)) {
                return "name=" + name + " id=" + id;
            }
            return null;
        }
    }

    /** Cross-thread visibility of the graph is the point, hence the shared stash. */
    static final Object STASH_LOCK = new Object();
    static Node[] stash = new Node[512];
    static volatile boolean running = true;
    static volatile String failure = null;
    static int nextId = 1;

    static synchronized int allocId() {
        return nextId++;
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

    static final class Worker extends Thread {
        private final int seed;
        private final int rounds;
        Worker(int seed, int rounds) {
            this.seed = seed;
            this.rounds = rounds;
        }

        public void run() {
            int rnd = seed * 0x9E3779B1 + 1;   // hex form: decimal would overflow int
            Node[] local = new Node[256];
            try {
                for(int round = 0 ; round < rounds && running ; round++) {
                    rnd = rnd * 1103515245 + 12345;
                    int slot = (rnd >>> 8) & 255;

                    // A short chain per round: depth makes the mark recurse and the
                    // worklist overflow rather than fitting in one batch.
                    Node head = new Node(allocId());
                    Node cur = head;
                    for(int d = 0 ; d < 12 ; d++) {
                        cur.left = new Node(allocId());
                        cur.right = new Node(allocId());
                        cur = cur.left;
                    }
                    local[slot] = head;

                    // Rewire an older node's child to a newer one WHILE the collector
                    // may be tracing: the deletion barrier has to catch the old value.
                    int other = (rnd >>> 16) & 255;
                    Node victim = local[other];
                    if(victim != null) {
                        victim.right = head;
                    }

                    // Publish and take back through shared state, so objects change
                    // reachability across threads mid-cycle.
                    if((round & 7) == 0) {
                        synchronized(STASH_LOCK) {
                            int si = (rnd >>> 4) & 511;
                            Node taken = stash[si];
                            stash[si] = head;
                            if(taken != null) {
                                local[(other + 1) & 255] = taken;
                            }
                        }
                    }

                    // Drop references so most of it is garbage.
                    if((round & 3) == 0) {
                        local[(slot + 7) & 255] = null;
                    }

                    // Verify what we still hold. A prematurely collected node shows
                    // up here as damaged content rather than as a crash.
                    if((round & 15) == 0) {
                        for(int i = 0 ; i < local.length ; i++) {
                            Node n = local[i];
                            int depth = 0;
                            while(n != null && depth < 6) {
                                String bad = n.check();
                                if(bad != null) {
                                    failure = "worker" + seed + " " + bad;
                                    running = false;
                                    return;
                                }
                                n = n.left;
                                depth++;
                            }
                        }
                    }
                }
            } catch (Throwable err) {
                failure = "worker" + seed + " threw " + err;
                running = false;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int threads = envInt("THREADS", 4);
        int rounds = envInt("ROUNDS", 4000);
        int gcEvery = envInt("GC_EVERY_MS", 40);

        Worker[] workers = new Worker[threads];
        for(int i = 0 ; i < threads ; i++) {
            workers[i] = new Worker(i + 1, rounds);
            workers[i].start();
        }

        // Keep collections frequent so mark overlaps mutation for most of the run.
        int cycles = 0;
        while(running) {
            boolean alive = false;
            for(int i = 0 ; i < threads ; i++) {
                if(workers[i].isAlive()) {
                    alive = true;
                    break;
                }
            }
            if(!alive) {
                break;
            }
            System.gc();
            cycles++;
            Thread.sleep(gcEvery);
        }
        for(int i = 0 ; i < threads ; i++) {
            workers[i].join();
        }

        // Final sweep over everything still reachable from the stash.
        String bad = null;
        synchronized(STASH_LOCK) {
            for(int i = 0 ; i < stash.length && bad == null ; i++) {
                Node n = stash[i];
                int depth = 0;
                while(n != null && depth < 12 && bad == null) {
                    bad = n.check();
                    n = n.left;
                    depth++;
                }
            }
        }
        if(bad != null && failure == null) {
            failure = "final " + bad;
        }
        if(failure != null) {
            System.out.println("GCSTRESS FAIL " + failure);
            System.exit(1);
        }
        System.out.println("GCSTRESS OK threads=" + threads + " rounds=" + rounds
                + " gcCycles=" + cycles + " ids=" + nextId);
    }
}
