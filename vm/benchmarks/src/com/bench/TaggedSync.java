package com.bench;

// Verifies Java synchronization semantics on TAGGED boxed Integers (the
// poor-man's-Valhalla immediates): synchronized(Integer.valueOf(k)) must be a
// real lock -- mutual exclusion across threads, wait/notify functional.
// Regression test for the review finding that tagged monitorEnter/Exit were
// no-ops (every thread entered the critical section at once).
public final class TaggedSync {
    static int unguarded;
    static int guarded;
    static final int THREADS = 8;
    static final int ITERS = 200000;

    public static void main(String[] a) throws Exception {
        final Object lock = Integer.valueOf(42); // tagged on 64-bit builds
        Thread[] ts = new Thread[THREADS];
        for (int t = 0; t < THREADS; t++) {
            ts[t] = new Thread(new Runnable() {
                public void run() {
                    for (int i = 0; i < ITERS; i++) {
                        synchronized (lock) {
                            guarded++;
                        }
                    }
                }
            });
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        int expected = THREADS * ITERS;
        System.out.println("guarded=" + guarded + " expected=" + expected);

        // wait/notify on a tagged value must not crash and must wake the waiter.
        final Object sig = Integer.valueOf(7);
        final boolean[] woke = new boolean[1];
        Thread waiter = new Thread(new Runnable() {
            public void run() {
                synchronized (sig) {
                    try {
                        sig.wait(10000);
                    } catch (InterruptedException e) {
                    }
                    woke[0] = true;
                }
            }
        });
        waiter.start();
        Thread.sleep(300);
        synchronized (sig) {
            sig.notify();
        }
        waiter.join(15000);
        System.out.println("woke=" + woke[0]);
        System.out.println(guarded == expected && woke[0] ? "TAGGEDSYNC PASS" : "TAGGEDSYNC FAIL");
    }
}
