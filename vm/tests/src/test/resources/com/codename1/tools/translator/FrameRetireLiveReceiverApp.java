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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Objects that are still reachable when the frame that allocated them returns,
 * each by a route the allocating frame cannot see in its own bytecode.
 *
 * <p>The translator retires an object at frame exit when it has proved the
 * object never left the frame. Every shape here keeps its object in a local,
 * passes it to nothing, stores it nowhere and returns nothing -- and the object
 * is live afterwards all the same:</p>
 *
 * <ul>
 *   <li>{@code nativeReceiver}: {@code Thread.start()} is native. It has no
 *       bytecode to examine, and it hands the receiver to a new thread that
 *       runs on it.</li>
 *   <li>{@code transitive}: the method called on the object keeps {@code this}
 *       to itself, and calls another method on {@code this} that does not.</li>
 *   <li>{@code overridden}: the call names the superclass's method, which is
 *       harmless; the object's own class overrides it.</li>
 *   <li>{@code abstractReceiver}: the same, through a method with no body at
 *       all.</li>
 *   <li>{@code clusterRead}: one local object is stored into a field of
 *       another, which is allowed, and then read back out and published.</li>
 * </ul>
 *
 * <p>The witness is {@code finalize()}: each object knows how to tell that it
 * is still reachable, and counts itself when it is finalized in that state. The
 * thread's finalizer releases nothing, so the defect is reported as a number
 * rather than as a crash somewhere else.</p>
 */
public class FrameRetireLiveReceiverApp {
    private static final int RING = 2048;
    static final Object[] KEEP = new Object[RING];
    static final AtomicInteger finalizedLive = new AtomicInteger();
    static final AtomicInteger finalizedLiveThread = new AtomicInteger();
    static final AtomicInteger finalizedDead = new AtomicInteger();
    static final AtomicInteger started = new AtomicInteger();
    static final AtomicInteger finished = new AtomicInteger();
    static Object sink;

    static final class Worker extends Thread {
        private final int naps;

        Worker(int naps) {
            this.naps = naps;
        }

        public void run() {
            for (int i = 0; i < naps; i++) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException err) {
                    break;
                }
            }
            finished.incrementAndGet();
        }

        /**
         * Counts, and releases nothing in either case. Calling up to
         * Thread.finalize() is not possible here: this file is also compiled
         * by JDKs that resolve java.lang.Thread from their own class library,
         * where the call binds to a method the translated runtime does not
         * have. The few hundred thread states this leaks do not matter to a
         * program that exits seconds later.
         */
        protected void finalize() {
            if (isAlive()) {
                finalizedLiveThread.incrementAndGet();
            } else {
                finalizedDead.incrementAndGet();
            }
        }
    }

    static class Tracked {
        int id;

        Tracked(int id) {
            this.id = id;
        }

        void touch() {
        }

        void publish() {
            register();
        }

        void register() {
            KEEP[id] = this;
        }

        protected void finalize() {
            if (KEEP[id] == this) {
                finalizedLive.incrementAndGet();
            } else {
                finalizedDead.incrementAndGet();
            }
        }
    }

    static final class Overrider extends Tracked {
        Overrider(int id) {
            super(id);
        }

        void touch() {
            KEEP[id] = this;
        }
    }

    abstract static class Shape {
        int id;

        abstract void touch();

        protected void finalize() {
            if (KEEP[id] == this) {
                finalizedLive.incrementAndGet();
            } else {
                finalizedDead.incrementAndGet();
            }
        }
    }

    static final class Square extends Shape {
        Square(int id) {
            this.id = id;
        }

        void touch() {
            KEEP[id] = this;
        }
    }

    static final class Node {
        Node next;
        int id;

        protected void finalize() {
            if (KEEP[id] == this) {
                finalizedLive.incrementAndGet();
            } else {
                finalizedDead.incrementAndGet();
            }
        }
    }

    /** Never published by anything: the shape retirement exists for. */
    static final class Scratch {
        int total;

        void add(int v) {
            total += v;
        }
    }

    static void nativeReceiver(int naps) {
        Thread t = new Worker(naps);
        t.setDaemon(true);
        t.start();
    }

    static void transitive(int id) {
        Tracked t = new Tracked(id);
        t.publish();
    }

    static void overridden(int id) {
        Tracked t = new Overrider(id);
        t.touch();
    }

    static void abstractReceiver(int id) {
        Shape s = new Square(id);
        s.touch();
    }

    static void clusterRead(int id) {
        Node a = new Node();
        Node b = new Node();
        b.id = id;
        a.next = b;
        KEEP[id] = a.next;
    }

    static int control(int v) {
        Scratch s = new Scratch();
        s.add(v);
        s.add(v + 1);
        return v;
    }

    private static int intEnv(String name, int def) {
        String v = System.getenv(name);
        if (v == null || v.length() == 0) {
            return def;
        }
        return Integer.parseInt(v);
    }

    public static void main(String[] args) {
        int rounds = intEnv("CN1_FRL_ROUNDS", 6000);
        int id = 0;
        int acc = 0;
        for (int r = 0; r < rounds; r++) {
            if (started.get() - finished.get() < 24) {
                started.incrementAndGet();
                nativeReceiver(6 + (r & 7));
            }
            for (int k = 0; k < 4; k++) {
                transitive(id);
                id = (id + 1) % RING;
                overridden(id);
                id = (id + 1) % RING;
                abstractReceiver(id);
                id = (id + 1) % RING;
                clusterRead(id);
                id = (id + 1) % RING;
                acc += control(r);
            }
            // Garbage, so the collector runs back to back and frame exits land
            // in every phase of a cycle.
            Object[] junk = new Object[64];
            for (int i = 0; i < 2000; i++) {
                junk[i & 63] = new int[8 + (i & 15)];
            }
            sink = junk;
        }
        long settle = System.currentTimeMillis() + 5000;
        while (started.get() != finished.get() && System.currentTimeMillis() < settle) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException err) {
                break;
            }
        }
        for (int i = 0; i < 4; i++) {
            System.gc();
            try {
                Thread.sleep(50);
            } catch (InterruptedException err) {
                break;
            }
        }
        System.out.println("FRAME_RETIRE_LIVE started=" + started.get()
                + " finished=" + finished.get()
                + " finalizedDead=" + finalizedDead.get()
                + " finalizedLive=" + finalizedLive.get()
                + " finalizedLiveThread=" + finalizedLiveThread.get()
                + " acc=" + acc);
        if (finalizedLive.get() != 0 || finalizedLiveThread.get() != 0
                || started.get() != finished.get()) {
            System.exit(1);
        }
    }
}
