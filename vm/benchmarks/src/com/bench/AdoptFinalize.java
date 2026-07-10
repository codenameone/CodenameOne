package com.bench;

/**
 * Decisive test for DOUBLE-FINALIZE of matured objects. A matured (-4) object that dies
 * flows through the legacy sweep's freeAndFinalize (runs its finalizer) AND -- because
 * codenameOneGcFree reverts it to a -3 dead BiBOP slot -- is later reclaimed by the BiBOP
 * sweep too. If the BiBOP reclaim path ALSO finalizes, the finalizer runs twice; for a
 * real object that frees a native peer in its finalizer that is a double-free
 * (the suite's "corrupted unsorted chunks" abort). Here the finalizer just counts, so a
 * finalized count GREATER than the created count proves the double-finalize.
 */
public class AdoptFinalize {
    static volatile int created = 0;
    static volatile int finalized = 0;

    static final class Res {
        Res a, b;              // non-leaf so it qualifies for maturation
        Res() { created++; }
        protected void finalize() { finalized++; }
    }

    static Res build(int depth) {
        if (depth <= 0) return null;
        Res r = new Res();
        r.a = build(depth - 1);
        r.b = build(depth - 1);
        return r;
    }

    public static void main(String[] args) {
        for (int round = 0; round < 25; round++) {
            Res[] hold = new Res[40];
            for (int i = 0; i < hold.length; i++) hold[i] = build(8); // 255 each
            System.gc(); System.gc();       // mature the survivors
            for (int i = 0; i < hold.length; i++) hold[i] = null;
            System.gc(); System.gc();       // matured objects die
            // give finalizers a chance to run + churn the heap
            long s = 0; for (int i = 0; i < 20000; i++) s += Integer.toString(i).length();
            if (s == -1) System.out.println(s);
        }
        for (int i = 0; i < 6; i++) { System.gc(); }
        System.out.println("created=" + created + " finalized=" + finalized
            + (finalized > created ? " DOUBLE-FINALIZE" : " ok"));
    }
}
