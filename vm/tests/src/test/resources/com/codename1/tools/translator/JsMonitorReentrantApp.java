/**
 * Verifies that a thread that already holds a monitor can re-enter the
 * same synchronized block without parking. ``monitorEnter`` MUST take
 * the fast ``count++`` path when ``monitor.owner === thread.id``;
 * otherwise it would wrongly park itself on its own monitor and
 * deadlock.
 *
 * Architecture note: only one OS thread (the Web Worker) is in play.
 * Reentrancy here means the same Java green thread acquiring a monitor
 * it already owns -- the cooperative scheduler must not park it on
 * its own monitor's entrants queue.
 *
 * Tests four single-thread reentry patterns then a heavy concurrent
 * phase with many green threads each doing nested reentry under
 * contention -- to exercise the count++/count-- bookkeeping at load
 * and confirm the lock is fully released after all reentry counts
 * unwind.
 *
 * Patterns:
 *   - Direct re-entry: synchronized(L) { synchronized(L) { ... } }
 *   - Method-call re-entry: synchronized(L) { method that synchronized(L) }
 *   - Synchronized-method re-entry on the same instance receiver
 *   - Heavy load: N green threads each doing K nested-reentry cycles
 *
 * result == 511 means all worked.
 */
public class JsMonitorReentrantApp {
    static final int CONTENDERS = 6;
    static final int CYCLES = 15;
    static final int RECURSION_DEPTH = 30;

    static final Object LOCK = new Object();
    static volatile int reentryWorkDone;
    public static volatile int result;

    static class Holder {
        synchronized void recurse(int depth) {
            if (depth > 0) {
                recurse(depth - 1);
            }
        }
    }

    static void inner() {
        synchronized (LOCK) {
            result |= 4;
        }
    }

    static class Reentrant extends Thread {
        public void run() {
            for (int i = 0; i < CYCLES; i++) {
                // Outer + 3 nested inner re-entries. Each successful
                // count-- on exit needs to leave the count at the
                // correct level so subsequent threads can acquire.
                synchronized (LOCK) {
                    synchronized (LOCK) {
                        synchronized (LOCK) {
                            synchronized (LOCK) {
                                reentryWorkDone++;
                            }
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // Pattern 1: direct re-entry.
        synchronized (LOCK) {
            result |= 1;
            synchronized (LOCK) {
                result |= 2;
            }
        }
        // Pattern 2: re-entry via method call inside the same lock.
        synchronized (LOCK) {
            inner();  // sets result |= 4
        }
        // Pattern 3: synchronized-method recursion -- each call re-enters
        // the same instance's monitor.
        Holder h = new Holder();
        h.recurse(RECURSION_DEPTH);
        result |= 8;

        // Pattern 4 (load): many green threads, each doing many nested
        // re-entry cycles. If count++/count-- ever miscounts, the lock
        // either gets stuck owned (next thread deadlocks) or gets
        // released early.
        Reentrant[] threads = new Reentrant[CONTENDERS];
        for (int i = 0; i < CONTENDERS; i++) {
            threads[i] = new Reentrant();
        }
        for (int i = 0; i < CONTENDERS; i++) {
            threads[i].start();
        }
        for (int i = 0; i < CONTENDERS; i++) {
            threads[i].join();
        }
        if (reentryWorkDone == CONTENDERS * CYCLES) {
            result |= 16;
        }

        // Pattern 5: verify lock is fully released after all the
        // reentry unwinding -- a fresh thread should acquire it
        // without contention.
        final boolean[] ok = new boolean[1];
        Thread other = new Thread() {
            public void run() {
                synchronized (LOCK) {
                    ok[0] = true;
                }
            }
        };
        other.start();
        other.join();
        if (ok[0]) {
            result |= 32;
        }

        if (result == (1 | 2 | 4 | 8 | 16 | 32)) {
            result = 511;
        }
    }
}
